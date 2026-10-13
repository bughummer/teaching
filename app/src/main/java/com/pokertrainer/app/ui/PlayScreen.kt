package com.pokertrainer.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import com.pokertrainer.app.HandHistoryStore
import trainer.engine.range.*

@Composable
fun SeatPickScreen(nav: NavHostController) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Play a Hand", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text("Pick the seat you're opening from.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Seat.entries) { seat ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { nav.navigate("opponentCount/${seat.name}") },
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(seat.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Opens about ${(seat.openFraction * 100).toInt()}% of hands.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OpponentCountScreen(nav: NavHostController, seatName: String) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Play a Hand", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text("How many opponents?", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { nav.navigate("archetypes/$seatName/1/0/_") },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("1 — heads-up (3-bets and check-raises included)") }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { nav.navigate("archetypes/$seatName/2/0/_") },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("2 — multi-way pot") }
    }
}

@Composable
fun ArchetypePickScreen(nav: NavHostController, seatName: String, count: Int, index: Int, chosenCsv: String) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Play a Hand", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            if (count == 1) "Pick who's in the big blind." else "Pick opponent ${index + 1} of $count.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Archetype.entries) { archetype ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val chosen = if (chosenCsv == "_") archetype.name else "$chosenCsv,${archetype.name}"
                        if (index + 1 < count) {
                            nav.navigate("archetypes/$seatName/$count/${index + 1}/$chosen")
                        } else {
                            nav.navigate("play/$seatName/$chosen")
                        }
                    },
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
fun PlayScreen(nav: NavHostController, seatName: String, archetypesCsv: String, history: HandHistoryStore) {
    val seat = remember(seatName) { Seat.valueOf(seatName) }
    val archetypes = remember(archetypesCsv) { archetypesCsv.split(",").map { Archetype.valueOf(it) } }
    val archetype = archetypes[0] // only meaningful heads-up (3-bet/check-raise states only reachable with one opponent)
    val opponentSummary = remember(archetypes) { archetypes.joinToString(" + ") { it.label } }
    var session by remember(archetypes, seat) { mutableStateOf(HandSession(archetypes, seat)) }
    var uiState by remember(session) { mutableStateOf(session.state) }
    var guessed by remember(session) { mutableStateOf(setOf<HandClass>()) }

    fun refresh() { uiState = session.state }
    fun newHand() {
        session = HandSession(archetypes, seat)
        uiState = session.state
        guessed = emptySet()
    }

    LaunchedEffect(uiState) {
        val summary = when (val s = uiState) {
            is HandState.HandEndedPreflop -> s.summary
            is HandState.HandEnded -> s.summary
            else -> null
        }
        summary?.let { history.record(seat.label, opponentSummary, it) }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("${seat.label} vs $opponentSummary", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        AnimatedContent(
            targetState = uiState,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "handState",
        ) { targetState ->
        Column {
        when (val state = targetState) {
            is HandState.PreflopDecision -> {
                Text("Your hand:")
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

            is HandState.FacingThreeBet -> {
                CardRow(listOf(state.heroHand.first, state.heroHand.second))
                Spacer(Modifier.height(16.dp))
                Text(
                    "${archetype.label} three-bet you. Their three-bet range here is narrow and genuinely strong — " +
                        "continuing needs a real hand, not just \"I opened, so I must defend.\"",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { session.respondToThreeBet(ThreeBetResponse.CALL); refresh() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Call and see a flop") }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { session.respondToThreeBet(ThreeBetResponse.FOUR_BET); refresh() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Four-bet") }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { session.respondToThreeBet(ThreeBetResponse.FOLD); refresh() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Fold") }
            }

            is HandState.HandEndedPreflop -> {
                CardRow(listOf(state.heroHand.first, state.heroHand.second))
                Spacer(Modifier.height(16.dp))
                Text(state.summary, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(24.dp))
                EndHandButtons(onPlayAgain = { newHand() }, onBack = { nav.popBackStack() })
            }

            is HandState.RangeGuess -> {
                AnimatedPotLabel(state.pot)
                Spacer(Modifier.height(8.dp))
                CardRow(listOf(state.heroHand.first, state.heroHand.second) + state.board)
                Spacer(Modifier.height(16.dp))
                Text(
                    "${state.opponentLabel} called. Drag across every hand you think they could still have " +
                        "(tap one to undo it)" + (if (state.remaining > 0) " — ${state.remaining} more opponent(s) after this" else "") + ":",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                RangeGridWidget(
                    cellColor = { if (it in guessed) Color(0xFF1565C0) else Color(0xFF9E9E9E) },
                    onTap = { handClass ->
                        guessed = if (handClass in guessed) guessed - handClass else guessed + handClass
                    },
                    onDragSelect = { handClass -> guessed = guessed + handClass },
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { guessed = emptySet() }) { Text("Clear selection") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { session.submitRangeGuess(guessed); refresh() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Lock in guess")
                }
            }

            is HandState.RangeGuessResult -> {
                var selected by remember(state) { mutableStateOf<HandClass?>(null) }
                CardRow(listOf(state.heroHand.first, state.heroHand.second) + state.board)
                Spacer(Modifier.height(12.dp))
                Text(
                    "${state.opponentLabel} — Precision ${(state.precision * 100).toInt()}% · Recall ${(state.recall * 100).toInt()}%",
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Green = you correctly flagged it. Red = you flagged it but villain folded that hand here. " +
                        "Orange = still in villain's range but you missed it. Tap any cell for why.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                RangeGridWidget(
                    cellColor = { handClass ->
                        when {
                            handClass in state.truePositives -> Color(0xFF2E7D32)
                            handClass in state.falseAlarms -> Color(0xFFC62828)
                            handClass in state.missed -> Color(0xFFEF6C00)
                            else -> Color(0xFF9E9E9E)
                        }
                    },
                    onTap = { selected = it },
                )
                selected?.let { handClass ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        session.explain(handClass, inTrueRange = handClass in state.truePositives || handClass in state.missed),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(12.dp))
                state.summaryLines.forEach { line ->
                    Text("• $line", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { session.proceedToFlopDecision(); refresh() }, modifier = Modifier.fillMaxWidth()) {
                    Text("See the flop decision")
                }
            }

            is HandState.StreetDecision -> {
                AnimatedPotLabel(state.pot, prefix = "${state.street.name.lowercase().replaceFirstChar { it.uppercase() }} · Pot: ")
                Spacer(Modifier.height(8.dp))
                CardRow(listOf(state.heroHand.first, state.heroHand.second) + state.board)
                Spacer(Modifier.height(12.dp))
                val rangeAdvantageOf = state.rangeAdvantageByOpponent.toMap()
                state.equityByOpponent.forEach { (label, equity) ->
                    val rangeAdvantage = rangeAdvantageOf[label] ?: 0.5
                    Text("vs $label — your hand: ${(equity * 100).toInt()}% · your range: ${(rangeAdvantage * 100).toInt()}%")
                }
                Spacer(Modifier.height(4.dp))
                val worstRangeAdvantage = state.rangeAdvantageByOpponent.minOf { it.second }
                Text(
                    if (worstRangeAdvantage > 0.52)
                        "Your range is ahead of everyone still in — a continuation bet applies real pressure."
                    else
                        "At least one opponent's range holds up well here — betting every time is less automatic.",
                    style = MaterialTheme.typography.bodySmall,
                )
                val worstEquity = state.equityByOpponent.minOf { it.second }
                Text(
                    if (worstEquity > 0.5)
                        "A bet here would be for value against everyone still in."
                    else
                        "A bet here would be a bluff against at least one opponent — you're not ahead of all of them.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { session.chooseStreetAction(FlopAction.CHECK); refresh() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Check")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { session.chooseStreetAction(FlopAction.BET_SMALL); refresh() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Bet half pot (${state.pot / 2})")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { session.chooseStreetAction(FlopAction.BET_BIG); refresh() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Bet full pot (${state.pot})")
                }
            }

            is HandState.FacingBet -> {
                AnimatedPotLabel(state.pot, prefix = "${state.street.name.lowercase().replaceFirstChar { it.uppercase() }} · Pot: ")
                Spacer(Modifier.height(8.dp))
                CardRow(listOf(state.heroHand.first, state.heroHand.second) + state.board)
                Spacer(Modifier.height(12.dp))
                Text("${archetype.label} bet ${state.betSize} into you.")
                Text("Your hand's equity here: ${(state.heroEquity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { session.respondToBet(FacingBetAction.CALL); refresh() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Call")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { session.respondToBet(FacingBetAction.RAISE); refresh() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Check-raise")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { session.respondToBet(FacingBetAction.FOLD); refresh() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Fold")
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
    }
}

@Composable
private fun EndHandButtons(onPlayAgain: () -> Unit, onBack: () -> Unit) {
    Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth()) { Text("Play another hand") }
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = onBack) { Text("Back") }
}
