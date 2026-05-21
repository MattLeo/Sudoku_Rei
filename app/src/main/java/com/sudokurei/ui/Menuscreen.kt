package com.sudokureai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sudokurei.game.Difficulty
import com.sudokurei.ui.theme.*

@Composable
fun MenuScreen(onDifficultySelected: (Difficulty) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // ── Title ──────────────────────────────────────────────────────────
            Text(
                text = "SUDOKU",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 4.sp
            )
            Text(
                text = "REI",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 8.sp
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(
                modifier = Modifier.width(80.dp),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(48.dp))

            // ── Prompt ─────────────────────────────────────────────────────────
            Text(
                text = "Select Difficulty",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(24.dp))

            // ── Difficulty buttons ─────────────────────────────────────────────
            Difficulty.entries.forEach { difficulty ->
                DifficultyButton(
                    difficulty = difficulty,
                    onClick = { onDifficultySelected(difficulty) }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun DifficultyButton(difficulty: Difficulty, onClick: () -> Unit) {
    val (bgColor, label) = when (difficulty) {
        Difficulty.EASY   -> DiffMedium to "Easy"
        Difficulty.MEDIUM -> DiffMedium to "Medium"
        Difficulty.HARD   -> DiffMedium to "Hard"
        Difficulty.EXPERT -> DiffMedium to "Expert"
    }
    val givens = 81 - difficulty.cellsToRemove

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "$givens givens",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.End
            )
        }
    }
}