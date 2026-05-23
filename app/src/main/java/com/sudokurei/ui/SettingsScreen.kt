package com.sudokurei.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sudokurei.viewmodel.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    showTimer: Boolean,
    onShowTimerChange: (Boolean) -> Unit,
    autoPause: Boolean,
    onAutoPauseChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ── Appearance ─────────────────────────────────────────────────
            Text(
                text  = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onThemeChange(mode) }
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick  = { onThemeChange(mode) }
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(mode.displayName, style = MaterialTheme.typography.bodyLarge)
                        }
                        if (index < ThemeMode.entries.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Timer ───────────────────────────────────────────────────────
            Text(
                text  = "Timer",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    // Show timer toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onShowTimerChange(!showTimer) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show Timer", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text  = "Display elapsed time during gameplay",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked         = showTimer,
                            onCheckedChange = onShowTimerChange
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Auto-pause toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAutoPauseChange(!autoPause) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-Pause", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text  = "Pause timer when app is backgrounded",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked         = autoPause,
                            onCheckedChange = onAutoPauseChange
                        )
                    }
                }
            }
        }
    }
}