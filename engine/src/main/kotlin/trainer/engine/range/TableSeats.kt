package trainer.engine.range

/** The 6 canonical table positions in preflop action order, for display and layout purposes only. */
object TableSeats {
    val SIX_MAX_ORDER = listOf("UTG", "MP", "CO", "BTN", "SB", "BB")

    private fun heroIndex(seat: Seat): Int = when (seat) {
        Seat.UNDER_THE_GUN -> 0
        Seat.MIDDLE_POSITION -> 1
        Seat.CUTOFF -> 2
        Seat.BUTTON -> 3
    }

    /** Up to 5 seat names for opponents, in table order starting right after hero's own seat. */
    fun opponentSeatLabels(heroSeat: Seat, count: Int): List<String> {
        require(count in 0..5)
        val idx = heroIndex(heroSeat)
        val rotated = SIX_MAX_ORDER.drop(idx + 1) + SIX_MAX_ORDER.take(idx)
        return rotated.take(count)
    }
}
