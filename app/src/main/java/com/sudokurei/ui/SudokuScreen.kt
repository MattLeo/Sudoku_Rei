package com.sudokurei.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sudokurei.game.Difficulty
import com.sudokurei.viewmodel.SudokuViewModel
import com.sudokurei.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SudokuScreen(vm: SudokuViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showNewGameDialog by remember { mutableStateOf(false) }
    var showCompletionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) showCompletionDialog = true
    }

    val completedNumbers = remember(state.cells) {
        (1..9).filter { num ->
            state.cells.count { it.value == num && !it.isError } == 9
        }.toSet()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sudoku", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { showNewGameDialog = true }) {
                        Text("New Game")
                    }
                }
            )
        }
    ) { padding ->

        if (state.isGenerating) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Generating puzzle…")
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Header row ────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DifficultyBadge(state.difficulty)
                TimerDisplay(
                    seconds = state.elapsedSeconds,
                    isPaused = state.isPaused,
                    onToggle = vm::togglePause
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Grid or pause overlay ─────────────────────────────────────
            if (state.isPaused) {
                PauseOverlay(Modifier.fillMaxWidth())
            } else {
                SudokuGrid(
                    modifier = Modifier.fillMaxWidth(),
                    cells = state.cells,
                    selectedCell = state.selectedCell,
                    onCellClick = vm::selectCell
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Action row ────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionButton(
                    label = "Undo",
                    icon = Icons.Default.Undo,
                    onClick = vm::undo,
                    enabled = !state.isComplete
                )
                ActionButton(
                    label = if (state.isNotesMode) "Notes ON" else "Notes",
                    icon = Icons.Default.Edit,
                    onClick = vm::toggleNotesMode,
                    tint = if (state.isNotesMode) MaterialTheme.colorScheme.primary
                    else LocalContentColor.current,
                    enabled = !state.isComplete
                )
                ActionButton(
                    label = "Hint (${state.maxHints - state.hintsUsed})",
                    icon = Icons.Default.Lightbulb,
                    onClick = vm::getHint,
                    enabled = state.hintsUsed < state.maxHints && !state.isComplete
                )
                ActionButton(
                    label = if (state.showErrors) "Errors ON" else "Errors",
                    icon = Icons.Default.RemoveRedEye,
                    onClick = vm::toggleShowErrors,
                    tint = if (state.showErrors) MaterialTheme.colorScheme.primary
                    else LocalContentColor.current
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Number pad ────────────────────────────────────────────────
            NumberPad(
                modifier = Modifier.fillMaxWidth(),
                completedNumbers = completedNumbers,
                onNumberClick = vm::enterNumber,
                onErase = vm::erase
            )
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────

    if (showNewGameDialog) {
        NewGameDialog(
            current = state.difficulty,
            onDismiss = { showNewGameDialog = false },
            onStart = { diff ->
                vm.startNewGame(diff)
                showNewGameDialog = false
                showCompletionDialog = false
            }
        )
    }

    if (showCompletionDialog) {
        CompletionDialog(
            difficulty = state.difficulty,
            seconds = state.elapsedSeconds,
            onDismiss = { showCompletionDialog = false },
            onNewGame = {
                showCompletionDialog = false
                showNewGameDialog = true
            }
        )
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────

@Composable
private fun TimerDisplay(
    seconds: Long,
    isPaused: Boolean,
    onToggle: () -> Unit
) {
    val text = remember(seconds) { "%02d:%02d".format(seconds / 60, seconds % 60) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Timer,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.titleMedium
        )
        IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                contentDescription = if (isPaused) "Resume" else "Pause",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DifficultyBadge(difficulty: Difficulty) {
    val color = when (difficulty) {
        Difficulty.EASY   -> DiffEasy
        Difficulty.MEDIUM -> DiffMedium
        Difficulty.HARD   -> DiffHard
        Difficulty.EXPERT -> DiffExpert
    }
    Surface(color = color, shape = RoundedCornerShape(4.dp)) {
        Text(
            text = difficulty.displayName.uppercase(),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tint: Color = LocalContentColor.current
) {
    val alpha = if (enabled) 1f else 0.38f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint.copy(alpha = alpha),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
        )
    }
}

@Composable
private fun PauseOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Pause,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Paused",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Tap ▶ to resume",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NewGameDialog(
    current: Difficulty,
    onDismiss: () -> Unit,
    onStart: (Difficulty) -> Unit
) {
    var selected by remember { mutableStateOf(current) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Game") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Choose difficulty:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
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
            Button(onClick = { onStart(selected) }) { Text("Start") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CompletionDialog(
    difficulty: Difficulty,
    seconds: Long,
    onDismiss: () -> Unit,
    onNewGame: () -> Unit
) {
    val time = "%02d:%02d".format(seconds / 60, seconds % 60)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Puzzle Complete! 🎉") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("${difficulty.displayName} difficulty", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = time,
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("time", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Button(onClick = onNewGame) { Text("New Game") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}