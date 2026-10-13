package trainer.engine.range

import kotlin.test.Test
import kotlin.test.assertEquals

class TableSeatsTest {
    @Test
    fun `opponent seats after the button wrap around to SB and BB first`() {
        assertEquals(listOf("SB", "BB"), TableSeats.opponentSeatLabels(Seat.BUTTON, 2))
    }

    @Test
    fun `opponent seats after under the gun go MP, CO, BTN, SB, BB`() {
        assertEquals(
            listOf("MP", "CO", "BTN", "SB", "BB"),
            TableSeats.opponentSeatLabels(Seat.UNDER_THE_GUN, 5),
        )
    }

    @Test
    fun `requesting zero opponents gives an empty list`() {
        assertEquals(emptyList(), TableSeats.opponentSeatLabels(Seat.CUTOFF, 0))
    }
}
