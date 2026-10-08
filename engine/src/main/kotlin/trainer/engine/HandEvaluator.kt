package trainer.engine

/**
 * Ranks the best 5-card hand out of 5-7 cards.
 * Returns a comparable score: higher is better. Encodes category (0-8) and
 * kickers so two scores can be compared directly with > or <.
 */
object HandEvaluator {

    fun score(cards: List<Card>): Long {
        require(cards.size in 5..7) { "need 5-7 cards, got ${cards.size}" }
        return combinations(cards, 5).maxOf { scoreFive(it) }
    }

    private fun combinations(cards: List<Card>, k: Int): List<List<Card>> {
        if (k == 0) return listOf(emptyList())
        if (cards.size == k) return listOf(cards)
        val head = cards.first()
        val tail = cards.drop(1)
        val withHead = combinations(tail, k - 1).map { it + head }
        val withoutHead = combinations(tail, k)
        return withHead + withoutHead
    }

    private fun scoreFive(hand: List<Card>): Long {
        val ranks = hand.map { it.rank }.sortedDescending()
        val counts = ranks.groupingBy { it }.eachCount()
        val byCountThenRank = counts.entries.sortedWith(
            compareByDescending<Map.Entry<Int, Int>> { it.value }.thenByDescending { it.key }
        )
        val isFlush = hand.map { it.suit }.toSet().size == 1
        val straightHigh = straightHighCard(ranks.toSet())

        fun pack(category: Int, kickers: List<Int>): Long {
            var v = category.toLong()
            for (k in kickers) v = v * 16 + k
            return v
        }

        return when {
            isFlush && straightHigh != null -> pack(8, listOf(straightHigh))
            byCountThenRank[0].value == 4 -> pack(7, listOf(byCountThenRank[0].key, byCountThenRank[1].key))
            byCountThenRank[0].value == 3 && byCountThenRank[1].value == 2 ->
                pack(6, listOf(byCountThenRank[0].key, byCountThenRank[1].key))
            isFlush -> pack(5, ranks)
            straightHigh != null -> pack(4, listOf(straightHigh))
            byCountThenRank[0].value == 3 ->
                pack(3, listOf(byCountThenRank[0].key) + byCountThenRank.drop(1).map { it.key })
            byCountThenRank[0].value == 2 && byCountThenRank[1].value == 2 -> {
                val pairs = listOf(byCountThenRank[0].key, byCountThenRank[1].key).sortedDescending()
                val kicker = byCountThenRank[2].key
                pack(2, pairs + kicker)
            }
            byCountThenRank[0].value == 2 ->
                pack(1, listOf(byCountThenRank[0].key) + byCountThenRank.drop(1).map { it.key })
            else -> pack(0, ranks)
        }
    }

    /** Returns the high card of the best straight in this rank set, or null. Ace plays low (A-5) too. */
    private fun straightHighCard(ranks: Set<Int>): Int? {
        val withLowAce = if (14 in ranks) ranks + 1 else ranks
        var best: Int? = null
        for (high in 5..14) {
            if ((high - 4..high).all { it in withLowAce }) best = high
        }
        return best
    }

    fun categoryName(score: Long): String {
        return when {
            score >= pow16(8) -> "Straight flush"
            score >= pow16(7) -> "Four of a kind"
            score >= pow16(6) -> "Full house"
            score >= pow16(5) -> "Flush"
            score >= pow16(4) -> "Straight"
            score >= pow16(3) -> "Three of a kind"
            score >= pow16(2) -> "Two pair"
            score >= pow16(1) -> "Pair"
            else -> "High card"
        }
    }

    private fun pow16(category: Int): Long {
        var v = category.toLong()
        repeat(5) { v *= 16 }
        return v
    }
}
