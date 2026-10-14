package trainer.engine.range

/** Short, state-specific reminders of what to weigh before acting -- "take this into account" notes. */
object CoachTips {

    fun forState(state: HandState, seat: Seat): String? = when (state) {
        is HandState.PreflopDecision ->
            "From ${seat.label} you should be opening about ${(seat.openFraction * 100).toInt()}% of hands. " +
                "If this hand isn't in that top slice, folding is correct even if it looks playable."

        is HandState.RangeGuess ->
            "Think about what keeps calling here, not just \"strong hands\": pairs that hit this board, " +
                "flush and straight draws, and overcards that could still improve all belong in a caller's range."

        is HandState.FacingBet -> {
            val callCost = state.betSize
            val potAfterCall = state.pot + callCost * 2
            val equityNeeded = (callCost.toDouble() / potAfterCall * 100).toInt()
            "Pot odds: you're calling $callCost into a pot that becomes $potAfterCall, so you need " +
                "roughly $equityNeeded% equity to break even on a call alone."
        }

        is HandState.StreetDecision ->
            "Before betting, check both numbers below: your hand's own equity, and whether your whole " +
                "range (not just this hand) is ahead of theirs on this board."

        else -> null
    }
}
