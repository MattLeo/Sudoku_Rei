package com.sudokurei.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sudokurei.game.Difficulty
import com.sudokurei.game.DifficultyStats
import com.sudokurei.game.PlayerStats
import com.sudokurei.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    stats: PlayerStats,
    onBack: () -> Unit,
    onReset: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Difficulty.entries.forEach { diff ->
                StatCard(difficulty = diff, stats = stats.forDifficulty(diff))
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                    )
                )
            ) {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Reset All Statistics")
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Statistics?") },
            text = { Text("This will permanently delete all your statistics. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { onReset(); showResetDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StatCard(difficulty: Difficulty, stats: DifficultyStats) {
    val color = when (difficulty) {
        Difficulty.EASY   -> DiffEasy
        Difficulty.MEDIUM -> DiffMedium
        Difficulty.HARD   -> DiffHard
        Difficulty.EXPERT -> DiffExpert
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Surface(color = color, shape = RoundedCornerShape(4.dp)) {
                Text(
                    text = difficulty.displayName.uppercase(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))

            // Row 1: Played, Won, Win Rate
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Played",   value = stats.gamesPlayed.toString())
                StatItem(label = "Won",      value = stats.gamesWon.toString())
                StatItem(label = "Win Rate", value = if (stats.gamesPlayed > 0) "${stats.winRate}%" else "—")
            }

            Spacer(Modifier.height(12.dp))

            // Row 2: Best, Average
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Best",    value = if (stats.hasStat) formatTime(stats.bestTime)    else "—")
                StatItem(label = "Average", value = if (stats.hasStat) formatTime(stats.averageTime) else "—")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatTime(seconds: Long): String {
    if (seconds == Long.MAX_VALUE) return "—"
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}