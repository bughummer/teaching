package trainer.engine.range

import kotlin.test.Test
import kotlin.test.assertTrue

class HandSessionTest {

    @Test
    fun `folding preflop ends the hand immediately`() {
        val session = HandSession(Archetype.ROCK, random = kotlin.random.Random(1))
        session.openOrFold(open = false)
        assertTrue(session.state is HandState.HandEndedPreflop)
    }

    @Test
    fun `full hand reaches a terminal state without crashing, run many seeds`() {
        for (seed in 0 until 50) {
            val session = HandSession(Archetype.entries[seed % 3], random = kotlin.random.Random(seed.toLong()))
            session.openOrFold(open = true)
            when (val afterOpen = session.state) {
                is HandState.HandEndedPreflop -> { /* villain folded or three-bet: done */ }
                is HandState.RangeGuess -> {
                    session.submitRangeGuess(setOf(HandClass(14, 14, null)))
                    assertTrue(session.state is HandState.RangeGuessResult)
                    session.proceedToFlopDecision()
                    assertTrue(session.state is HandState.FlopDecision)
                    session.chooseFlopAction(FlopAction.entries[seed % 3])
                    assertTrue(session.state is HandState.HandEnded, "expected terminal state for seed $seed")
                }
                else -> error("unexpected state after open: $afterOpen")
            }
        }
    }

    @Test
    fun `range guess scores precision and recall sanely`() {
        val session = HandSession(Archetype.STATION, random = kotlin.random.Random(7))
        session.openOrFold(open = true)
        val guessState = session.state
        if (guessState !is HandState.RangeGuess) return // villain folded/3bet this seed; not what we're testing
        session.submitRangeGuess(setOf(HandClass(14, 14, null), HandClass(2, 2, null)))
        val result = session.state as HandState.RangeGuessResult
        assertTrue(result.precision in 0.0..1.0)
        assertTrue(result.recall in 0.0..1.0)
    }
}
