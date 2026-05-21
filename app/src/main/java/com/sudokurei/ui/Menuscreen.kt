package com.sudokurei.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun MenuScreen(
    onNewGame: (Difficulty) -> Unit,
    onStatistics: () -> Unit,
    onSettings: () -> Unit
) {
    var showDifficultyDialog by remember { mutableStateOf(false) }

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
            // ── Title ──────────────────────────────────────────────────────
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
            Spacer(Modifier.height(56.dp))

            // ── New Game ───────────────────────────────────────────────────
            Button(
                onClick = { showDifficultyDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("New Game", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(12.dp))

            // ── Statistics ─────────────────────────────────────────────────
            FilledTonalButton(
                onClick = onStatistics,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Statistics", fontSize = 18.sp)
            }

            Spacer(Modifier.height(12.dp))

            // ── Settings ───────────────────────────────────────────────────
            FilledTonalButton(
                onClick = onSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Settings", fontSize = 18.sp)
            }
        }
    }

    if (showDifficultyDialog) {
        DifficultyDialog(
            onDismiss = { showDifficultyDialog = false },
            onSelect = { diff ->
                showDifficultyDialog = false
                onNewGame(diff)
            }
        )
    }
}

@Composable
private fun DifficultyDialog(
    onDismiss: () -> Unit,
    onSelect: (Difficulty) -> Unit
) {
    var selected by remember { mutableStateOf(Difficulty.EASY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Difficulty") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Difficulty.entries.forEach { diff ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selected = diff }
                            .padding(vertical = 2.dp)
                    ) {
                        RadioButton(selected = selected == diff, onClick = { selected = diff })
                        Spacer(Modifier.width(6.dp))
                        Text(diff.displayName, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${81 - diff.cellsToRemove} givens",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSelect(selected) }) { Text("Start") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}