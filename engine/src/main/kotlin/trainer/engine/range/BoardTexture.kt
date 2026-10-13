package trainer.engine.range

import trainer.engine.Card

object BoardTexture {
    /** Rough 0 (bone dry) to 1 (very wet) wetness score for a 3-5 card board. */
    fun wetness(board: List<Card>): Double {
        val suitCounts = board.groupingBy { it.suit }.eachCount()
        val maxSuited = suitCounts.values.maxOrNull() ?: 1
        val flushComponent = when (maxSuited) { 1 -> 0.0; 2 -> 0.5; else -> 1.0 }

        val ranks = board.map { it.rank }.sorted()
        val span = ranks.last() - ranks.first()
        val straightComponent = when {
            span <= 2 -> 1.0
            span <= 4 -> 0.6
            else -> 0.2
        }
        return ((flushComponent + straightComponent) / 2).coerceIn(0.0, 1.0)
    }
}
