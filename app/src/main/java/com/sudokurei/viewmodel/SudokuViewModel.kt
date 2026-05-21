package com.sudokurei.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sudokurei.game.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayDeque

class SudokuViewModel : ViewModel() {

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val history = ArrayDeque<Move>()
    private var timerJob: Job? = null

    init { startNewGame(Difficulty.EASY) }

    // ── Game lifecycle ─────────────────────────────────────────────────────

    fun startNewGame(difficulty: Difficulty) {
        timerJob?.cancel()
        history.clear()
        _state.update { it.copy(isGenerating = true) }

        viewModelScope.launch(Dispatchers.Default) {
            val puzzle = SudokuEngine.generate(difficulty)
            val cells = puzzle.initial.mapIndexed { i, v ->
                CellState(value = v, isGiven = v != 0)
            }
            withContext(Dispatchers.Main) {
                _state.value = GameState(
                    cells = cells,
                    solution = puzzle.solution,
                    difficulty = difficulty,
                    maxHints = difficulty.maxHints
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
        val s = _state.value
        val idx = s.selectedCell
        if (idx < 0 || s.cells[idx].isGiven || s.isComplete || s.isPaused) return

        val cell = s.cells[idx]
        pushHistory(Move(idx, cell))

        val newCells = s.cells.toMutableList()
        if (s.isNotesMode) {
            // Toggle note; clear value
            val notes = cell.notes.toMutableSet().also {
                if (num in it) it.remove(num) else it.add(num)
            }
            newCells[idx] = cell.copy(value = 0, notes = notes, isError = false)
        } else {
            newCells[idx] = cell.copy(value = num, notes = emptySet())
            // Eliminate placed number from notes in all related cells
            eliminateNoteFromRelated(newCells, idx, num)
        }

        applyUpdate(newCells)
    }

    fun erase() {
        val s = _state.value
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
        val hintValue = s.solution[idx]
        newCells[idx] = CellState(value = hintValue, isGiven = false)
        eliminateNoteFromRelated(newCells, idx, hintValue)

        val checked = if (s.showErrors)
            SudokuEngine.checkErrors(newCells, s.solution) else newCells
        val complete = checked.all { it.value != 0 && !it.isError }

        _state.update {
            it.copy(
                cells = checked,
                hintsUsed = it.hintsUsed + 1,
                selectedCell = idx,
                isComplete = complete
            )
        }
        if (complete) timerJob?.cancel()
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
        val s = _state.value
        val cells = if (show)
            SudokuEngine.checkErrors(s.cells, s.solution)
        else
            s.cells.map { it.copy(isError = false) }
        _state.update { it.copy(cells = cells) }
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private fun applyUpdate(newCells: MutableList<CellState>, forceIncomplete: Boolean = false) {
        val s = _state.value
        val checked = if (s.showErrors)
            SudokuEngine.checkErrors(newCells, s.solution) else newCells
        val complete = !forceIncomplete && checked.all { it.value != 0 && !it.isError }
        _state.update { it.copy(cells = checked, isComplete = complete) }
        if (complete) timerJob?.cancel()
    }

    private fun eliminateNoteFromRelated(cells: MutableList<CellState>, idx: Int, num: Int) {
        SudokuEngine.getRelatedIndices(idx).forEach { relIdx ->
            val rel = cells[relIdx]
            if (num in rel.notes) {
                cells[relIdx] = rel.copy(notes = rel.notes - num)
            }
        }
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