package com.sudokurei.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import com.sudokurei.game.CellState
import com.sudokurei.game.SudokuEngine
import com.sudokurei.ui.theme.*

@Composable
fun SudokuGrid(
    modifier: Modifier = Modifier,
    cells: List<CellState>,
    selectedCell: Int,
    onCellClick: (Int) -> Unit
) {
    val relatedCells = remember(selectedCell) {
        if (selectedCell >= 0) SudokuEngine.getRelatedIndices(selectedCell) else emptySet()
    }
    val selectedValue = remember(selectedCell, cells) {
        if (selectedCell >= 0) cells.getOrNull(selectedCell)?.value ?: 0 else 0
    }

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val cellPx = size.width / 9f
                    val col = (offset.x / cellPx).toInt().coerceIn(0, 8)
                    val row = (offset.y / cellPx).toInt().coerceIn(0, 8)
                    onCellClick(row * 9 + col)
                }
            }
    ) {
        val cellSizeDp = maxWidth / 9
        val density = LocalDensity.current
        val cellSizePx = with(density) { cellSizeDp.toPx() }
        val numberFontSize = with(density) { (cellSizePx * 0.48f).toSp() }
        val noteFontSize = with(density) { (cellSizePx * 0.22f).toSp() }
        val textMeasurer = rememberTextMeasurer()

        // ── Canvas: backgrounds + grid lines ──────────────────────────────
        Canvas(Modifier.fillMaxSize()) {
            val cSize = size.width / 9f

            // Cell backgrounds
            cells.forEachIndexed { i, cell ->
                val row = i / 9
                val col = i % 9
                val bg = when {
                    i == selectedCell                                          -> CellSelected
                    cell.isError                                               -> CellError
                    i in relatedCells                                          -> CellRelated
                    selectedValue != 0 && cell.value == selectedValue &&
                            !cell.isError                                       -> CellSameValue
                    else                                                       -> CellNormal
                }
                drawRect(
                    color = bg,
                    topLeft = Offset(col * cSize, row * cSize),
                    size = Size(cSize, cSize)
                )
            }

            // Grid lines
            val thin  = 1.dp.toPx()
            val thick = 2.5.dp.toPx()
            val outer = 3.dp.toPx()

            for (i in 0..9) {
                val pos = i * cSize
                val isBox = i % 3 == 0
                val sw    = when { i == 0 || i == 9 -> outer; isBox -> thick; else -> thin }
                val color = if (isBox) GridThickLine else GridThinLine

                drawLine(color, Offset(0f, pos), Offset(size.width, pos), sw)
                drawLine(color, Offset(pos, 0f), Offset(pos, size.height), sw)
            }

            // Numbers and notes drawn directly on canvas
            cells.forEachIndexed { i, cell ->
                val row = i / 9
                val col = i % 9
                val x = col * cSize
                val y = row * cSize

                when {
                    cell.value != 0 -> {
                        val color = when {
                            cell.isError  -> NumberError
                            cell.isGiven  -> NumberGiven
                            else          -> NumberUser
                        }
                        val weight = if (cell.isGiven) FontWeight.Bold else FontWeight.Medium
                        val measured = textMeasurer.measure(
                            text = cell.value.toString(),
                            style = TextStyle(
                                fontSize = numberFontSize,
                                fontWeight = weight,
                                color = color
                            )
                        )
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(
                                x + (cSize - measured.size.width) / 2,
                                y + (cSize - measured.size.height) / 2
                            )
                        )
                    }

                    cell.notes.isNotEmpty() -> {
                        val noteCell = cSize / 3f
                        for (n in 1..9) {
                            if (n !in cell.notes) continue
                            val nr = (n - 1) / 3
                            val nc = (n - 1) % 3
                            val measured = textMeasurer.measure(
                                text = n.toString(),
                                style = TextStyle(
                                    fontSize = noteFontSize,
                                    color = NumberNote
                                )
                            )
                            drawText(
                                textLayoutResult = measured,
                                topLeft = Offset(
                                    x + nc * noteCell + (noteCell - measured.size.width) / 2,
                                    y + nr * noteCell + (noteCell - measured.size.height) / 2
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}