package com.sudokurei.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sudokurei.data.GameRepository
import com.sudokurei.data.StatsRepository
import com.sudokurei.game.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayDeque

enum class AppScreen { MENU, GAME, STATS, SETTINGS }

class SudokuViewModel(application: Application) : AndroidViewModel(application) {

    private val gameRepo  = GameRepository(application)
    private val statsRepo = StatsRepository(application)

    private val _state  = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _screen = MutableStateFlow(AppScreen.MENU)
    val screen: StateFlow<AppScreen> = _screen.asStateFlow()

    private val _stats  = MutableStateFlow(PlayerStats())
    val stats: StateFlow<PlayerStats> = _stats.asStateFlow()

    private val history   = ArrayDeque<Move>()
    private var timerJob: Job? = null

    init {
        viewModelScope.launch {
            _stats.value = statsRepo.load()

            val saved = gameRepo.load()
            if (saved != null) {
                val (state, showMenu) = saved
                _state.value  = state
                _screen.value = if (showMenu) AppScreen.MENU else AppScreen.GAME
                if (!showMenu && !state.isComplete) startTimer()
            }

            // Auto-save game state on every change
            combine(_state, _screen) { s, sc -> Pair(s, sc) }
                .collectLatest { (s, sc) -> gameRepo.save(s, sc == AppScreen.MENU) }
        }
    }

    // ── Navigation ─────────────────────────────────────────────────────────

    fun navigateTo(screen: AppScreen) {
        _screen.value = screen
    }

    fun showMainMenu() {
        timerJob?.cancel()
        _screen.value = AppScreen.MENU
    }

    // ── Game lifecycle ─────────────────────────────────────────────────────

    fun startNewGame(difficulty: Difficulty) {
        timerJob?.cancel()
        history.clear()
        _screen.value = AppScreen.GAME
        _state.update { it.copy(isGenerating = true, isComplete = false) }

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
        if (_state.value.isPaused || _state.value.isComplete) return
        _state.update { it.copy(selectedCell = index) }
    }

    fun enterNumber(num: Int) {
        val s   = _state.value
        val idx = s.selectedCell
        if (idx < 0 || s.cells[idx].isGiven || s.isComplete || s.isPaused) return

        val cell = s.cells[idx]
        pushHistory(Move(idx, cell))

        val newCells = s.cells.toMutableList()
        if (s.isNotesMode) {
            val notes = cell.notes.toMutableSet().also {
                if (num in it) it.remove(num) else it.add(num)
            }
            newCells[idx] = cell.copy(value = 0, notes = notes, isError = false)
        } else {
            newCells[idx] = cell.copy(value = num, notes = emptySet())
            SudokuEngine.getRelatedIndices(idx).forEach { relIdx ->
                val rel = newCells[relIdx]
                if (num in rel.notes) {
                    newCells[relIdx] = rel.copy(notes = rel.notes - num)
                }
            }
        }

        applyUpdate(newCells)
    }

    fun erase() {
        val s   = _state.value
        val idx = s.selectedCell
        if (idx < 0 || s.cells[idx].isGiven || s.isComplete || s.isPaused) return
        pushHistory(Move(idx, s.cells[idx]))
        val newCells = s.cells.toMutableList()
        newCells[idx] = CellState(isGiven = false)
        applyUpdate(newCells)
    }

    fun undo() {
        val move = history.pollLast() ?: return
        val newCells = _state.value.cells.toMutableList()
        newCells[move.cellIndex] = move.previousCell
        applyUpdate(newCells, forceIncomplete = true)
    }

    fun getHint() {
        val s = _state.value
        if (s.hintsUsed >= s.maxHints || s.isComplete || s.isPaused) return

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
        if (complete && !s.isComplete) {
            timerJob?.cancel()
            recordWin(s.difficulty, s.elapsedSeconds)
        }
    }

    // ── Toggles ────────────────────────────────────────────────────────────

    fun togglePause() {
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

    // ── Private helpers ────────────────────────────────────────────────────

    private fun applyUpdate(newCells: MutableList<CellState>, forceIncomplete: Boolean = false) {
        val s        = _state.value
        val checked  = if (s.showErrors) SudokuEngine.checkErrors(newCells, s.solution) else newCells
        val complete = !forceIncomplete && checked.all { it.value != 0 && !it.isError }
        _state.update { it.copy(cells = checked, isComplete = complete) }
        if (complete && !s.isComplete) {
            timerJob?.cancel()
            recordWin(s.difficulty, s.elapsedSeconds)
        }
    }

    private fun recordWin(difficulty: Difficulty, elapsedSeconds: Long) {
        val current = _stats.value
        val ds      = current.forDifficulty(difficulty)
        val updated = ds.copy(
            gamesWon  = ds.gamesWon + 1,
            bestTime  = minOf(ds.bestTime, elapsedSeconds),
            totalTime = ds.totalTime + elapsedSeconds
        )
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
                if (!s.isPaused && !s.isComplete && !s.isGenerating) {
                    _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
        }
    }
}