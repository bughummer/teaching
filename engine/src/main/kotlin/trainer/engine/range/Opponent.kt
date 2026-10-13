package trainer.engine.range

/** Simple preflop/postflop tendencies, loosely modeled, not a solver. */
enum class Archetype(val label: String, val description: String) {
    ROCK(
        "The Rock",
        "Folds a lot, only continues with real strength. Easy to push off a hand, hard to bluff for value.",
    ),
    STATION(
        "The Station",
        "Calls far too often and almost never raises or folds once in. Bets rarely mean much; don't bluff this one.",
    ),
    SHARK(
        "The Shark",
        "Balanced and aggressive. Bets and raises for real reasons, including a healthy mix of bluffs.",
    );

    /** Multiplier applied to the baseline fold/call/3-bet split preflop. */
    val foldBias: Double get() = when (this) { ROCK -> 1.3; STATION -> 0.5; SHARK -> 1.0 }
    val threeBetBias: Double get() = when (this) { ROCK -> 0.7; STATION -> 0.2; SHARK -> 1.3 }

    /** How often this archetype folds to a continuation bet, before factoring in board/range equity. */
    val baseFoldToCBet: Double get() = when (this) { ROCK -> 0.65; STATION -> 0.25; SHARK -> 0.45 }
}

enum class PreflopAction { FOLD, CALL, THREE_BET }

object Opponent {
    fun preflopAction(archetype: Archetype, random: kotlin.random.Random): PreflopAction {
        val baseFold = 0.45 * archetype.foldBias
        val baseThreeBet = 0.12 * archetype.threeBetBias
        val fold = baseFold.coerceIn(0.05, 0.85)
        val threeBet = baseThreeBet.coerceIn(0.02, 0.35)
        val call = (1.0 - fold - threeBet).coerceAtLeast(0.05)
        val roll = random.nextDouble() * (fold + call + threeBet)
        return when {
            roll < fold -> PreflopAction.FOLD
            roll < fold + call -> PreflopAction.CALL
            else -> PreflopAction.THREE_BET
        }
    }

    /**
     * Whether this archetype folds to a flop continuation bet, factoring in how much their actual
     * continuing range connects with this board (less connection -> folds more).
     */
    fun foldsToCBet(archetype: Archetype, rangeEquityOnBoard: Double, random: kotlin.random.Random): Boolean {
        val adjustment = (0.5 - rangeEquityOnBoard) * 0.6
        val foldChance = (archetype.baseFoldToCBet + adjustment).coerceIn(0.1, 0.9)
        return random.nextDouble() < foldChance
    }
}
