package com.sudokurei.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NumberPad(
    modifier: Modifier = Modifier,
    onNumberClick: (Int) -> Unit,
    onErase: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (row in 0..2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (col in 0..2) {
                    val num = row * 3 + col + 1
                    FilledTonalButton(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        onClick = { onNumberClick(num) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = num.toString(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            onClick = onErase
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Erase",
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Erase")
        }
    }
}