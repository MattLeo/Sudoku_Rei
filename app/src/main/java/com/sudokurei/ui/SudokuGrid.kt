package com.sudokurei.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.sudokurei.game.CellState
import com.sudokurei.game.SudokuEngine
import com.sudokurei.ui.theme.*
import kotlinx.coroutines.launch

private val FlashColor = Color(0xFFFFD700)

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

    // ── Flash animation state ──────────────────────────────────────────────
    val scope = rememberCoroutineScope()
    val flashAlphas = remember { mutableStateMapOf<Int, Animatable<Float, AnimationVector1D>>() }
    val previouslyComplete = remember { mutableSetOf<String>() }

    LaunchedEffect(cells) {
        val nowComplete = mutableSetOf<String>()
        val newCells = mutableSetOf<Int>()

        // Rows
        for (row in 0..8) {
            val indices = (0..8).map { row * 9 + it }
            if (indices.all { cells[it].value != 0 && !cells[it].isError }) {
                val id = "row_$row"
                nowComplete.add(id)
                if (id !in previouslyComplete) newCells.addAll(indices)
            }
        }
        // Columns
        for (col in 0..8) {
            val indices = (0..8).map { it * 9 + col }
            if (indices.all { cells[it].value != 0 && !cells[it].isError }) {
                val id = "col_$col"
                nowComplete.add(id)
                if (id !in previouslyComplete) newCells.addAll(indices)
            }
        }
        // Boxes
        for (br in 0..2) for (bc in 0..2) {
            val indices = (0..2).flatMap { r -> (0..2).map { c -> (br * 3 + r) * 9 + bc * 3 + c } }
            if (indices.all { cells[it].value != 0 && !cells[it].isError }) {
                val id = "box_${br}_${bc}"
                nowComplete.add(id)
                if (id !in previouslyComplete) newCells.addAll(indices)
            }
        }

        previouslyComplete.clear()
        previouslyComplete.addAll(nowComplete)

        // Trigger fade for newly completed cells
        newCells.forEach { idx ->
            val anim = flashAlphas.getOrPut(idx) { Animatable(0f) }
            scope.launch {
                anim.snapTo(1f)
                anim.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    // Read alpha values in composable scope so changes trigger recomposition
    val flashSnapshot = flashAlphas.entries.associate { (k, v) -> k to v.value }

    // ── Grid ──────────────────────────────────────────────────────────────
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

        Canvas(Modifier.fillMaxSize()) {
            val cSize = size.width / 9f

            // 1. Cell backgrounds
            cells.forEachIndexed { i, cell ->
                val row = i / 9
                val col = i % 9
                val bg = when {
                    i == selectedCell                                        -> CellSelected
                    cell.isError                                             -> CellError
                    i in relatedCells                                        -> CellRelated
                    selectedValue != 0 && cell.value == selectedValue &&
                            !cell.isError                                    -> CellSameValue
                    else                                                     -> CellNormal
                }
                drawRect(
                    color = bg,
                    topLeft = Offset(col * cSize, row * cSize),
                    size = Size(cSize, cSize)
                )
            }

            // 2. Gold flash overlay (above backgrounds, below grid lines)
            flashSnapshot.forEach { (i, alpha) ->
                if (alpha > 0f) {
                    val row = i / 9
                    val col = i % 9
                    drawRect(
                        color = FlashColor.copy(alpha = alpha * 0.45f),
                        topLeft = Offset(col * cSize, row * cSize),
                        size = Size(cSize, cSize)
                    )
                }
            }

            // 3. Grid lines
            val thin  = 1.dp.toPx()
            val thick = 2.5.dp.toPx()
            val outer = 3.dp.toPx()

            for (i in 0..9) {
                val pos   = i * cSize
                val isBox = i % 3 == 0
                val sw    = when { i == 0 || i == 9 -> outer; isBox -> thick; else -> thin }
                val color = if (isBox) GridThickLine else GridThinLine

                drawLine(color, Offset(0f, pos), Offset(size.width, pos), sw)
                drawLine(color, Offset(pos, 0f), Offset(pos, size.height), sw)
            }

            // 4. Numbers and notes
            cells.forEachIndexed { i, cell ->
                val row = i / 9
                val col = i % 9
                val x   = col * cSize
                val y   = row * cSize

                when {
                    cell.value != 0 -> {
                        val color = when {
                            cell.isError -> NumberError
                            cell.isGiven -> NumberGiven
                            else         -> NumberUser
                        }
                        val measured = textMeasurer.measure(
                            text = cell.value.toString(),
                            style = TextStyle(
                                fontSize = numberFontSize,
                                fontWeight = if (cell.isGiven) FontWeight.Bold else FontWeight.Medium,
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
                                style = TextStyle(fontSize = noteFontSize, color = NumberNote)
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