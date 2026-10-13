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

    /** Baseline fraction of this archetype's range that continues facing a postflop bet, before board/sizing. */
    val baseContinueFraction: Double get() = when (this) { ROCK -> 0.35; STATION -> 0.75; SHARK -> 0.55 }

    /** How often this archetype folds to a four-bet (a much smaller, tighter calling range than a 3-bet). */
    val fourBetFoldFrequency: Double get() = when (this) { ROCK -> 0.75; STATION -> 0.35; SHARK -> 0.55 }

    /** How often this archetype bets into the preflop raiser instead of checking to them (a "donk" lead). */
    val leadFrequency: Double get() = when (this) { ROCK -> 0.08; STATION -> 0.3; SHARK -> 0.2 }
}

enum class PreflopAction { FOLD, CALL, THREE_BET }
enum class SimplePreflopAction { FOLD, CALL }

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
     * Fraction of this archetype's range that continues facing a bet of this size, on this board.
     * Used both to decide whether the single sampled villain hand folds, and to narrow the
     * on-screen range afterward, so the two always agree with each other.
     */
    fun continueFraction(archetype: Archetype, betSizeFactor: Double, rangeAdvantage: Double): Double {
        val sizeAdjustment = -(betSizeFactor - 0.5) * 0.3
        val boardAdjustment = -(rangeAdvantage - 0.5) * 0.6
        return (archetype.baseContinueFraction + sizeAdjustment + boardAdjustment).coerceIn(0.15, 0.95)
    }

    fun foldsToBet(archetype: Archetype, betSizeFactor: Double, rangeAdvantage: Double, random: kotlin.random.Random): Boolean =
        random.nextDouble() >= continueFraction(archetype, betSizeFactor, rangeAdvantage)

    /**
     * Fold-or-call only, no three-bet: used for a second opponent in a multi-way pot, who is
     * modeled as a cold-caller rather than a blind defender (shortcut: multi-way pots don't model
     * anyone three-betting, to keep the number of branches bounded).
     */
    fun multiwayPreflopAction(archetype: Archetype, random: kotlin.random.Random): SimplePreflopAction {
        val fold = (0.5 * archetype.foldBias).coerceIn(0.1, 0.85)
        return if (random.nextDouble() < fold) SimplePreflopAction.FOLD else SimplePreflopAction.CALL
    }

    fun foldsToFourBet(archetype: Archetype, random: kotlin.random.Random): Boolean =
        random.nextDouble() < archetype.fourBetFoldFrequency

    fun leadsIntoHero(archetype: Archetype, random: kotlin.random.Random): Boolean =
        random.nextDouble() < archetype.leadFrequency
}
