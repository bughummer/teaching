package trainer.engine.range

import trainer.engine.Card
import trainer.engine.Deck
import trainer.engine.HandEvaluator

enum class FlopAction { CHECK, BET_SMALL, BET_BIG }

sealed class HandState {
    data class PreflopDecision(val heroHand: Pair<Card, Card>) : HandState()

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
    ) : HandState()

    data class FlopDecision(
        val heroHand: Pair<Card, Card>,
        val board: List<Card>,
        val pot: Int,
        val heroEquity: Double,
        val rangeAdvantage: Double,
    ) : HandState()

    data class HandEnded(val summary: String, val heroHand: Pair<Card, Card>, val board: List<Card>) : HandState()
}

/**
 * One playable hand: hero on the button facing a big-blind defender. Pure logic, no UI —
 * shortcut: pot/chip accounting is simplified (no exact blind bookkeeping across streets),
 * good enough for teaching equity and range concepts, not for real-money accuracy.
 */
class HandSession(
    private val archetype: Archetype,
    private val random: kotlin.random.Random = kotlin.random.Random.Default,
) {
    private val heroHand: Pair<Card, Card>
    private var board: List<Card> = emptyList()
    private var villainCallRange: Range = DefaultRanges.bigBlindCallRange()
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
                state = HandState.HandEndedPreflop(
                    heroHand,
                    "Villain three-bet. ${archetype.label}'s three-bet range here is narrow — " +
                        "standard play is to fold most hands and continue only with your own premiums. " +
                        "(Four-bet strategy isn't covered by this drill yet.)",
                )
            }
            PreflopAction.CALL -> {
                pot = 6
                board = dealExcluding(setOf(heroHand.first, heroHand.second), count = 3)
                state = HandState.RangeGuess(heroHand, board, pot)
            }
        }
    }

    fun submitRangeGuess(guessed: Set<HandClass>) {
        val current = state
        require(current is HandState.RangeGuess)
        val dead = setOf(heroHand.first, heroHand.second) + board
        val trueClasses = villainCallRange.classes().filter { it.combos(dead).isNotEmpty() }.toSet()

        val truePositives = guessed.intersect(trueClasses)
        val missed = trueClasses - guessed
        val falseAlarms = guessed - trueClasses
        val precision = if (guessed.isEmpty()) 0.0 else truePositives.size.toDouble() / guessed.size
        val recall = if (trueClasses.isEmpty()) 0.0 else truePositives.size.toDouble() / trueClasses.size

        state = HandState.RangeGuessResult(
            heroHand, board, pot, guessed, truePositives, missed, falseAlarms, precision, recall,
        )
    }

    fun proceedToFlopDecision() {
        val current = state
        require(current is HandState.RangeGuessResult)
        val heroEquity = Equity.vsRange(heroHand, board, villainCallRange, trials = 400, random = random).equity
        val rangeAdvantage = Equity.rangeVsRange(
            DefaultRanges.buttonOpenRange(), villainCallRange, board, trialsPerCombo = 20, random = random,
        )
        state = HandState.FlopDecision(heroHand, board, pot, heroEquity, rangeAdvantage)
    }

    fun chooseFlopAction(action: FlopAction) {
        val current = state
        require(current is HandState.FlopDecision)
        val dead = setOf(heroHand.first, heroHand.second) + board
        val villainHand = villainCallRange.sample(dead, random)
            ?: error("no villain combos left to sample")

        if (action == FlopAction.CHECK) {
            state = resolveShowdown(villainHand, current.pot, "You checked back.")
            return
        }

        val betSize = if (action == FlopAction.BET_SMALL) current.pot / 2 else current.pot
        val villainFolds = Opponent.foldsToCBet(archetype, current.rangeAdvantage, random)
        if (villainFolds) {
            state = HandState.HandEnded(
                "You bet $betSize and villain folded. You win the pot (+${current.pot}).",
                heroHand, board,
            )
        } else {
            state = resolveShowdown(villainHand, current.pot + betSize * 2, "You bet $betSize and got called.")
        }
    }

    private fun resolveShowdown(villainHand: Pair<Card, Card>, finalPot: Int, lead: String): HandState.HandEnded {
        val dead = setOf(heroHand.first, heroHand.second, villainHand.first, villainHand.second) + board
        val runout = dealExcluding(dead, count = 5 - board.size)
        val fullBoard = board + runout
        val heroScore = HandEvaluator.score(listOf(heroHand.first, heroHand.second) + fullBoard)
        val villainScore = HandEvaluator.score(listOf(villainHand.first, villainHand.second) + fullBoard)
        val outcome = when {
            heroScore > villainScore -> "You win the pot (+$finalPot) with ${HandEvaluator.categoryName(heroScore)} " +
                "vs villain's ${HandEvaluator.categoryName(villainScore)} (${villainHand.first}${villainHand.second})."
            heroScore < villainScore -> "Villain wins the pot with ${HandEvaluator.categoryName(villainScore)} " +
                "(${villainHand.first}${villainHand.second}) vs your ${HandEvaluator.categoryName(heroScore)}."
            else -> "Chop — both hands tie with ${HandEvaluator.categoryName(heroScore)}."
        }
        return HandState.HandEnded("$lead $outcome", heroHand, fullBoard)
    }

    /** Draws [count] cards from a fresh deck that aren't in [exclude] (draws extra to guarantee enough remain). */
    private fun dealExcluding(exclude: Set<Card>, count: Int): List<Card> =
        Deck().draw(count + exclude.size).filter { it !in exclude }.take(count)
}
