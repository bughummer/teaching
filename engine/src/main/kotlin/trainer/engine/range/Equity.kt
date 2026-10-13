package trainer.engine.range

import trainer.engine.Card
import trainer.engine.HandEvaluator
import trainer.engine.fullDeck

data class EquityResult(val winFraction: Double, val tieFraction: Double) {
    val equity: Double get() = winFraction + tieFraction / 2
}

object Equity {

    /**
     * Monte Carlo equity of [heroHand] vs every weighted combo in [villainRange] (skipping combos
     * blocked by hero's cards or the board), dealing the rest of the board randomly each trial.
     */
    fun vsRange(
        heroHand: Pair<Card, Card>,
        board: List<Card>,
        villainRange: Range,
        trials: Int = 400,
        random: kotlin.random.Random = kotlin.random.Random.Default,
    ): EquityResult {
        val dead = setOf(heroHand.first, heroHand.second) + board
        val villainCombos = villainRange.weightedCombos(dead)
        require(villainCombos.isNotEmpty()) { "villain range has no combos left after removing dead cards" }

        var winWeight = 0.0
        var tieWeight = 0.0
        var totalWeight = 0.0

        repeat(trials) {
            val (villainHand, weight) = villainCombos[random.nextInt(villainCombos.size)]
            val usedForBoard = dead + villainHand.first + villainHand.second
            val remainingDeck = fullDeck().filter { it !in usedForBoard }.shuffled(random)
            val fullBoard = board + remainingDeck.take(5 - board.size)

            val heroScore = HandEvaluator.score(listOf(heroHand.first, heroHand.second) + fullBoard)
            val villainScore = HandEvaluator.score(listOf(villainHand.first, villainHand.second) + fullBoard)

            totalWeight += weight
            when {
                heroScore > villainScore -> winWeight += weight
                heroScore == villainScore -> tieWeight += weight
            }
        }

        return EquityResult(winWeight / totalWeight, tieWeight / totalWeight)
    }

    /** Average equity of every combo in [heroRange] vs every combo in [villainRange] on this board. */
    fun rangeVsRange(
        heroRange: Range,
        villainRange: Range,
        board: List<Card>,
        trialsPerCombo: Int = 30,
        random: kotlin.random.Random = kotlin.random.Random.Default,
    ): Double {
        val heroCombos = heroRange.weightedCombos(board.toSet())
        if (heroCombos.isEmpty()) return 0.5
        var sum = 0.0
        var weightSum = 0.0
        for ((combo, weight) in heroCombos) {
            val dead = board.toSet() + combo.first + combo.second
            if (villainRange.weightedCombos(dead).isEmpty()) continue
            val result = vsRange(combo, board, villainRange, trialsPerCombo, random)
            sum += result.equity * weight
            weightSum += weight
        }
        return if (weightSum == 0.0) 0.5 else sum / weightSum
    }
}
