package com.pokertrainer.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import trainer.engine.range.HandClass

private val ranksDescending = listOf(14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2)
private val CELL_SIZE = 22.dp

fun gridClasses(): List<List<HandClass>> =
    ranksDescending.indices.map { r ->
        ranksDescending.indices.map { c ->
            val rowRank = ranksDescending[r]
            val colRank = ranksDescending[c]
            when {
                r == c -> HandClass(rowRank, rowRank, null)
                r < c -> HandClass(rowRank, colRank, true)
                else -> HandClass(colRank, rowRank, false)
            }
        }
    }

/**
 * A 13x13 starting-hand grid. [cellColor] decides each cell's fill; pass [onTap] to make a single
 * cell tappable (used for the results screen's tap-to-explain), or [onDragSelect] to let a
 * finger-drag paint many cells at once (used for the range-guessing drill, so picking ~20 hands
 * doesn't take ~20 taps). Either, both, or neither can be passed.
 */
@Composable
fun RangeGridWidget(
    cellColor: (HandClass) -> Color,
    modifier: Modifier = Modifier,
    onTap: ((HandClass) -> Unit)? = null,
    onDragSelect: ((HandClass) -> Unit)? = null,
) {
    val grid = remember { gridClasses() }
    val density = LocalDensity.current
    val cellPx = with(density) { CELL_SIZE.toPx() }

    val dragModifier = if (onDragSelect != null) {
        Modifier.pointerInput(onDragSelect) {
            detectDragGestures { change, _ ->
                val col = (change.position.x / cellPx).toInt().coerceIn(0, grid.lastIndex)
                val row = (change.position.y / cellPx).toInt().coerceIn(0, grid.lastIndex)
                onDragSelect(grid[row][col])
            }
        }
    } else Modifier

    Column(modifier = modifier.then(dragModifier)) {
        grid.forEach { row ->
            Row {
                row.forEach { handClass ->
                    val animatedColor by animateColorAsState(cellColor(handClass), label = "cell")
                    val base = Modifier.size(CELL_SIZE).background(animatedColor)
                    val cellModifier = if (onTap != null) base.clickable { onTap(handClass) } else base
                    Box(cellModifier, contentAlignment = Alignment.Center) {
                        Text(handClass.label, fontSize = 7.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
