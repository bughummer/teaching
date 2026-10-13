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

sealed class HandState {
    data class PreflopDecision(val heroHand: Pair<Card, Card>, val seat: Seat) : HandState()

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

    /** Hero acts first (as the preflop raiser in position). */
    data class StreetDecision(
        val street: Street,
        val heroHand: Pair<Card, Card>,
        val board: List<Card>,
        val pot: Int,
        val heroEquity: Double,
        val rangeAdvantage: Double,
    ) : HandState()

    /** Villain led into hero instead of checking; hero can fold, call, or check-raise. */
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
 * One playable hand: hero opens from a chosen seat, heads-up against a big-blind defender, through
 * to river showdown. Pure logic, no UI — shortcut: pot/chip accounting tracks real bet sizes but
 * assumes heads-up stacks are always deep enough (no all-ins / short-stack side pots).
 */
class HandSession(
    private val archetype: Archetype,
    private val seat: Seat = Seat.BUTTON,
    private val random: kotlin.random.Random = kotlin.random.Random.Default,
) {
    private val heroHand: Pair<Card, Card>
    private var board: List<Card> = emptyList()
    private var villainRange: Range = DefaultRanges.bigBlindCallRange(seat)
    private var pot: Int = 0

    var state: HandState
        private set

    init {
        val deck = Deck()
        val cards = deck.draw(2)
        heroHand = cards[0] to cards[1]
        state = HandState.PreflopDecision(heroHand, seat)
    }

    fun openOrFold(open: Boolean) {
        val current = state
        require(current is HandState.PreflopDecision)
        if (!open) {
            state = HandState.HandEndedPreflop(heroHand, "You folded. No chips won or lost.")
            return
        }

        val basePot = SB + BB + OPEN
        when (Opponent.preflopAction(archetype, random)) {
            PreflopAction.FOLD -> {
                pot = basePot
                state = HandState.HandEndedPreflop(heroHand, "Villain folded preflop. You win the pot uncontested (+$pot).")
            }
            PreflopAction.THREE_BET -> {
                pot = basePot + (THREE_BET - BB)
                state = HandState.FacingThreeBet(heroHand, pot)
            }
            PreflopAction.CALL -> {
                pot = basePot + (OPEN - BB)
                villainRange = DefaultRanges.bigBlindCallRange(seat)
                dealFlopAndStartRangeGuess()
            }
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
                villainRange = DefaultRanges.bigBlindThreeBetSlice(seat)
                dealFlopAndStartRangeGuess()
            }

            ThreeBetResponse.FOUR_BET -> {
                val potAfterFourBet = current.pot + (FOUR_BET - OPEN)
                if (Opponent.foldsToFourBet(archetype, random)) {
                    state = HandState.HandEndedPreflop(
                        heroHand, "You four-bet and villain folded. You win the pot (+$potAfterFourBet).",
                    )
                } else {
                    pot = potAfterFourBet + (FOUR_BET - THREE_BET)
                    villainRange = DefaultRanges.topFraction(0.025)
                    dealFlopAndStartRangeGuess()
                }
            }
        }
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
            DefaultRanges.openRange(seat), villainRange, board, trialsPerCombo = 15, random = random,
        )
        return HandState.StreetDecision(street, heroHand, board, pot, heroEquity, rangeAdvantage)
    }

    fun chooseStreetAction(action: FlopAction) {
        val current = state
        require(current is HandState.StreetDecision)

        if (action == FlopAction.CHECK) {
            if (Opponent.leadsIntoHero(archetype, random)) {
                val betSize = current.pot / 2
                state = HandState.FacingBet(current.street, heroHand, board, current.pot, betSize, current.heroEquity)
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
        val continueFraction = Opponent.continueFraction(archetype, betSizeFactor, current.rangeAdvantage)
        if (random.nextDouble() >= continueFraction) {
            state = HandState.HandEnded(
                "You bet $betSize and villain folded. You win the pot (+${current.pot}).",
                heroHand, board,
            )
            return
        }

        pot = current.pot + betSize * 2
        villainRange = villainRange.narrowOnBoard(board, continueFraction)

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
                val raiseSize = current.pot
                val rangeAdvantage = Equity.rangeVsRange(
                    DefaultRanges.openRange(seat), villainRange, board, trialsPerCombo = 15, random = random,
                )
                val continueFraction = Opponent.continueFraction(archetype, 1.3, rangeAdvantage)
                if (random.nextDouble() >= continueFraction) {
                    state = HandState.HandEnded(
                        "You check-raised and villain folded. You win the pot (+${current.pot + current.betSize}).",
                        heroHand, board,
                    )
                } else {
                    villainRange = villainRange.narrowOnBoard(board, continueFraction)
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
