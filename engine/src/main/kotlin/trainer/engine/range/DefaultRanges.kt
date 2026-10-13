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

    fun buttonOpenRange(): Range = topFraction(0.45)
    fun bigBlindDefendRange(): Range = topFraction(0.35)

    /** The strongest slice of the defend range, treated as "would 3-bet instead of flatting." */
    fun bigBlindThreeBetSlice(): Range {
        val defend = bigBlindDefendRange().classes()
        val threeBetClasses = topFraction(0.07).classes()
        return Range(threeBetClasses.filter { it in defend }.associateWith { 1.0 })
    }

    /** The defend range minus the three-bet slice: hands that call rather than raise. */
    fun bigBlindCallRange(): Range {
        val threeBet = bigBlindThreeBetSlice().classes()
        return bigBlindDefendRange().filter { it !in threeBet }
    }
}
