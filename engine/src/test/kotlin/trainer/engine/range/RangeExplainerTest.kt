package trainer.engine.range

import trainer.engine.Card
import trainer.engine.Suit
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RangeExplainerTest {

    private val board = listOf(Card(3, Suit.SPADES), Card(13, Suit.SPADES), Card(6, Suit.DIAMONDS)) // 3s Ks 6d

    @Test
    fun `an unpaired hand matching two board ranks is read as pairing the board, not as disconnected`() {
        // K6o makes two pair on a K-x-6 board -- it must not be called "low and disconnected".
        val k6o = HandClass(13, 6, false)
        val reason = RangeExplainer.reason(k6o, board, Archetype.STATION, inTrueRange = true)
        assertFalse(reason.contains("disconnected"), "K6o pairs two board cards here; got: $reason")
        assertTrue(reason.contains("pairs") || reason.contains("connects"), "expected a pairing-related reason, got: $reason")
    }

    @Test
    fun `an unpaired hand matching only one board rank is also read as pairing the board`() {
        val k9o = HandClass(13, 9, false) // king matches the board's king; 9 doesn't matter
        val reason = RangeExplainer.reason(k9o, board, Archetype.STATION, inTrueRange = true)
        assertFalse(reason.contains("disconnected"), "K9o pairs the board's king; got: $reason")
    }

    @Test
    fun `a pocket pair matching a board rank is still read as making trips`() {
        val sixes = HandClass(6, 6, null)
        val reason = RangeExplainer.reason(sixes, board, Archetype.STATION, inTrueRange = true)
        assertFalse(reason.contains("disconnected"), "66 makes trips on this board; got: $reason")
    }

    @Test
    fun `a pocket pair above the board's top card is still read as an overpair`() {
        val aces = HandClass(14, 14, null)
        val reason = RangeExplainer.reason(aces, board, Archetype.STATION, inTrueRange = true)
        assertTrue(reason.contains("overpair") || reason.contains("too strong"), "expected overpair wording, got: $reason")
    }

    @Test
    fun `two unrelated broadway cards are read as two high cards, not disconnected`() {
        val aq = HandClass(14, 12, true) // neither ace nor queen is on this board
        val reason = RangeExplainer.reason(aq, board, Archetype.STATION, inTrueRange = true)
        assertFalse(reason.contains("disconnected"), "AQ is two live overcards, not disconnected; got: $reason")
    }

    @Test
    fun `an open-ended straight draw is not read as disconnected`() {
        val drawBoard = listOf(Card(7, Suit.CLUBS), Card(6, Suit.HEARTS), Card(2, Suit.SPADES))
        val nineEightSuited = HandClass(9, 8, true) // 98 makes an open-ended straight draw on 7-6-2
        val reason = RangeExplainer.reason(nineEightSuited, drawBoard, Archetype.STATION, inTrueRange = true)
        assertFalse(reason.contains("disconnected"), "98 has an open-ended straight draw on 7-6-2; got: $reason")
        assertTrue(reason.contains("draw"), "expected draw-related wording, got: $reason")
    }

    @Test
    fun `a gutshot straight draw is not read as disconnected`() {
        val drawBoard = listOf(Card(9, Suit.CLUBS), Card(6, Suit.HEARTS), Card(2, Suit.SPADES))
        val eightSeven = HandClass(8, 7, false) // 87 + 9-6 board needs a 5 (gutshot via 9-8-7-6-5, also open via 5 or... check)
        val reason = RangeExplainer.reason(eightSeven, drawBoard, Archetype.STATION, inTrueRange = true)
        assertFalse(reason.contains("disconnected"), "87 has a straight draw with a 9 on board; got: $reason")
    }

    @Test
    fun `a suited hand sharing two board suits is read as a flush draw`() {
        val flushBoard = listOf(Card(2, Suit.HEARTS), Card(9, Suit.HEARTS), Card(4, Suit.CLUBS))
        val kqHearts = HandClass(13, 12, true) // suited class; representative combo shares hearts with the board
        val reason = RangeExplainer.reason(kqHearts, flushBoard, Archetype.STATION, inTrueRange = true)
        assertFalse(reason.contains("disconnected"), "a suited class here has a flush draw; got: $reason")
        assertTrue(reason.contains("flush"), "expected flush-draw wording, got: $reason")
    }

    @Test
    fun `flush draw and straight draw together is read as a monster combo draw`() {
        // 9s 8s on a 7s 6s 2c board: flush draw (2 spades on board) and open-ended straight draw.
        val drawBoard = listOf(Card(7, Suit.SPADES), Card(6, Suit.SPADES), Card(2, Suit.CLUBS))
        val nineEightSpades = HandClass(9, 8, true)
        val reason = RangeExplainer.reason(nineEightSpades, drawBoard, Archetype.STATION, inTrueRange = true)
        assertTrue(reason.contains("monster") || reason.contains("combo draw"), "expected combo-draw wording, got: $reason")
    }

    @Test
    fun `a genuinely low, unconnected hand is still read as disconnected`() {
        val ninetwo = HandClass(9, 2, false) // doesn't match 3, K, or 6, and isn't two broadway cards
        val reason = RangeExplainer.reason(ninetwo, board, Archetype.STATION, inTrueRange = false)
        assertTrue(reason.contains("disconnected"), "expected 92o to be read as disconnected, got: $reason")
    }
}
