package com.sudokurei.game

data class DifficultyStats(
    val gamesPlayed: Int = 0,
    val gamesWon: Int = 0,
    val bestTime: Long = Long.MAX_VALUE,
    val totalTime: Long = 0L
) {
    val averageTime: Long get() = if (gamesWon > 0) totalTime / gamesWon else 0L
    val hasStat: Boolean get() = gamesWon > 0
    val winRate: Int get() = if (gamesPlayed > 0) (gamesWon * 100) / gamesPlayed else 0
}

data class PlayerStats(
    val easy:   DifficultyStats = DifficultyStats(),
    val medium: DifficultyStats = DifficultyStats(),
    val hard:   DifficultyStats = DifficultyStats(),
    val expert: DifficultyStats = DifficultyStats()
) {
    fun forDifficulty(difficulty: Difficulty): DifficultyStats = when (difficulty) {
        Difficulty.EASY   -> easy
        Difficulty.MEDIUM -> medium
        Difficulty.HARD   -> hard
        Difficulty.EXPERT -> expert
    }

    fun withUpdated(difficulty: Difficulty, stats: DifficultyStats): PlayerStats = when (difficulty) {
        Difficulty.EASY   -> copy(easy   = stats)
        Difficulty.MEDIUM -> copy(medium = stats)
        Difficulty.HARD   -> copy(hard   = stats)
        Difficulty.EXPERT -> copy(expert = stats)
    }
}