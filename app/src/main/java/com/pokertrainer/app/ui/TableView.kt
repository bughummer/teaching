package com.pokertrainer.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import trainer.engine.Card as EngineCard
import trainer.engine.range.OpponentDisplay

private val FELT_GREEN = Color(0xFF14532D)
private val RAIL_BROWN = Color(0xFF3E2723)

/** A green felt table: opponent seats along the top, community board and pot in the middle. */
@Composable
fun PokerTable(
    opponents: List<OpponentDisplay>,
    board: List<EngineCard>,
    pot: Int? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 190.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(FELT_GREEN)
            .border(BorderStroke(5.dp, RAIL_BROWN), RoundedCornerShape(100.dp))
            .padding(vertical = 14.dp, horizontal = 10.dp),
    ) {
        Row(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            opponents.forEach { opponent -> SeatChip(opponent) }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (board.isNotEmpty()) {
                CardRow(board)
                Spacer(Modifier.height(8.dp))
            }
            if (pot != null) AnimatedPotLabel(pot, prefix = "Pot: ")
        }
    }
}

private data class PositionLabel(val name: String, val alignment: Alignment)

private val SIX_MAX_LAYOUT = listOf(
    PositionLabel("UTG", Alignment.TopCenter),
    PositionLabel("MP", Alignment.TopEnd),
    PositionLabel("CO", Alignment.BottomEnd),
    PositionLabel("BTN", Alignment.BottomCenter),
    PositionLabel("SB", Alignment.BottomStart),
    PositionLabel("BB", Alignment.TopStart),
)

/** A simplified 6-max table diagram: where every named position sits, [highlight] picked out in gold. */
@Composable
fun SeatDiagram(highlight: String? = null, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(80.dp))
            .background(FELT_GREEN)
            .border(BorderStroke(4.dp, RAIL_BROWN), RoundedCornerShape(80.dp))
            .padding(12.dp),
    ) {
        SIX_MAX_LAYOUT.forEach { position ->
            val isHighlighted = position.name == highlight
            Box(
                modifier = Modifier
                    .align(position.alignment)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isHighlighted) Color(0xFFC9A227) else Color(0xFF5D4037)),
                contentAlignment = Alignment.Center,
            ) {
                Text(position.name, color = Color.White, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun SeatChip(opponent: OpponentDisplay) {
    val alpha = if (opponent.folded) 0.4f else 1f
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF5D4037).copy(alpha = alpha)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                opponent.label.substringBefore(" ").take(3),
                color = Color.White.copy(alpha = alpha),
                fontSize = 10.sp,
            )
        }
        Text(
            if (opponent.folded) "${opponent.label} (folded)" else opponent.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
