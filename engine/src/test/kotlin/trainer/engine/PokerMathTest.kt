package trainer.engine

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class PokerMathTest {
    private fun closeTo(expected: Double, actual: Double) =
        assertTrue(abs(expected - actual) < 0.001, "expected $expected, got $actual")

    @Test
    fun `required equity matches the flush draw example in content`() {
        closeTo(0.25, PokerMath.requiredEquity(potBeforeCall = 60, bet = 30))
    }

    @Test
    fun `break even bluff frequency matches the content example`() {
        closeTo(1.0 / 3.0, PokerMath.breakEvenBluffFrequency(potBeforeBet = 50, bet = 25))
    }
}
