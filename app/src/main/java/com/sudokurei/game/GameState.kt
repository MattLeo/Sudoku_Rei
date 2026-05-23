package com.sudokurei.game

data class CellState(
    val value: Int = 0,
    val isGiven: Boolean = false,
    val isError: Boolean = false,
    val notes: Set<Int> = emptySet()
)

data class GameState(
    val cells: List<CellState> = List(81) { CellState() },
    val solution: IntArray = IntArray(81),
    val difficulty: Difficulty = Difficulty.EASY,
    val selectedCell: Int = -1,
    val hintsUsed: Int = 0,
    val maxHints: Int = 5,
    val isComplete: Boolean = false,
    val isGenerating: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val isPaused: Boolean = false,
    val isNotesMode: Boolean = false,
    val showErrors: Boolean = true,
    val mistakeCount: Int = 0,
    val isGameOver: Boolean = false,
    val isNewBest: Boolean = false,
    val undosRemaining: Int = 3
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GameState) return false
        return cells == other.cells &&
                solution.contentEquals(other.solution) &&
                difficulty == other.difficulty &&
                selectedCell == other.selectedCell &&
                hintsUsed == other.hintsUsed &&
                maxHints == other.maxHints &&
                isComplete == other.isComplete &&
                isGenerating == other.isGenerating &&
                elapsedSeconds == other.elapsedSeconds &&
                isPaused == other.isPaused &&
                isNotesMode == other.isNotesMode &&
                showErrors == other.showErrors &&
                mistakeCount == other.mistakeCount &&
                isGameOver == other.isGameOver &&
                isNewBest == other.isNewBest &&
                undosRemaining == other.undosRemaining
    }

    override fun hashCode(): Int {
        var result = cells.hashCode()
        result = 31 * result + solution.contentHashCode()
        result = 31 * result + difficulty.hashCode()
        result = 31 * result + selectedCell
        result = 31 * result + hintsUsed
        result = 31 * result + maxHints
        result = 31 * result + isComplete.hashCode()
        result = 31 * result + isGenerating.hashCode()
        result = 31 * result + elapsedSeconds.hashCode()
        result = 31 * result + isPaused.hashCode()
        result = 31 * result + isNotesMode.hashCode()
        result = 31 * result + showErrors.hashCode()
        result = 31 * result + mistakeCount
        result = 31 * result + isGameOver.hashCode()
        result = 31 * result + isNewBest.hashCode()
        result = 31 * result + undosRemaining
        return result
    }
}

data class Move(
    val cellIndex: Int,
    val previousCell: CellState
)