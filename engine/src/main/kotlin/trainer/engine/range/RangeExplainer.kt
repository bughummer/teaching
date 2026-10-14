package trainer.engine.range

import trainer.engine.Card

/** A coarse bucket for why a starting-hand class plays the way it does on a given board. */
private enum class BoardRelation { OVERPAIR, PAIRS_BOARD, TWO_HIGH_CARDS, LOW_UNCONNECTED }

object RangeExplainer {

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
            handClass.highRank >= 11 && handClass.lowRank >= 11 -> BoardRelation.TWO_HIGH_CARDS
            else -> BoardRelation.LOW_UNCONNECTED
        }
    }

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
