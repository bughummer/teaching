package com.pokertrainer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import trainer.engine.range.HandClass

private val ranksDescending = listOf(14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2)

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
 * A 13x13 starting-hand grid. [cellColor] decides each cell's fill; pass [onTap] to make cells
 * tappable (used for the "guess the range" drill), or omit it for a read-only reveal.
 */
@Composable
fun RangeGridWidget(
    cellColor: (HandClass) -> Color,
    modifier: Modifier = Modifier,
    onTap: ((HandClass) -> Unit)? = null,
) {
    val grid = remember { gridClasses() }
    Column(modifier = modifier) {
        grid.forEach { row ->
            Row {
                row.forEach { handClass ->
                    val base = Modifier
                        .size(22.dp)
                        .background(cellColor(handClass))
                    val cellModifier = if (onTap != null) base.clickable { onTap(handClass) } else base
                    Box(cellModifier, contentAlignment = Alignment.Center) {
                        Text(handClass.label, fontSize = 7.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
