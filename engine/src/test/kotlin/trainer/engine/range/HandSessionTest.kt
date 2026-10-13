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
    fun `folding to a three-bet ends the hand`() {
        for (seed in 0 until 100) {
            val session = HandSession(Archetype.SHARK, random = kotlin.random.Random(seed.toLong()))
            session.openOrFold(open = true)
            val afterOpen = session.state
            if (afterOpen is HandState.FacingThreeBet) {
                session.respondToThreeBet(call = false)
                assertTrue(session.state is HandState.HandEndedPreflop)
                return
            }
        }
    }

    @Test
    fun `a full hand reaches river showdown or ends earlier without crashing, many seeds`() {
        for (seed in 0 until 60) {
            val session = HandSession(Archetype.entries[seed % 3], random = kotlin.random.Random(seed.toLong()))
            session.openOrFold(open = true)

            when (session.state) {
                is HandState.HandEndedPreflop -> continue
                is HandState.FacingThreeBet -> session.respondToThreeBet(call = true)
                else -> {}
            }

            if (session.state is HandState.HandEndedPreflop) continue

            val guessState = session.state
            assertTrue(guessState is HandState.RangeGuess, "expected RangeGuess for seed $seed, got $guessState")
            session.submitRangeGuess(setOf(HandClass(14, 14, null)))
            assertTrue(session.state is HandState.RangeGuessResult)
            session.proceedToFlopDecision()

            var guardRails = 0
            while (session.state is HandState.StreetDecision && guardRails < 10) {
                session.chooseStreetAction(FlopAction.entries[(seed + guardRails) % 3])
                guardRails++
            }
            assertTrue(session.state is HandState.HandEnded, "expected terminal state for seed $seed, got ${session.state}")
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
        assertTrue(result.summaryLines.isNotEmpty())
    }
}
