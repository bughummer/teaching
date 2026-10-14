package trainer.engine.range

/**
 * Ranks starting hands with a simplified version of the classic Chen formula (a well-known public
 * heuristic, not specific to any course) and builds ranges as "top N% of combos by that score."
 */
object DefaultRanges {

    private fun chenScore(handClass: HandClass): Double {
        fun highCardPoints(rank: Int) = when (rank) {
            14 -> 10.0; 13 -> 8.0; 12 -> 7.0; 11 -> 6.0; 10 -> 5.0
            else -> rank / 2.0
        }
        if (handClass.highRank == handClass.lowRank) {
            return (highCardPoints(handClass.highRank) * 2).coerceAtLeast(5.0)
        }
        var score = highCardPoints(handClass.highRank)
        if (handClass.suited == true) score += 2.0
        val gap = handClass.highRank - handClass.lowRank - 1
        score -= when {
            gap <= 0 -> 0.0
            gap == 1 -> 1.0
            gap == 2 -> 2.0
            gap == 3 -> 4.0
            else -> 5.0
        }
        if (gap <= 1 && handClass.highRank <= 12) score += 1.0
        return score
    }

    /** All 169 classes ordered strongest-first by Chen score (ties broken by combo count descending). */
    val strengthOrder: List<HandClass> by lazy {
        HandClass.all().sortedWith(compareByDescending<HandClass> { chenScore(it) }.thenByDescending { it.comboCount })
    }

    /**
     * A range containing the strongest classes by Chen score whose combined weighted combo count is
     * approximately [fraction] of all 1326 combos. The boundary class gets a partial (mixed) weight.
     */
    fun topFraction(fraction: Double): Range {
        require(fraction in 0.0..1.0)
        val targetCombos = fraction * 1326.0
        val weights = mutableMapOf<HandClass, Double>()
        var accumulated = 0.0
        for (handClass in strengthOrder) {
            if (accumulated >= targetCombos) break
            val remaining = targetCombos - accumulated
            val weight = (remaining / handClass.comboCount).coerceIn(0.0, 1.0)
            weights[handClass] = weight
            accumulated += handClass.comboCount * weight
        }
        return Range(weights)
    }

    fun openRange(seat: Seat): Range = topFraction(seat.openFraction)
    fun buttonOpenRange(): Range = openRange(Seat.BUTTON)

    /**
     * How wide the big blind defends depends on how wide the opener's range is: an under-the-gun
     * open is much more credible than a button open, so the big blind can afford to defend tighter
     * against it and still show a profit, and wider against a button open.
     */
    fun bigBlindDefendRange(heroOpenSeat: Seat = Seat.BUTTON): Range =
        topFraction((0.20 + heroOpenSeat.openFraction * 0.4).coerceAtMost(0.55))

    /**
     * The strongest slice of the defend range, treated as "would 3-bet instead of flatting." Width
     * scales with how wide the opener's own range is: three-betting a tight under-the-gun open
     * mostly gets called (their whole range is already strong), so the profitable 3-bet range there
     * stays narrow and value-heavy; three-betting a wide button open folds out far more hands, so
     * that 3-bet range can include a lot more bluffs.
     */
    fun bigBlindThreeBetSlice(heroOpenSeat: Seat = Seat.BUTTON): Range {
        val defend = bigBlindDefendRange(heroOpenSeat).classes()
        val threeBetFraction = 0.035 + heroOpenSeat.openFraction * 0.09
        val threeBetClasses = topFraction(threeBetFraction).classes()
        return Range(threeBetClasses.filter { it in defend }.associateWith { 1.0 })
    }

    /** The defend range minus the three-bet slice: hands that call rather than raise. */
    fun bigBlindCallRange(heroOpenSeat: Seat = Seat.BUTTON): Range {
        val threeBet = bigBlindThreeBetSlice(heroOpenSeat).classes()
        return bigBlindDefendRange(heroOpenSeat).filter { it !in threeBet }
    }
}

enum class Seat(val label: String, val openFraction: Double) {
    UNDER_THE_GUN("Under the Gun", 0.13),
    MIDDLE_POSITION("Middle Position", 0.20),
    CUTOFF("Cutoff", 0.30),
    BUTTON("Button", 0.45),
}
