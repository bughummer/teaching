package trainer.engine.range

import trainer.engine.Card
import trainer.engine.Suit
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BluffReadoutTest {

    // 9s 8s 7c: two suited cards and a tight 3-rank span -- wetness 0.75, well above the 0.5 cutoff.
    private val wetBoard = listOf(Card(9, Suit.SPADES), Card(8, Suit.SPADES), Card(7, Suit.CLUBS))

    @Test
    fun `a range with no monster classes on a wet board reads as capped`() {
        // Villain's whole range is low cards that at most pick up a straight draw here -- no
        // overpair, pairs-the-board, or combo draw (straight draw alone doesn't count as a monster).
        val weakRange = Range(mapOf(HandClass(5, 4, false) to 1.0, HandClass(6, 3, false) to 1.0))
        assertTrue(BluffReadout.isCapped(weakRange, wetBoard))
    }

    @Test
    fun `a range that still contains overpairs on a wet board is not capped`() {
        val strongRange = Range(mapOf(HandClass(14, 14, null) to 1.0))
        assertFalse(BluffReadout.isCapped(strongRange, wetBoard))
    }

    @Test
    fun `a dry board is never read as capped regardless of range`() {
        val dryBoard = listOf(Card(2, Suit.CLUBS), Card(7, Suit.HEARTS), Card(11, Suit.DIAMONDS))
        val weakRange = Range(mapOf(HandClass(5, 4, false) to 1.0))
        assertFalse(BluffReadout.isCapped(weakRange, dryBoard))
    }

    @Test
    fun `holding one of villain's two cards removes that exact combo from the blocked fraction`() {
        val board = listOf(Card(2, Suit.HEARTS), Card(9, Suit.HEARTS), Card(4, Suit.CLUBS))
        val heroHand = Card(14, Suit.HEARTS) to Card(3, Suit.CLUBS)
        // AKs: 4 combos (one per suit), none overlapping the board. Hero's ace of hearts removes
        // exactly the AhKh combo, leaving 3 of 4 -- a 25% reduction.
        val akSuited = Range(mapOf(HandClass(14, 13, true) to 1.0))
        val blocked = BluffReadout.blockedFraction(heroHand, akSuited, board, topFraction = 1.0)
        assertTrue(blocked > 0.2, "expected hero's ace of hearts to block the AhKh combo, got $blocked")
    }

    @Test
    fun `an unrelated hand blocks none of villain's strongest combos`() {
        val board = listOf(Card(2, Suit.HEARTS), Card(9, Suit.DIAMONDS), Card(4, Suit.CLUBS))
        val heroHand = Card(5, Suit.SPADES) to Card(6, Suit.SPADES)
        val range = Range(mapOf(HandClass(14, 14, null) to 1.0))
        val blocked = BluffReadout.blockedFraction(heroHand, range, board, topFraction = 1.0)
        assertTrue(blocked < 0.1, "expected no blocking effect, got $blocked")
    }
}
