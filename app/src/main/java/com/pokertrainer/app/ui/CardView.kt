package com.pokertrainer.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import trainer.engine.Card as EngineCard
import trainer.engine.Suit

@Composable
fun PlayingCard(card: EngineCard, modifier: Modifier = Modifier) {
    val isRed = card.suit == Suit.HEARTS || card.suit == Suit.DIAMONDS
    val color = if (isRed) Color(0xFFC62828) else Color(0xFF1B1B1B)
    Card(
        modifier = modifier.size(width = 48.dp, height = 68.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFBBBBBB)),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(card.label, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text(card.suit.symbol, color = color, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * A row of cards that deal themselves in: each card slides down and fades in, staggered by
 * position. Keying each card by its own identity plus its index means cards already on screen
 * (e.g. the flop) don't replay their animation when a new card (the turn) is appended after them.
 */
/** Pot text that counts up/down smoothly instead of snapping when the pot changes. */
@Composable
fun AnimatedPotLabel(pot: Int, modifier: Modifier = Modifier, prefix: String = "Pot: ") {
    val animatedPot by animateIntAsState(targetValue = pot, animationSpec = tween(400), label = "pot")
    Text("$prefix$animatedPot", modifier = modifier)
}

@Composable
fun CardRow(cards: List<EngineCard>, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        cards.forEachIndexed { index, card ->
            key(card, index) {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(animationSpec = tween(durationMillis = 250, delayMillis = index * 80)) { -it } +
                        fadeIn(animationSpec = tween(durationMillis = 250, delayMillis = index * 80)),
                ) {
                    PlayingCard(card)
                }
            }
        }
    }
}
