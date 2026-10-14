package trainer.engine.range

import trainer.engine.Card
import trainer.engine.Deck
import trainer.engine.HandEvaluator

enum class FlopAction { CHECK, BET_SMALL, BET_BIG }
enum class FacingBetAction { FOLD, CALL, RAISE }
enum class ThreeBetResponse { FOLD, CALL, FOUR_BET }
enum class Street { FLOP, TURN, RIVER }

/** Fixed preflop bet sizes in big-blind units (BB = 2): open 2.5x, 3-bet 3x the open, 4-bet ~2.3x the 3-bet. */
private const val SB = 1
private const val BB = 2
private const val OPEN = 5
private const val THREE_BET = 15
private const val FOUR_BET = 35

/** One seat still (or no longer) in the hand against hero. */
private class VillainSeat(val archetype: Archetype, val label: String, var range: Range, var folded: Boolean = false)

data class OpponentDisplay(val label: String, val folded: Boolean)

sealed class HandState {
    data class PreflopDecision(val heroHand: Pair<Card, Card>, val seat: Seat) : HandState()

    /** Only reached heads-up (one opponent): multi-way pots don't model a three-bet. */
    data class FacingThreeBet(val heroHand: Pair<Card, Card>, val pot: Int) : HandState()

    data class HandEndedPreflop(val heroHand: Pair<Card, Card>, val summary: String) : HandState()

    /** Guessing one opponent's range at a time; [opponentLabel] names which one, [remaining] counts those still to guess after this one. */
    data class RangeGuess(
        val heroHand: Pair<Card, Card>,
        val board: List<Card>,
        val pot: Int,
        val opponentLabel: String,
        val remaining: Int,
    ) : HandState()

    data class RangeGuessResult(
        val heroHand: Pair<Card, Card>,
        val board: List<Card>,
        val pot: Int,
        val opponentLabel: String,
        val guessed: Set<HandClass>,
        val truePositives: Set<HandClass>,
        val missed: Set<HandClass>,
        val falseAlarms: Set<HandClass>,
        val precision: Double,
        val recall: Double,
        val summaryLines: List<String>,
    ) : HandState()

    /** Hero acts first (as the preflop raiser in position). One equity/range-advantage entry per still-active opponent. */
    data class StreetDecision(
        val street: Street,
        val heroHand: Pair<Card, Card>,
        val board: List<Card>,
        val pot: Int,
        val equityByOpponent: List<Pair<String, Double>>,
        val rangeAdvantageByOpponent: List<Pair<String, Double>>,
    ) : HandState()

    /** Villain led into hero instead of checking; only reachable heads-up. Hero can fold, call, or check-raise. */
    data class FacingBet(
        val street: Street,
        val heroHand: Pair<Card, Card>,
        val board: List<Card>,
        val pot: Int,
        val betSize: Int,
        val heroEquity: Double,
    ) : HandState()

    data class HandEnded(val summary: String, val heroHand: Pair<Card, Card>, val board: List<Card>) : HandState()
}

/**
 * One playable hand: hero opens from a chosen seat against one or two opponents, through to river
 * showdown. Pure logic, no UI — shortcuts: pot/chip accounting assumes deep enough stacks (no
 * all-ins), a three-bet/four-bet is only modeled heads-up, and only the heads-up opponent ever
 * leads a bet into hero (a second opponent only ever folds or calls hero's own bets).
 */
