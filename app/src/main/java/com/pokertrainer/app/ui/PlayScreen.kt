package com.pokertrainer.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import trainer.engine.range.*

@Composable
fun ArchetypePickScreen(nav: NavHostController) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Play a Hand", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "You're on the button. Pick who's in the big blind.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Archetype.entries) { archetype ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { nav.navigate("play/${archetype.name}") },
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(archetype.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(archetype.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun PlayScreen(nav: NavHostController, archetypeName: String) {
    val archetype = remember(archetypeName) { Archetype.valueOf(archetypeName) }
    var session by remember(archetype) { mutableStateOf(HandSession(archetype)) }
    var uiState by remember(session) { mutableStateOf(session.state) }
    var guessed by remember(session) { mutableStateOf(setOf<HandClass>()) }

    fun refresh() { uiState = session.state }
    fun newHand() {
        session = HandSession(archetype)
        uiState = session.state
        guessed = emptySet()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("vs ${archetype.label}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        when (val state = uiState) {
            is HandState.PreflopDecision -> {
                Text("You're on the button. Your hand:")
                Spacer(Modifier.height(8.dp))
                CardRow(listOf(state.heroHand.first, state.heroHand.second))
                Spacer(Modifier.height(24.dp))
                Button(onClick = { session.openOrFold(true); refresh() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Open-raise")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { session.openOrFold(false); refresh() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Fold")
                }
            }

            is HandState.HandEndedPreflop -> {
                CardRow(listOf(state.heroHand.first, state.heroHand.second))
                Spacer(Modifier.height(16.dp))
                Text(state.summary, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(24.dp))
                EndHandButtons(onPlayAgain = { newHand() }, onBack = { nav.popBackStack() })
            }

            is HandState.RangeGuess -> {
                Text("Pot: ${state.pot}")
                Spacer(Modifier.height(8.dp))
                CardRow(listOf(state.heroHand.first, state.heroHand.second) + state.board)
                Spacer(Modifier.height(16.dp))
                Text(
                    "Villain called your raise. Tap every hand you think they could still have:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                RangeGridWidget(
                    cellColor = { if (it in guessed) Color(0xFF1565C0) else Color(0xFF9E9E9E) },
                    onTap = { handClass ->
                        guessed = if (handClass in guessed) guessed - handClass else guessed + handClass
                    },
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { session.submitRangeGuess(guessed); refresh() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Lock in guess")
                }
            }

            is HandState.RangeGuessResult -> {
                CardRow(listOf(state.heroHand.first, state.heroHand.second) + state.board)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Precision ${(state.precision * 100).toInt()}% · Recall ${(state.recall * 100).toInt()}%",
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Green = you correctly flagged it. Red = you flagged it but villain folded that hand here. " +
                        "Orange = still in villain's range but you missed it.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                RangeGridWidget(cellColor = { handClass ->
                    when {
                        handClass in state.truePositives -> Color(0xFF2E7D32)
                        handClass in state.falseAlarms -> Color(0xFFC62828)
                        handClass in state.missed -> Color(0xFFEF6C00)
                        else -> Color(0xFF9E9E9E)
                    }
                })
                Spacer(Modifier.height(16.dp))
                Button(onClick = { session.proceedToFlopDecision(); refresh() }, modifier = Modifier.fillMaxWidth()) {
                    Text("See the flop decision")
                }
            }

            is HandState.FlopDecision -> {
                Text("Pot: ${state.pot}")
                Spacer(Modifier.height(8.dp))
                CardRow(listOf(state.heroHand.first, state.heroHand.second) + state.board)
                Spacer(Modifier.height(12.dp))
                Text("Your hand's equity vs villain's range here: ${(state.heroEquity * 100).toInt()}%")
                Text("Your whole opening range's equity vs their range: ${(state.rangeAdvantage * 100).toInt()}%")
                Spacer(Modifier.height(4.dp))
                Text(
                    if (state.rangeAdvantage > 0.52)
                        "Your range is ahead here — a continuation bet applies real pressure."
                    else
                        "This board favors villain's range more than usual — betting every time is less automatic.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { session.chooseFlopAction(FlopAction.CHECK); refresh() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Check")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { session.chooseFlopAction(FlopAction.BET_SMALL); refresh() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Bet half pot (${state.pot / 2})")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { session.chooseFlopAction(FlopAction.BET_BIG); refresh() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Bet full pot (${state.pot})")
                }
            }

            is HandState.HandEnded -> {
                CardRow(listOf(state.heroHand.first, state.heroHand.second) + state.board)
                Spacer(Modifier.height(16.dp))
                Text(state.summary, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(24.dp))
                EndHandButtons(onPlayAgain = { newHand() }, onBack = { nav.popBackStack() })
            }
        }
    }
}

@Composable
private fun EndHandButtons(onPlayAgain: () -> Unit, onBack: () -> Unit) {
    Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) { Text("Play another hand") }
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onBack) { Text("Back") }
}
