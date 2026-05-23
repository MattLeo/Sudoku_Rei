package com.sudokurei.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sudokurei.game.Difficulty
import com.sudokurei.viewmodel.AppScreen
import com.sudokurei.viewmodel.SudokuViewModel
import com.sudokurei.ui.theme.*

// Navigation hub

@Composable
fun SudokuScreen(vm: SudokuViewModel = viewModel()) {
    val screen    by vm.screen.collectAsStateWithLifecycle()
    val stats     by vm.stats.collectAsStateWithLifecycle()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()
    val showTimer by vm.showTimer.collectAsStateWithLifecycle()
    val autoPause by vm.autoPause.collectAsStateWithLifecycle()


    // Auto-pause when app is backgrounded
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) vm.onAppBackground()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    when (screen) {
        AppScreen.MENU -> MenuScreen(
            onNewGame    = vm::startNewGame,
            onStatistics = { vm.navigateTo(AppScreen.STATS) },
            onSettings   = { vm.navigateTo(AppScreen.SETTINGS) }
        )
        AppScreen.GAME     -> GameContent(vm)
        AppScreen.STATS    -> StatsScreen(
            stats   = stats,
            onBack  = { vm.navigateTo(AppScreen.MENU) },
            onReset = vm::resetStats
        )
        AppScreen.SETTINGS -> SettingsScreen(
            themeMode         = themeMode,
            onThemeChange     = vm::setThemeMode,
            showTimer         = showTimer,
            onShowTimerChange = vm::setShowTimer,
            autoPause         = autoPause,
            onAutoPauseChange = vm::setAutoPause,
            onBack            = { vm.navigateTo(AppScreen.MENU) }
        )
    }
}

// Game screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameContent(vm: SudokuViewModel) {
    val state     by vm.state.collectAsStateWithLifecycle()
    val showTimer by vm.showTimer.collectAsStateWithLifecycle()
    var showCompletionDialog by remember { mutableStateOf(false) }
    var showGameOverDialog   by remember { mutableStateOf(false) }
    var showAbandonDialog    by remember { mutableStateOf(false) }

    LaunchedEffect(state.isComplete) {
        if (state.isComplete) showCompletionDialog = true
    }
    LaunchedEffect(state.isGameOver) {
        if (state.isGameOver) showGameOverDialog = true
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
                    TextButton(onClick = {
                        if (state.isComplete || state.isGameOver) {
                            vm.showMainMenu()
                        } else {
                            showAbandonDialog = true
                        }
                    }) { Text("Menu") }
                }
            )
        }
    ) { padding ->

        if (state.isGenerating) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Generating puzzle...")
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
            // Header
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DifficultyBadge(state.difficulty)
                MistakeCounter(state.mistakeCount)
                TimerDisplay(state.elapsedSeconds, state.isPaused, showTimer, vm::togglePause)
            }

            Spacer(Modifier.height(10.dp))

            if (state.isPaused) {
                PauseOverlay(Modifier.fillMaxWidth())
            } else {
                SudokuGrid(
                    modifier     = Modifier.fillMaxWidth(),
                    cells        = state.cells,
                    selectedCell = state.selectedCell,
                    onCellClick  = vm::selectCell
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionButton("Undo", Icons.AutoMirrored.Filled.Undo, vm::undo,
                    enabled = !state.isComplete && !state.isGameOver)
                ActionButton(
                    label   = if (state.isNotesMode) "Notes ON" else "Notes",
                    icon    = Icons.Default.Edit,
                    onClick = vm::toggleNotesMode,
                    tint    = if (state.isNotesMode) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                    enabled = !state.isComplete && !state.isGameOver
                )
                ActionButton(
                    label   = "Hint (${state.maxHints - state.hintsUsed})",
                    icon    = Icons.Default.Lightbulb,
                    onClick = vm::getHint,
                    enabled = state.hintsUsed < state.maxHints && !state.isComplete && !state.isGameOver
                )
                ActionButton(
                    label   = if (state.showErrors) "Errors ON" else "Errors",
                    icon    = Icons.Default.RemoveRedEye,
                    onClick = vm::toggleShowErrors,
                    tint    = if (state.showErrors) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }

            Spacer(Modifier.height(10.dp))

            NumberPad(
                modifier         = Modifier.fillMaxWidth(),
                completedNumbers = completedNumbers,
                onNumberClick    = vm::enterNumber,
                onErase          = vm::erase
            )
        }
    }

    if (showCompletionDialog) {
        CompletionDialog(
            difficulty = state.difficulty,
            seconds    = state.elapsedSeconds,
            onDismiss  = { showCompletionDialog = false },
            onMenu     = { showCompletionDialog = false; vm.showMainMenu() }
        )
    }

    if (showGameOverDialog) {
        GameOverDialog(
            onTryAgain = {
                showGameOverDialog = false
                vm.startNewGame(state.difficulty)
            },
            onMenu = {
                showGameOverDialog = false
                vm.showMainMenu()
            }
        )
    }

    if (showAbandonDialog) {
        AlertDialog(
            onDismissRequest = { showAbandonDialog = false },
            title = { Text("Abandon Puzzle?") },
            text = { Text("Your current progress will be lost.") },
            confirmButton = {
                Button(
                    onClick = {
                        showAbandonDialog = false
                        vm.showMainMenu()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Abandon") }
            },
            dismissButton = {
                TextButton(onClick = { showAbandonDialog = false }) {
                    Text("Keep Playing")
                }
            }
        )
    }
}

// Sub-composables

@Composable
private fun MistakeCounter(mistakeCount: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = if (index < mistakeCount)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TimerDisplay(seconds: Long, isPaused: Boolean, showTime: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showTime) {
            val text = remember(seconds) { "%02d:%02d".format(seconds / 60, seconds % 60) }
            Icon(Icons.Default.Timer, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.titleMedium)
        }
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
        Icon(icon, label, tint = tint.copy(alpha = alpha), modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
    }
}

@Composable
private fun PauseOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Pause, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text("Paused", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Tap to resume", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CompletionDialog(
    difficulty: Difficulty,
    seconds: Long,
    onDismiss: () -> Unit,
    onMenu: () -> Unit
) {
    val time = "%02d:%02d".format(seconds / 60, seconds % 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Puzzle Complete!") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("${difficulty.displayName} difficulty", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(12.dp))
                Text(time,
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("time", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = onMenu) { Text("Main Menu") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun GameOverDialog(
    onTryAgain: () -> Unit,
    onMenu: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Game Over") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("You made 3 mistakes.", style = MaterialTheme.typography.bodyLarge)
            }
        },
        confirmButton = { Button(onClick = onTryAgain) { Text("Try Again") } },
        dismissButton = { TextButton(onClick = onMenu) { Text("Main Menu") } }
    )
}