class HandSession(
    opponentArchetypes: List<Archetype>,
    private val seat: Seat = Seat.BUTTON,
    private val random: kotlin.random.Random = kotlin.random.Random.Default,
) {
    constructor(archetype: Archetype, seat: Seat = Seat.BUTTON, random: kotlin.random.Random = kotlin.random.Random.Default) :
        this(listOf(archetype), seat, random)

    init {
        require(opponentArchetypes.size in 1..5) { "HandSession supports 1-5 opponents, got ${opponentArchetypes.size}" }
    }

    private val heroHand: Pair<Card, Card>
    private var board: List<Card> = emptyList()
    private val villains: List<VillainSeat> = run {
        val seatLabels = TableSeats.opponentSeatLabels(seat, opponentArchetypes.size)
        opponentArchetypes.mapIndexed { i, a ->
            val label = if (opponentArchetypes.size == 1) a.label else "${seatLabels[i]} · ${a.label}"
            VillainSeat(a, label, DefaultRanges.bigBlindCallRange(seat, opponentArchetypes.size))
        }
    }
    private var rangeGuessCursor = 0
    private var pot: Int = 0

    var state: HandState
        private set

    init {
        val deck = Deck()
        val cards = deck.draw(2)
        heroHand = cards[0] to cards[1]
        state = HandState.PreflopDecision(heroHand, seat)
    }

    private fun activeVillains() = villains.filter { !it.folded }

    /** Every opponent's table seat/archetype label and whether they've folded, for display. */
    fun opponentDisplays(): List<OpponentDisplay> = villains.map { OpponentDisplay(it.label, it.folded) }

    fun openOrFold(open: Boolean) {
        val current = state
        require(current is HandState.PreflopDecision)
        if (!open) {
            state = HandState.HandEndedPreflop(heroHand, "You folded. No chips won or lost.")
            return
        }

        val basePot = SB + BB + OPEN
        if (villains.size == 1) {
            when (Opponent.preflopAction(villains[0].archetype, random)) {
                PreflopAction.FOLD -> {
                    villains[0].folded = true
                    pot = basePot
                    state = HandState.HandEndedPreflop(heroHand, "Villain folded preflop. You win the pot uncontested (+$pot).")
                }
                PreflopAction.THREE_BET -> {
                    pot = basePot + (THREE_BET - BB)
                    state = HandState.FacingThreeBet(heroHand, pot)
                }
                PreflopAction.CALL -> {
                    villains[0].range = DefaultRanges.bigBlindCallRange(seat)
                    pot = basePot + (OPEN - BB)
                    dealFlopAndStartRangeGuesses()
                }
            }
            return
        }

        // Multi-way: the first seat is the blind defender, any further seats are cold-callers
        // (they've posted nothing yet, so calling costs them the full open amount, not OPEN - BB).
        pot = basePot
        villains.forEachIndexed { i, villain ->
            when (Opponent.multiwayPreflopAction(villain.archetype, random)) {
                SimplePreflopAction.FOLD -> villain.folded = true
                SimplePreflopAction.CALL -> pot += if (i == 0) (OPEN - BB) else OPEN
            }
        }

        if (activeVillains().isEmpty()) {
            state = HandState.HandEndedPreflop(heroHand, "Everyone folded preflop. You win the pot uncontested (+$pot).")
        } else {
            dealFlopAndStartRangeGuesses()
        }
    }

    fun respondToThreeBet(action: ThreeBetResponse) {
        val current = state
        require(current is HandState.FacingThreeBet)
        when (action) {
            ThreeBetResponse.FOLD ->
                state = HandState.HandEndedPreflop(heroHand, "You folded to the three-bet. No further chips lost.")

            ThreeBetResponse.CALL -> {
                pot = current.pot + (THREE_BET - OPEN)
                villains[0].range = DefaultRanges.bigBlindThreeBetSlice(seat, villains.size)
                dealFlopAndStartRangeGuesses()
            }

            ThreeBetResponse.FOUR_BET -> {
                val potAfterFourBet = current.pot + (FOUR_BET - OPEN)
                if (Opponent.foldsToFourBet(villains[0].archetype, random)) {
                    villains[0].folded = true
                    state = HandState.HandEndedPreflop(
                        heroHand, "You four-bet and villain folded. You win the pot (+$potAfterFourBet).",
                    )
                } else {
                    pot = potAfterFourBet + (FOUR_BET - THREE_BET)
                    villains[0].range = DefaultRanges.topFraction(0.025)
                    dealFlopAndStartRangeGuesses()
                }
            }
        }
    }

    private fun dealFlopAndStartRangeGuesses() {
        board = dealExcluding(setOf(heroHand.first, heroHand.second), count = 3)
        rangeGuessCursor = 0
        state = nextRangeGuessOrStreetDecision()
    }

    private fun nextRangeGuessOrStreetDecision(): HandState {
        val active = activeVillains()
        return if (rangeGuessCursor < active.size) {
            val villain = active[rangeGuessCursor]
            HandState.RangeGuess(heroHand, board, pot, villain.label, active.size - rangeGuessCursor - 1)
        } else {
            buildStreetDecision(Street.FLOP)
        }
    }

    fun submitRangeGuess(guessed: Set<HandClass>) {
        val current = state
        require(current is HandState.RangeGuess)
        val villain = activeVillains()[rangeGuessCursor]
        val dead = setOf(heroHand.first, heroHand.second) + board
        val trueClasses = villain.range.classes().filter { it.combos(dead).isNotEmpty() }.toSet()

        val truePositives = guessed.intersect(trueClasses)
        val missed = trueClasses - guessed
        val falseAlarms = guessed - trueClasses
        val precision = if (guessed.isEmpty()) 0.0 else truePositives.size.toDouble() / guessed.size
        val recall = if (trueClasses.isEmpty()) 0.0 else truePositives.size.toDouble() / trueClasses.size
        val summaryLines = RangeExplainer.summary(missed, falseAlarms, board, villain.archetype)

        state = HandState.RangeGuessResult(
            heroHand, board, pot, villain.label, guessed, truePositives, missed, falseAlarms, precision, recall, summaryLines,
        )
    }

    /** Short per-class reason, for showing alongside an individual missed/false-alarm cell. */
    fun explain(handClass: HandClass, inTrueRange: Boolean): String {
        val villain = activeVillains().getOrElse(rangeGuessCursor) { activeVillains().last() }
        return RangeExplainer.reason(handClass, board, villain.archetype, inTrueRange)
    }

    /** A short bluffing-relevant read (capped range, blockers) against each opponent it currently applies to. */
    fun bluffReadouts(): List<Pair<String, String>> =
        activeVillains().mapNotNull { v -> BluffReadout.note(heroHand, v.range, board)?.let { v.label to it } }

    fun proceedToFlopDecision() {
        val current = state
        require(current is HandState.RangeGuessResult)
        rangeGuessCursor++
        state = nextRangeGuessOrStreetDecision()
    }

    private fun buildStreetDecision(street: Street): HandState.StreetDecision {
        val active = activeVillains()
        val equities = active.map { v ->
            v.label to Equity.vsRange(heroHand, board, v.range, trials = 300, random = random).equity
        }
        val advantages = active.map { v ->
            v.label to Equity.rangeVsRange(DefaultRanges.openRange(seat, villains.size), v.range, board, trialsPerCombo = 15, random = random)
        }
        return HandState.StreetDecision(street, heroHand, board, pot, equities, advantages)
    }

    fun chooseStreetAction(action: FlopAction) {
        val current = state
        require(current is HandState.StreetDecision)
        val active = activeVillains()

        if (action == FlopAction.CHECK) {
            if (villains.size == 1 && Opponent.leadsIntoHero(active[0].archetype, random)) {
                val betSize = current.pot / 2
                state = HandState.FacingBet(current.street, heroHand, board, current.pot, betSize, current.equityByOpponent[0].second)
            } else if (current.street == Street.RIVER) {
                state = resolveShowdown(current.pot, "You checked it through.")
            } else {
                dealNextCard()
                state = buildStreetDecision(nextStreet(current.street))
            }
            return
        }

        val betSizeFactor = if (action == FlopAction.BET_SMALL) 0.5 else 1.0
        val betSize = (current.pot * betSizeFactor).toInt()
        val rangeAdvantageOf = current.rangeAdvantageByOpponent.toMap()
        var callers = 0
        for (villain in active) {
            val rangeAdvantage = rangeAdvantageOf[villain.label] ?: 0.5
            val continueFraction = Opponent.continueFraction(villain.archetype, betSizeFactor, rangeAdvantage)
            if (random.nextDouble() >= continueFraction) {
                villain.folded = true
            } else {
                villain.range = villain.range.narrowOnBoard(board, continueFraction)
                callers++
            }
        }

        if (callers == 0) {
            state = HandState.HandEnded(
                "You bet $betSize and everyone folded. You win the pot (+${current.pot}).",
                heroHand, board,
            )
            return
        }

        pot = current.pot + betSize * (1 + callers)
        if (current.street == Street.RIVER) {
            state = resolveShowdown(pot, "You bet $betSize and got called.")
        } else {
            dealNextCard()
            state = buildStreetDecision(nextStreet(current.street))
        }
    }

    fun respondToBet(action: FacingBetAction) {
        val current = state
        require(current is HandState.FacingBet)

        when (action) {
            FacingBetAction.FOLD ->
                state = HandState.HandEnded(
                    "You folded to villain's bet. Villain wins the pot (+${current.pot}).", heroHand, board,
                )

            FacingBetAction.CALL -> {
                pot = current.pot + current.betSize * 2
                // shortcut: a donk bet hero just calls doesn't narrow villain's displayed range —
                // modeling which classes lead here (vs. check) is a further refinement, not done yet.
                if (current.street == Street.RIVER) {
                    state = resolveShowdown(pot, "You called villain's bet.")
                } else {
                    dealNextCard()
                    state = buildStreetDecision(nextStreet(current.street))
                }
            }

            FacingBetAction.RAISE -> {
                val villain = activeVillains()[0]
                val raiseSize = current.pot
                val rangeAdvantage = Equity.rangeVsRange(
                    DefaultRanges.openRange(seat, villains.size), villain.range, board, trialsPerCombo = 15, random = random,
                )
                val continueFraction = Opponent.continueFraction(villain.archetype, 1.3, rangeAdvantage)
                if (random.nextDouble() >= continueFraction) {
                    state = HandState.HandEnded(
                        "You check-raised and villain folded. You win the pot (+${current.pot + current.betSize}).",
                        heroHand, board,
                    )
                } else {
                    villain.range = villain.range.narrowOnBoard(board, continueFraction)
                    pot = current.pot + current.betSize + raiseSize * 2
                    if (current.street == Street.RIVER) {
                        state = resolveShowdown(pot, "You check-raised and got called.")
                    } else {
                        dealNextCard()
                        state = buildStreetDecision(nextStreet(current.street))
                    }
                }
            }
        }
    }

    private fun nextStreet(street: Street): Street = when (street) {
        Street.FLOP -> Street.TURN
        Street.TURN -> Street.RIVER
        Street.RIVER -> error("no street after river")
    }

    private fun dealNextCard() {
        board = board + dealExcluding(setOf(heroHand.first, heroHand.second) + board, count = 1)
    }

    private fun resolveShowdown(finalPot: Int, lead: String): HandState.HandEnded {
        var dead = setOf(heroHand.first, heroHand.second) + board
        val contenders = mutableListOf("You" to HandEvaluator.score(listOf(heroHand.first, heroHand.second) + board))
        for (villain in activeVillains()) {
            val hand = villain.range.sample(dead, random) ?: error("no combos left to sample for ${villain.label}")
            dead = dead + hand.first + hand.second
            contenders.add("${villain.label} (${hand.first}${hand.second})" to HandEvaluator.score(listOf(hand.first, hand.second) + board))
        }

        val bestScore = contenders.maxOf { it.second }
        val winners = contenders.filter { it.second == bestScore }
        val outcome = if (winners.size > 1) {
            "Chop between ${winners.joinToString(" and ") { it.first }} with ${HandEvaluator.categoryName(bestScore)}."
        } else {
            val (winnerName, winnerScore) = winners.first()
            val others = contenders.filter { it.first != winnerName }
                .joinToString(", ") { "${it.first}'s ${HandEvaluator.categoryName(it.second)}" }
            if (winnerName == "You") {
                "You win the pot (+$finalPot) with ${HandEvaluator.categoryName(winnerScore)} vs $others."
            } else {
                "$winnerName wins the pot with ${HandEvaluator.categoryName(winnerScore)} vs your ${HandEvaluator.categoryName(contenders[0].second)}."
            }
        }
        return HandState.HandEnded("$lead $outcome", heroHand, board)
    }

    /** Draws [count] cards from a fresh deck that aren't in [exclude] (draws extra to guarantee enough remain). */
    private fun dealExcluding(exclude: Set<Card>, count: Int): List<Card> =
        Deck().draw(count + exclude.size).filter { it !in exclude }.take(count)
}
