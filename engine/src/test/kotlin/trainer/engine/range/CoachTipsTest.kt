package trainer.engine.range

import trainer.engine.Card
import trainer.engine.Suit
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoachTipsTest {

    @Test
    fun `preflop tip names the seat's opening percentage`() {
        val state = HandState.PreflopDecision(Card(14, Suit.SPADES) to Card(13, Suit.SPADES), Seat.CUTOFF)
        val tip = CoachTips.forState(state, Seat.CUTOFF)
        assertTrue(tip != null && tip.contains("30%"), "expected cutoff's 30% open to appear, got: $tip")
    }

    @Test
    fun `facing bet tip computes pot odds correctly`() {
        val state = HandState.FacingBet(
            Street.FLOP, Card(14, Suit.SPADES) to Card(13, Suit.SPADES), emptyList(), pot = 100, betSize = 50, heroEquity = 0.4,
        )
        // calling 50 into a pot that becomes 100 + 50*2 = 200 needs 50/200 = 25% equity.
        val tip = CoachTips.forState(state, Seat.BUTTON)
        assertTrue(tip != null && tip.contains("25%"), "expected 25% equity needed, got: $tip")
    }

    @Test
    fun `hand-ended states have no tip`() {
        val state = HandState.HandEnded("done", Card(14, Suit.SPADES) to Card(13, Suit.SPADES), emptyList())
        assertNull(CoachTips.forState(state, Seat.BUTTON))
    }
}
