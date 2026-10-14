package trainer.engine.range

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RangeMathTest {

    @Test
    fun `all 169 classes sum to 1326 combos`() {
        val total = HandClass.all().sumOf { it.comboCount }
        assertEquals(1326, total)
    }

    @Test
    fun `combos returns the right count per class`() {
        val pair = HandClass(14, 14, null)
        assertEquals(6, pair.combos().size)
        val suited = HandClass(14, 13, true)
        assertEquals(4, suited.combos().size)
        val offsuit = HandClass(14, 13, false)
        assertEquals(12, offsuit.combos().size)
    }

    @Test
    fun `topFraction produces roughly the requested combo mass`() {
        val range = DefaultRanges.topFraction(0.2)
        val combos = range.totalWeightedCombos()
        assertTrue(combos in 250.0..270.0, "expected ~265 combos (20% of 1326), got $combos")
    }

    @Test
    fun `topFraction of 1 includes every class at full weight`() {
        val range = DefaultRanges.topFraction(1.0)
        assertEquals(169, range.classes().size)
        assertTrue(range.totalWeightedCombos() >= 1320.0)
    }

    @Test
    fun `stronger hands rank ahead of weaker ones`() {
        val order = DefaultRanges.strengthOrder
        val aa = order.indexOf(HandClass(14, 14, null))
        val akSuited = order.indexOf(HandClass(14, 13, true))
        val sevenDeuceOff = order.indexOf(HandClass(7, 2, false))
        assertTrue(aa < akSuited, "AA should rank above AKs")
        assertTrue(akSuited < sevenDeuceOff, "AKs should rank above 72o")
    }

    @Test
    fun `range sample respects excluded dead cards`() {
        val range = DefaultRanges.topFraction(0.5)
        val dead = setOf(trainer.engine.Card(14, trainer.engine.Suit.SPADES))
        repeat(50) {
            val combo = range.sample(dead) ?: error("expected a sample")
            assertTrue(combo.first != dead.first() && combo.second != dead.first())
        }
    }

    @Test
    fun `pocket aces crush a weak range preflop`() {
        val heroHand = trainer.engine.Card(14, trainer.engine.Suit.SPADES) to trainer.engine.Card(14, trainer.engine.Suit.HEARTS)
        val weakRange = Range(mapOf(HandClass(7, 2, false) to 1.0, HandClass(9, 4, false) to 1.0))
        val result = Equity.vsRange(heroHand, board = emptyList(), villainRange = weakRange, trials = 300, random = kotlin.random.Random(42))
        assertTrue(result.equity > 0.8, "AA should dominate 72o/94o preflop, got ${result.equity}")
    }

    @Test
    fun `big blind call range excludes the three-bet slice`() {
        val callRange = DefaultRanges.bigBlindCallRange()
        val threeBet = DefaultRanges.bigBlindThreeBetSlice()
        val overlap = callRange.classes().intersect(threeBet.classes())
        assertTrue(overlap.isEmpty(), "call range and three-bet range should not overlap")
    }

    @Test
    fun `three-bet slice is wider against a wider opening range`() {
        val vsButton = DefaultRanges.bigBlindThreeBetSlice(Seat.BUTTON).totalWeightedCombos()
        val vsUtg = DefaultRanges.bigBlindThreeBetSlice(Seat.UNDER_THE_GUN).totalWeightedCombos()
        assertTrue(vsButton > vsUtg, "3-bet range vs a button open ($vsButton) should be wider than vs UTG ($vsUtg)")
    }
}
