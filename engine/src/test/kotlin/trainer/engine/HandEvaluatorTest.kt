package trainer.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HandEvaluatorTest {

    private fun card(s: String): Card {
        val rank = when (s.dropLast(1)) {
            "A" -> 14; "K" -> 13; "Q" -> 12; "J" -> 11; "T" -> 10
            else -> s.dropLast(1).toInt()
        }
        val suit = when (s.last()) {
            'c' -> Suit.CLUBS; 'd' -> Suit.DIAMONDS; 'h' -> Suit.HEARTS; 's' -> Suit.SPADES
            else -> error("bad suit")
        }
        return Card(rank, suit)
    }

    private fun hand(vararg s: String) = s.map { card(it) }

    @Test
    fun `category ordering is respected`() {
        val straightFlush = HandEvaluator.score(hand("9h", "8h", "7h", "6h", "5h"))
        val quads = HandEvaluator.score(hand("9h", "9d", "9c", "9s", "2h"))
        val fullHouse = HandEvaluator.score(hand("9h", "9d", "9c", "2s", "2h"))
        val flush = HandEvaluator.score(hand("9h", "7h", "5h", "3h", "2h"))
        val straight = HandEvaluator.score(hand("9h", "8d", "7c", "6s", "5h"))
        val trips = HandEvaluator.score(hand("9h", "9d", "9c", "5s", "2h"))
        val twoPair = HandEvaluator.score(hand("9h", "9d", "5c", "5s", "2h"))
        val pair = HandEvaluator.score(hand("9h", "9d", "5c", "4s", "2h"))
        val highCard = HandEvaluator.score(hand("9h", "7d", "5c", "4s", "2h"))

        val ordered = listOf(highCard, pair, twoPair, trips, straight, flush, fullHouse, quads, straightFlush)
        for (i in 0 until ordered.size - 1) {
            assertTrue(ordered[i] < ordered[i + 1], "expected index $i to score lower than ${i + 1}")
        }
    }

    @Test
    fun `wheel straight counts ace low`() {
        val wheel = HandEvaluator.score(hand("Ah", "2d", "3c", "4s", "5h"))
        assertEquals("Straight", HandEvaluator.categoryName(wheel))
    }

    @Test
    fun `best five of seven is selected`() {
        // Board gives a straight; hero's pair shouldn't be picked over it.
        val cards = hand("2h", "2d", "9c", "Tc", "Jc", "Qc", "Kc")
        assertEquals("Straight flush", HandEvaluator.categoryName(HandEvaluator.score(cards)))
    }

    @Test
    fun `higher pair beats lower pair`() {
        val acePair = HandEvaluator.score(hand("Ah", "Ad", "5c", "4s", "2h"))
        val kingPair = HandEvaluator.score(hand("Kh", "Kd", "5c", "4s", "2h"))
        assertTrue(acePair > kingPair)
    }
}
