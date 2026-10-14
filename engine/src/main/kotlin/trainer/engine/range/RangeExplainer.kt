package trainer.engine.range

import trainer.engine.Card

/** A coarse bucket for why a starting-hand class plays the way it does on a given board. */
private enum class BoardRelation { OVERPAIR, PAIRS_BOARD, COMBO_DRAW, FLUSH_DRAW, STRAIGHT_DRAW, TWO_HIGH_CARDS, LOW_UNCONNECTED }

object RangeExplainer {

    /**
     * Suited-class only: could some real combo of this class share a suit with 2+ board cards (a
     * live flush draw)? Checked across every suit the class could be dealt in, not just one --
     * "KQs" means any of 4 suits, and only one of them need line up with the board.
     */
    private fun hasFlushDraw(handClass: HandClass, board: List<Card>): Boolean {
        if (handClass.suited != true) return false
        return handClass.combos(board.toSet()).any { combo -> board.count { it.suit == combo.first.suit } >= 2 }
    }

    /**
     * Does a hand rank, combined with the board, sit inside a 5-rank window that already has 4 of
     * its 5 ranks present (an open-ended or gutshot straight draw)? Ace counts both high and low.
     */
    private fun hasStraightDraw(handClass: HandClass, board: List<Card>): Boolean {
        val allRanks = (board.map { it.rank } + handClass.highRank + handClass.lowRank).toSet()
        val withLowAce = if (14 in allRanks) allRanks + 1 else allRanks
        for (low in 1..10) {
            val window = low..(low + 4)
            val present = window.count { it in withLowAce }
            val handInvolved = handClass.highRank in window || handClass.lowRank in window
            if (present >= 4 && handInvolved) return true
        }
        return false
    }

    private fun relationTo(handClass: HandClass, board: List<Card>): BoardRelation {
        val boardRanks = board.map { it.rank }
        val boardHigh = boardRanks.max()
        val isPocketPair = handClass.highRank == handClass.lowRank
        return when {
            isPocketPair && handClass.highRank > boardHigh -> BoardRelation.OVERPAIR
            // Matches a board rank: a pocket pair makes trips, an unpaired hand makes (at least) one pair --
            // this must be checked for both hole cards, not just pocket pairs, or a hand like K6 on a K-x-6
            // board (two pair) gets misread as having missed the board entirely.
            handClass.highRank in boardRanks || handClass.lowRank in boardRanks -> BoardRelation.PAIRS_BOARD
            // A flush draw and a straight draw together is a "monster draw" -- strong enough to bet
            // for value, not just as a bluff, unlike either draw alone.
            hasFlushDraw(handClass, board) && hasStraightDraw(handClass, board) -> BoardRelation.COMBO_DRAW
            hasFlushDraw(handClass, board) -> BoardRelation.FLUSH_DRAW
            hasStraightDraw(handClass, board) -> BoardRelation.STRAIGHT_DRAW
            handClass.highRank >= 11 && handClass.lowRank >= 11 -> BoardRelation.TWO_HIGH_CARDS
            else -> BoardRelation.LOW_UNCONNECTED
        }
    }

    /** Would this class usually be a raise, not just a call, on this board? Used to detect a capped range. */
    fun isMonsterClass(handClass: HandClass, board: List<Card>): Boolean =
        relationTo(handClass, board) in setOf(BoardRelation.OVERPAIR, BoardRelation.PAIRS_BOARD, BoardRelation.COMBO_DRAW)

    /** One short, board-specific sentence explaining why [handClass] does or doesn't belong in range here. */
    fun reason(handClass: HandClass, board: List<Card>, archetype: Archetype, inTrueRange: Boolean): String {
        val relation = relationTo(handClass, board)
        return when (relation) {
            BoardRelation.OVERPAIR ->
                if (inTrueRange) "${handClass.label} is a pocket pair above every board card — too strong to let go."
                else "${handClass.label} would be an overpair here — ${archetype.label} doesn't fold hands this strong."
            BoardRelation.PAIRS_BOARD ->
                if (inTrueRange) "${handClass.label} pairs a board card (trips if it's a pocket pair, a pair or two pair otherwise) for a strong, well-disguised hand."
                else "${handClass.label} connects directly with this board — matching a board card for at least a pair is too strong to be excluded."
            BoardRelation.COMBO_DRAW ->
                if (inTrueRange) "${handClass.label} has a flush draw and a straight draw at once — a monster draw ${archetype.label} happily continues (and often raises) with."
                else "${handClass.label} is a combo draw here (flush and straight draw together) — one of the strongest hands in range, not a fold."
            BoardRelation.FLUSH_DRAW ->
                if (inTrueRange) "${handClass.label} has a live flush draw here — enough equity for ${archetype.label} to continue."
                else "${handClass.label} has a flush draw on this board — that's real equity, not a fold."
            BoardRelation.STRAIGHT_DRAW ->
                if (inTrueRange) "${handClass.label} has a straight draw on this board — worth continuing with outs like this."
                else "${handClass.label} has a straight draw here (open-ended or a gutshot) — too many outs to fold."
            BoardRelation.TWO_HIGH_CARDS ->
                if (inTrueRange) "${handClass.label} is two big cards — overcards or a draw are enough for ${archetype.label} to continue."
                else "${handClass.label} missed this board entirely and has no pair — ${archetype.label} lets this go."
            BoardRelation.LOW_UNCONNECTED ->
                if (inTrueRange) "${handClass.label} connects with this board more than it looks — still worth a continue."
                else "${handClass.label} is low and disconnected from this board — a classic fold for ${archetype.label}."
        }
    }

    /** A few headline sentences summarizing the pattern across a whole guess, not every single cell. */
    fun summary(
        missed: Set<HandClass>,
        falseAlarms: Set<HandClass>,
        board: List<Card>,
        archetype: Archetype,
    ): List<String> {
        val lines = mutableListOf<String>()
        val missedOverpairs = missed.count { relationTo(it, board) == BoardRelation.OVERPAIR }
        if (missedOverpairs > 0) {
            lines += "You under-counted overpairs: $missedOverpairs class(es) above the board's top card are hands " +
                "${archetype.label} essentially never folds."
        }
        val missedDraws = missed.count {
            val r = relationTo(it, board)
            r == BoardRelation.FLUSH_DRAW || r == BoardRelation.STRAIGHT_DRAW || r == BoardRelation.COMBO_DRAW
        }
        if (missedDraws > 0) {
            lines += "You missed $missedDraws draw(s): a flush or straight draw has enough equity that " +
                "${archetype.label} keeps continuing with it, not just made hands."
        }
        val wronglyKeptAir = falseAlarms.count { relationTo(it, board) == BoardRelation.LOW_UNCONNECTED }
        if (wronglyKeptAir > 0) {
            lines += "You kept $wronglyKeptAir low, disconnected class(es) that ${archetype.label} folds to this bet " +
                "— low cards with no pair rarely continue."
        }
        if (lines.isEmpty()) {
            lines += "Good read overall — your guess tracked the board texture closely."
        }
        return lines
    }
}
