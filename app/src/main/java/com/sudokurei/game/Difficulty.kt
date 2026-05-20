package com.sudokurei.game

enum class Difficulty(
    val displayName: String,
    val cellsToRemove: Int,
    val maxHints: Int
) {
    EASY("Easy", 36, 5),
    MEDIUM("Medium", 46, 3),
    HARD("Hard", 51, 2),
    EXPERT("Expert", 56, 1)
}