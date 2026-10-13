package trainer.engine.range

import trainer.engine.Card
import trainer.engine.Deck
import trainer.engine.HandEvaluator

enum class FlopAction { CHECK, BET_SMALL, BET_BIG }
enum class Street { FLOP, TURN, RIVER }

sealed class HandState {
    data class PreflopDecision(val heroHand: Pair<Card, Card>) : HandState()

    /** Villain three-bet; hero decides whether to continue with their narrow range. */
    data class FacingThreeBet(val heroHand: Pair<Card, Card>, val pot: Int) : HandState()

    data class HandEndedPreflop(val heroHand: Pair<Card, Card>, val summary: String) : HandState()

    data class RangeGuess(val heroHand: Pair<Card, Card>, val board: List<Card>, val pot: Int) : HandState()

    data class RangeGuessResult(
        val heroHand: Pair<Card, Card>,
        val board: List<Card>,
        val pot: Int,
        val guessed: Set<HandClass>,
        val truePositives: Set<HandClass>,
        val missed: Set<HandClass>,
        val falseAlarms: Set<HandClass>,
        val precision: Double,
        val recall: Double,
        val summaryLines: List<String>,
    ) : HandState()

    data class StreetDecision(
        val street: Street,
        val heroHand: Pair<Card, Card>,
        val board: List<Card>,
        val pot: Int,
        val heroEquity: Double,
        val rangeAdvantage: Double,
    ) : HandState()

    data class HandEnded(val summary: String, val heroHand: Pair<Card, Card>, val board: List<Card>) : HandState()
}

/**
 * One playable hand: hero on the button facing a big-blind defender, through to river showdown.
 * Pure logic, no UI — shortcut: pot/chip accounting is simplified (no exact blind bookkeeping,
 * no 4-bets), good enough for teaching equity and range concepts, not real-money accuracy.
 */
