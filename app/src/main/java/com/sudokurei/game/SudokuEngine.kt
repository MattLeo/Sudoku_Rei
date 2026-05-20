package com.sudokurei.game

object SudokuEngine {

    data class Puzzle(val solution: IntArray, val initial: IntArray)

    // ── Public API ─────────────────────────────────────────────────────────

    fun generate(difficulty: Difficulty): Puzzle {
        val solution = IntArray(81)
        fillGrid(solution)
        val initial = removeCells(solution.copyOf(), difficulty.cellsToRemove)
        return Puzzle(solution, initial)
    }

    fun getRelatedIndices(index: Int): Set<Int> {
        val row = index / 9
        val col = index % 9
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        val related = mutableSetOf<Int>()
        for (i in 0..8) {
            related.add(row * 9 + i)
            related.add(i * 9 + col)
        }
        for (r in boxRow until boxRow + 3)
            for (c in boxCol until boxCol + 3)
                related.add(r * 9 + c)
        related.remove(index)
        return related
    }

    fun checkErrors(cells: List<CellState>, solution: IntArray): List<CellState> =
        cells.mapIndexed { i, cell ->
            if (!cell.isGiven && cell.value != 0)
                cell.copy(isError = cell.value != solution[i])
            else
                cell.copy(isError = false)
        }

    fun findHintCell(cells: List<CellState>, solution: IntArray): Int {
        val candidates = cells.indices.filter { i ->
            val c = cells[i]
            !c.isGiven && (c.value == 0 || c.value != solution[i])
        }
        return candidates.randomOrNull() ?: -1
    }

    // ── Private: grid generation ───────────────────────────────────────────

    private fun fillGrid(grid: IntArray): Boolean {
        val pos = grid.indexOf(0)
        if (pos == -1) return true
        val candidates = (1..9).shuffled()
        for (num in candidates) {
            if (isValid(grid, pos, num)) {
                grid[pos] = num
                if (fillGrid(grid)) return true
                grid[pos] = 0
            }
        }
        return false
    }

    private fun removeCells(grid: IntArray, count: Int): IntArray {
        val positions = (0..80).shuffled()
        for (pos in positions) {
            if (grid.count { it == 0 } >= count) break
            val backup = grid[pos]
            grid[pos] = 0
            // Restore if removing this cell makes solution non-unique
            if (!hasUniqueSolution(grid)) {
                grid[pos] = backup
            }
        }
        return grid
    }

    // ── Private: solver / validation ──────────────────────────────────────

    fun isValid(grid: IntArray, pos: Int, num: Int): Boolean {
        val row = pos / 9
        val col = pos % 9
        val boxRow = (row / 3) * 3
        val boxCol = (col / 3) * 3
        for (i in 0..8) {
            if (grid[row * 9 + i] == num) return false
            if (grid[i * 9 + col] == num) return false
            if (grid[(boxRow + i / 3) * 9 + boxCol + i % 3] == num) return false
        }
        return true
    }

    private fun hasUniqueSolution(puzzle: IntArray): Boolean {
        val copy = puzzle.copyOf()
        var count = 0

        fun solve(): Boolean {
            val pos = copy.indexOf(0)
            if (pos == -1) { count++; return count > 1 }
            for (num in 1..9) {
                if (isValid(copy, pos, num)) {
                    copy[pos] = num
                    if (solve()) return true
                    copy[pos] = 0
                }
            }
            return false
        }

        solve()
        return count == 1
    }
}