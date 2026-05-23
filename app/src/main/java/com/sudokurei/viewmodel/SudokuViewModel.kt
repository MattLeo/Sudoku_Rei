package com.sudokurei.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sudokurei.data.GameRepository
import com.sudokurei.data.StatsRepository
import com.sudokurei.data.ThemeRepository
import com.sudokurei.game.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayDeque

enum class ThemeMode(val displayName: String) {
    SYSTEM("System Default"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class AppScreen { MENU, GAME, STATS, SETTINGS }

class SudokuViewModel(application: Application) : AndroidViewModel(application) {

    private val gameRepo   = GameRepository(application)
    private val statsRepo  = StatsRepository(application)
    private val themeRepo  = ThemeRepository(application)

    private val _state  = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _screen = MutableStateFlow(AppScreen.MENU)
    val screen: StateFlow<AppScreen> = _screen.asStateFlow()

    private val _stats     = MutableStateFlow(PlayerStats())
    val stats: StateFlow<PlayerStats> = _stats.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _showTimer  = MutableStateFlow(true)
    val showTimer: StateFlow<Boolean> = _showTimer.asStateFlow()

    private val _autoPause  = MutableStateFlow(true)
    val autoPause: StateFlow<Boolean> = _autoPause.asStateFlow()

    private val history   = ArrayDeque<Move>()
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            _themeMode.value  = themeRepo.loadTheme()
            _showTimer.value  = themeRepo.loadShowTimer()
            _autoPause.value  = themeRepo.loadAutoPause()
            _stats.value = statsRepo.load()

            val saved = gameRepo.load()
            if (saved != null) {
                val (state, showMenu) = saved
                _state.value  = state
                _screen.value = if (showMenu) AppScreen.MENU else AppScreen.GAME
                if (!showMenu && !state.isComplete && !state.isGameOver) startTimer()
            }

            combine(_state, _screen) { s, sc -> Pair(s, sc) }
                .collectLatest { (s, sc) -> gameRepo.save(s, sc == AppScreen.MENU) }
        }
    }

    // ── Navigation ─────────────────────────────────────────────────────────

    fun navigateTo(screen: AppScreen) { _screen.value = screen }

    fun showMainMenu() {
        timerJob?.cancel()
        _screen.value = AppScreen.MENU
    }

    // ── Game lifecycle ─────────────────────────────────────────────────────

    fun startNewGame(difficulty: Difficulty) {
        timerJob?.cancel()
        history.clear()
        _screen.value = AppScreen.GAME
        _state.update { it.copy(isGenerating = true, isComplete = false, isGameOver = false) }

        viewModelScope.launch(Dispatchers.Default) {
            val puzzle = SudokuEngine.generate(difficulty)
            val cells  = puzzle.initial.mapIndexed { i, v -> CellState(value = v, isGiven = v != 0) }
            withContext(Dispatchers.Main) {
                _state.value = GameState(
                    cells      = cells,
                    solution   = puzzle.solution,
                    difficulty = difficulty,
                    maxHints   = difficulty.maxHints
                )
                startTimer()
            }
        }
    }

    // ── Input ──────────────────────────────────────────────────────────────

    fun selectCell(index: Int) {
        if (_state.value.isPaused || _state.value.isComplete || _state.value.isGameOver) return
        _state.update { it.copy(selectedCell = index) }
    }

    fun enterNumber(num: Int) {
        val s   = _state.value
        val idx = s.selectedCell
        if (idx < 0 || s.cells[idx].isGiven || s.isComplete || s.isPaused || s.isGameOver) return

        val cell = s.cells[idx]
        pushHistory(Move(idx, cell))

        val newCells = s.cells.toMutableList()
        if (s.isNotesMode) {
            val notes = cell.notes.toMutableSet().also {
                if (num in it) it.remove(num) else it.add(num)
            }
            newCells[idx] = cell.copy(value = 0, notes = notes, isError = false)
            applyUpdate(newCells)
        } else {
            newCells[idx] = cell.copy(value = num, notes = emptySet())
            // Remove num from notes in all related cells
            SudokuEngine.getRelatedIndices(idx).forEach { relIdx ->
                val rel = newCells[relIdx]
                if (num in rel.notes) newCells[relIdx] = rel.copy(notes = rel.notes - num)
            }
            // Check if this placement is a mistake
            val isMistake = num != s.solution[idx]
            applyUpdate(newCells, mistakeDelta = if (isMistake) 1 else 0)
        }
    }

    fun erase() {
        val s   = _state.value
        val idx = s.selectedCell
        if (idx < 0 || s.cells[idx].isGiven || s.isComplete || s.isPaused || s.isGameOver) return
        pushHistory(Move(idx, s.cells[idx]))
        val newCells = s.cells.toMutableList()
        newCells[idx] = CellState(isGiven = false)
        applyUpdate(newCells)
    }

