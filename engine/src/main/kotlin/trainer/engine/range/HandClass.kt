package trainer.engine.range

import trainer.engine.Card
import trainer.engine.Suit

/**
 * One of the 169 distinct starting-hand classes (e.g. "AKs", "72o", "TT").
 * Two card classes of the same rank pair are always "paired" with suited=null.
 */
data class HandClass(val highRank: Int, val lowRank: Int, val suited: Boolean?) {
    init {
        require(highRank in 2..14 && lowRank in 2..14)
        if (highRank == lowRank) require(suited == null) else require(suited != null)
        require(highRank >= lowRank)
    }

    /** How many of the 1326 concrete two-card combos belong to this class. */
    val comboCount: Int
        get() = when {
            highRank == lowRank -> 6
            suited == true -> 4
            else -> 12
        }

    val label: String
        get() {
            fun r(x: Int) = when (x) { 14 -> "A"; 13 -> "K"; 12 -> "Q"; 11 -> "J"; 10 -> "T"; else -> x.toString() }
            return if (highRank == lowRank) "${r(highRank)}${r(highRank)}" else "${r(highRank)}${r(lowRank)}${if (suited == true) "s" else "o"}"
        }

    /** All concrete card-pair combos for this class, excluding any cards in [dead]. */
    fun combos(dead: Set<Card> = emptySet()): List<Pair<Card, Card>> {
        val result = mutableListOf<Pair<Card, Card>>()
        if (highRank == lowRank) {
            for (i in Suit.entries.indices) for (j in i + 1 until Suit.entries.size) {
                val a = Card(highRank, Suit.entries[i]); val b = Card(highRank, Suit.entries[j])
                if (a !in dead && b !in dead) result.add(a to b)
            }
        } else if (suited == true) {
            for (s in Suit.entries) {
                val a = Card(highRank, s); val b = Card(lowRank, s)
                if (a !in dead && b !in dead) result.add(a to b)
            }
        } else {
            for (s1 in Suit.entries) for (s2 in Suit.entries) {
                if (s1 == s2) continue
                val a = Card(highRank, s1); val b = Card(lowRank, s2)
                if (a !in dead && b !in dead) result.add(a to b)
            }
        }
        return result
    }

    companion object {
        /** All 169 classes, ordered strongest-ish to weakest by a simple heuristic (see [HandStrengthOrder]). */
        fun all(): List<HandClass> {
            val list = mutableListOf<HandClass>()
            for (hi in 14 downTo 2) {
                list.add(HandClass(hi, hi, null))
                for (lo in hi - 1 downTo 2) {
                    list.add(HandClass(hi, lo, true))
                    list.add(HandClass(hi, lo, false))
                }
            }
            return list
        }
    }
}
