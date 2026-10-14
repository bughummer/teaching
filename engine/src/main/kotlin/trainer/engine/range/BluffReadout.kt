package trainer.engine.range

import trainer.engine.Card
import trainer.engine.HandEvaluator

/**
 * A short read on whether this is a good bluffing spot (Chapters 10-11): is villain's range
 * capped -- no monster classes left despite a wet board, since this engine never models a
 * villain raise, only fold/call -- and how many of their strongest remaining combos does hero's
 * own hand block? Card removal is already correct everywhere else (every [Range] sample excludes
 * hero's hole cards), so this is purely about surfacing that signal as a decision aid, not new math.
 */
object BluffReadout {

    /** True when the board is wet enough that a real hand would usually have raised already, but
     * villain's remaining range contains no monster classes: their range reads as capped. */
    fun isCapped(villainRange: Range, board: List<Card>): Boolean {
        if (BoardTexture.wetness(board) < 0.5) return false
        return villainRange.classes().none { RangeExplainer.isMonsterClass(it, board) }
    }

    /**
     * Of villain's strongest remaining combos (top [topFraction] of their range by board
     * strength), what fraction no longer exist once hero's own hole cards are excluded. A high
     * fraction means a bluff is more credible: villain is less likely to hold what they'd need
     * to call.
     */
    fun blockedFraction(heroHand: Pair<Card, Card>, villainRange: Range, board: List<Card>, topFraction: Double = 0.34): Double {
        val boardOnly = board.toSet()
        val strongestFirst = villainRange.classes().sortedByDescending { handClass ->
            val combo = handClass.combos(boardOnly).firstOrNull()
            if (combo == null) -1 else HandEvaluator.score(listOf(combo.first, combo.second) + board)
        }
        val topClasses = strongestFirst.take((strongestFirst.size * topFraction).toInt().coerceAtLeast(1))
        val combosBefore = topClasses.sumOf { it.combos(boardOnly).size }
        if (combosBefore == 0) return 0.0
        val combosAfter = topClasses.sumOf { it.combos(boardOnly + heroHand.first + heroHand.second).size }
        return 1.0 - combosAfter.toDouble() / combosBefore
    }

    /** One short sentence surfacing either signal, or null if neither clearly applies here. */
    fun note(heroHand: Pair<Card, Card>, villainRange: Range, board: List<Card>): String? {
        val capped = isCapped(villainRange, board)
        val blocked = blockedFraction(heroHand, villainRange, board)
        return when {
            capped && blocked > 0.2 ->
                "This range reads as capped (no monster hands left after calling this far), and your hand blocks " +
                    "${(blocked * 100).toInt()}% of their strongest remaining combos -- a strong spot to bluff."
            capped ->
                "This range reads as capped -- villain called without ever raising on this wet board, so a real " +
                    "hand is less likely than it looks."
            blocked > 0.3 ->
                "Your hand blocks ${(blocked * 100).toInt()}% of villain's strongest remaining combos here, making " +
                    "a bluff more credible."
            else -> null
        }
    }
}
