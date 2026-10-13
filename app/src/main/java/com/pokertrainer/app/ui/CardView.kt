package com.pokertrainer.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun CardRow(cards: List<EngineCard>, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        cards.forEach { PlayingCard(it) }
    }
}