class HandSession(
    private val archetype: Archetype,
    private val random: kotlin.random.Random = kotlin.random.Random.Default,
) {
    private val heroHand: Pair<Card, Card>
    private var board: List<Card> = emptyList()
    private var villainRange: Range = DefaultRanges.bigBlindCallRange()
    private var pot: Int = 0

    var state: HandState
        private set

    init {
        val deck = Deck()
        val cards = deck.draw(2)
        heroHand = cards[0] to cards[1]
        state = HandState.PreflopDecision(heroHand)
    }

    fun openOrFold(open: Boolean) {
        val current = state
        require(current is HandState.PreflopDecision)
        if (!open) {
            state = HandState.HandEndedPreflop(heroHand, "You folded. No chips won or lost.")
            return
        }

        when (Opponent.preflopAction(archetype, random)) {
            PreflopAction.FOLD -> {
                pot = 3
                state = HandState.HandEndedPreflop(heroHand, "Villain folded preflop. You win the pot uncontested (+$pot).")
            }
            PreflopAction.THREE_BET -> {
                pot = 9
                state = HandState.FacingThreeBet(heroHand, pot)
            }
            PreflopAction.CALL -> {
                pot = 6
                dealFlopAndStartRangeGuess()
            }
        }
    }

    fun respondToThreeBet(call: Boolean) {
        val current = state
        require(current is HandState.FacingThreeBet)
        if (!call) {
            state = HandState.HandEndedPreflop(heroHand, "You folded to the three-bet. No further chips lost.")
            return
        }
        // Continuing means facing ${archetype.label}'s genuinely strong three-bet range for the rest of the hand.
        villainRange = DefaultRanges.bigBlindThreeBetSlice()
        dealFlopAndStartRangeGuess()
    }

    private fun dealFlopAndStartRangeGuess() {
        board = dealExcluding(setOf(heroHand.first, heroHand.second), count = 3)
        state = HandState.RangeGuess(heroHand, board, pot)
    }

    fun submitRangeGuess(guessed: Set<HandClass>) {
        val current = state
        require(current is HandState.RangeGuess)
        val dead = setOf(heroHand.first, heroHand.second) + board
        val trueClasses = villainRange.classes().filter { it.combos(dead).isNotEmpty() }.toSet()

        val truePositives = guessed.intersect(trueClasses)
        val missed = trueClasses - guessed
        val falseAlarms = guessed - trueClasses
        val precision = if (guessed.isEmpty()) 0.0 else truePositives.size.toDouble() / guessed.size
        val recall = if (trueClasses.isEmpty()) 0.0 else truePositives.size.toDouble() / trueClasses.size
        val summaryLines = RangeExplainer.summary(missed, falseAlarms, board, archetype)

        state = HandState.RangeGuessResult(
            heroHand, board, pot, guessed, truePositives, missed, falseAlarms, precision, recall, summaryLines,
        )
    }

    /** Short per-class reason, for showing alongside an individual missed/false-alarm cell. */
    fun explain(handClass: HandClass, inTrueRange: Boolean): String =
        RangeExplainer.reason(handClass, board, archetype, inTrueRange)

    fun proceedToFlopDecision() {
        val current = state
        require(current is HandState.RangeGuessResult)
        state = buildStreetDecision(Street.FLOP)
    }

    private fun buildStreetDecision(street: Street): HandState.StreetDecision {
        val heroEquity = Equity.vsRange(heroHand, board, villainRange, trials = 300, random = random).equity
        val rangeAdvantage = Equity.rangeVsRange(
            DefaultRanges.buttonOpenRange(), villainRange, board, trialsPerCombo = 15, random = random,
        )
        return HandState.StreetDecision(street, heroHand, board, pot, heroEquity, rangeAdvantage)
    }

    fun chooseStreetAction(action: FlopAction) {
        val current = state
        require(current is HandState.StreetDecision)

        if (action == FlopAction.CHECK) {
            if (current.street == Street.RIVER) {
                state = resolveShowdown(current.pot, "You checked it through.")
            } else {
                dealNextCard()
                state = buildStreetDecision(nextStreet(current.street))
            }
            return
        }

        val betSize = if (action == FlopAction.BET_SMALL) current.pot / 2 else current.pot
        val villainFolds = Opponent.foldsToCBet(archetype, current.rangeAdvantage, random)
        if (villainFolds) {
            state = HandState.HandEnded(
                "You bet $betSize and villain folded. You win the pot (+${current.pot}).",
                heroHand, board,
            )
            return
        }

        pot = current.pot + betSize * 2
        val keepFraction = if (action == FlopAction.BET_SMALL) 0.75 else 0.5
        villainRange = villainRange.narrowOnBoard(board, keepFraction)

        if (current.street == Street.RIVER) {
            state = resolveShowdown(pot, "You bet $betSize and got called.")
        } else {
            dealNextCard()
            state = buildStreetDecision(nextStreet(current.street))
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
        val dead = setOf(heroHand.first, heroHand.second) + board
        val villainHand = villainRange.sample(dead, random) ?: error("no villain combos left to sample")
        val heroScore = HandEvaluator.score(listOf(heroHand.first, heroHand.second) + board)
        val villainScore = HandEvaluator.score(listOf(villainHand.first, villainHand.second) + board)
        val outcome = when {
            heroScore > villainScore -> "You win the pot (+$finalPot) with ${HandEvaluator.categoryName(heroScore)} " +
                "vs villain's ${HandEvaluator.categoryName(villainScore)} (${villainHand.first}${villainHand.second})."
            heroScore < villainScore -> "Villain wins the pot with ${HandEvaluator.categoryName(villainScore)} " +
                "(${villainHand.first}${villainHand.second}) vs your ${HandEvaluator.categoryName(heroScore)}."
            else -> "Chop — both hands tie with ${HandEvaluator.categoryName(heroScore)}."
        }
        return HandState.HandEnded("$lead $outcome", heroHand, board)
    }

    /** Draws [count] cards from a fresh deck that aren't in [exclude] (draws extra to guarantee enough remain). */
    private fun dealExcluding(exclude: Set<Card>, count: Int): List<Card> =
        Deck().draw(count + exclude.size).filter { it !in exclude }.take(count)
}
