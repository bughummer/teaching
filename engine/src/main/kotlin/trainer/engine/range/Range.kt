package trainer.engine.range

import trainer.engine.Card

/** A weighted mix of hand classes: weight 1.0 = always play this class, 0.0 = never. */
class Range(private val weights: Map<HandClass, Double>) {

    fun weightOf(handClass: HandClass): Double = weights[handClass] ?: 0.0

    fun classes(): Set<HandClass> = weights.keys

    /** Total combo-weighted mass remaining in this range (for display/overlap scoring). */
    fun totalWeightedCombos(): Double = weights.entries.sumOf { it.value * it.key.comboCount }

    /** A new range keeping only combos consistent with [predicate], renormalized weights untouched (mass just drops). */
    fun filter(predicate: (HandClass) -> Boolean): Range =
        Range(weights.filterKeys(predicate))

    /** A new range with each class's weight scaled by [factor] (clamped to the class's own weight bounds by caller). */
    fun scaleWeights(factor: (HandClass, Double) -> Double): Range =
        Range(weights.mapValues { (handClass, w) -> factor(handClass, w).coerceIn(0.0, 1.0) })

    /** Randomly draws one concrete two-card hand from this range, weighted by class mass, excluding dead cards. */
    fun sample(dead: Set<Card>, random: kotlin.random.Random = kotlin.random.Random.Default): Pair<Card, Card>? {
        val candidates = weights.entries.flatMap { (handClass, w) ->
            if (w <= 0.0) emptyList() else handClass.combos(dead).map { it to w }
        }
        if (candidates.isEmpty()) return null
        val total = candidates.sumOf { it.second }
        var target = random.nextDouble() * total
        for ((combo, w) in candidates) {
            target -= w
            if (target <= 0.0) return combo
        }
        return candidates.last().first
    }

    /** All concrete combos in this range with positive weight, excluding dead cards, each tagged with its weight. */
    fun weightedCombos(dead: Set<Card>): List<Pair<Pair<Card, Card>, Double>> =
        weights.entries.filter { it.value > 0.0 }
            .flatMap { (handClass, w) -> handClass.combos(dead).map { it to w } }
}