    fun undo() {
        if (_state.value.isGameOver) return
        val move = history.pollLast() ?: return
        val newCells = _state.value.cells.toMutableList()
        newCells[move.cellIndex] = move.previousCell
        applyUpdate(newCells, forceIncomplete = true)
    }

    fun getHint() {
        val s = _state.value
        if (s.hintsUsed >= s.maxHints || s.isComplete || s.isPaused || s.isGameOver) return

        val idx = SudokuEngine.findHintCell(s.cells, s.solution)
        if (idx < 0) return

        pushHistory(Move(idx, s.cells[idx]))
        val newCells = s.cells.toMutableList()
        newCells[idx] = CellState(value = s.solution[idx], isGiven = false)

        val checked  = if (s.showErrors) SudokuEngine.checkErrors(newCells, s.solution) else newCells
        val complete = checked.all { it.value != 0 && !it.isError }

        _state.update {
            it.copy(cells = checked, hintsUsed = it.hintsUsed + 1, selectedCell = idx, isComplete = complete)
        }
        if (complete) {
            timerJob?.cancel()
            recordWin(s.difficulty, s.elapsedSeconds)
        }
    }

    // ── Toggles ────────────────────────────────────────────────────────────

    fun togglePause() {
        if (_state.value.isGameOver) return
        val nowPaused = !_state.value.isPaused
        _state.update { it.copy(isPaused = nowPaused, selectedCell = -1) }
        if (nowPaused) timerJob?.cancel() else startTimer()
    }

    fun toggleNotesMode() = _state.update { it.copy(isNotesMode = !it.isNotesMode) }

    fun toggleShowErrors() {
        val show = !_state.value.showErrors
        _state.update { it.copy(showErrors = show) }
        val s     = _state.value
        val cells = if (show)
            SudokuEngine.checkErrors(s.cells, s.solution)
        else
            s.cells.map { it.copy(isError = false) }
        _state.update { it.copy(cells = cells) }
    }

    // ── Stats ──────────────────────────────────────────────────────────────

    fun resetStats() {
        statsRepo.reset()
        _stats.value = PlayerStats()
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        themeRepo.saveTheme(mode)
    }

    fun setShowTimer(show: Boolean) {
        _showTimer.value = show
        themeRepo.saveShowTimer(show)
    }

    fun setAutoPause(enabled: Boolean) {
        _autoPause.value = enabled
        themeRepo.saveAutoPause(enabled)
    }

    fun onAppBackground() {
        val s = _state.value
        if (_autoPause.value &&
            !s.isPaused &&
            !s.isComplete &&
            !s.isGameOver &&
            _screen.value == AppScreen.GAME
        ) {
            togglePause()
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private fun applyUpdate(
        newCells: MutableList<CellState>,
        forceIncomplete: Boolean = false,
        mistakeDelta: Int = 0
    ) {
        val s            = _state.value
        val checked      = if (s.showErrors) SudokuEngine.checkErrors(newCells, s.solution) else newCells
        val complete     = !forceIncomplete && checked.all { it.value != 0 && !it.isError }
        val newMistakes  = s.mistakeCount + mistakeDelta
        val gameOver     = newMistakes >= 3

        _state.update {
            it.copy(
                cells        = checked,
                isComplete   = complete,
                mistakeCount = newMistakes,
                isGameOver   = gameOver
            )
        }

        when {
            complete && !s.isComplete -> {
                timerJob?.cancel()
                recordWin(s.difficulty, s.elapsedSeconds)
            }
            gameOver && !s.isGameOver -> {
                timerJob?.cancel()
                recordLoss(s.difficulty)
            }
        }
    }

    private fun recordWin(difficulty: Difficulty, elapsedSeconds: Long) {
        val current   = _stats.value
        val ds        = current.forDifficulty(difficulty)
        val isNewBest = ds.gamesWon == 0 || elapsedSeconds < ds.bestTime
        val updated   = ds.copy(
            gamesPlayed = ds.gamesPlayed + 1,
            gamesWon    = ds.gamesWon + 1,
            bestTime    = minOf(ds.bestTime, elapsedSeconds),
            totalTime   = ds.totalTime + elapsedSeconds
        )
        val newStats = current.withUpdated(difficulty, updated)
        _stats.value = newStats
        statsRepo.save(newStats)
        _state.update { it.copy(isNewBest = isNewBest) }
    }

    private fun recordLoss(difficulty: Difficulty) {
        val current  = _stats.value
        val ds       = current.forDifficulty(difficulty)
        val updated  = ds.copy(gamesPlayed = ds.gamesPlayed + 1)
        val newStats = current.withUpdated(difficulty, updated)
        _stats.value = newStats
        statsRepo.save(newStats)
    }

    private fun pushHistory(move: Move) {
        history.addLast(move)
        if (history.size > 50) history.pollFirst()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000)
                val s = _state.value
                if (!s.isPaused && !s.isComplete && !s.isGenerating && !s.isGameOver) {
                    _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
        }
    }
}