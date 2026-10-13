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
                session.respondToThreeBet(ThreeBetResponse.FOLD)
                assertTrue(session.state is HandState.HandEndedPreflop)
                return
            }
        }
    }

    @Test
    fun `four-betting either wins preflop or proceeds to a flop`() {
        for (seed in 0 until 100) {
            val session = HandSession(Archetype.ROCK, random = kotlin.random.Random(seed.toLong()))
            session.openOrFold(open = true)
            if (session.state is HandState.FacingThreeBet) {
                session.respondToThreeBet(ThreeBetResponse.FOUR_BET)
                val result = session.state
                assertTrue(
                    result is HandState.HandEndedPreflop || result is HandState.RangeGuess,
                    "expected a terminal preflop win or a flop after four-betting, got $result",
                )
                return
            }
        }
    }

    @Test
    fun `every seat produces a playable preflop hand without crashing`() {
        for (seat in Seat.entries) {
            val session = HandSession(Archetype.SHARK, seat, random = kotlin.random.Random(seat.ordinal.toLong()))
            assertTrue(session.state is HandState.PreflopDecision)
            session.openOrFold(open = true)
        }
    }

    @Test
    fun `a full hand reaches river showdown or ends earlier without crashing, many seeds`() {
        for (seed in 0 until 80) {
            val session = HandSession(Archetype.entries[seed % 3], Seat.entries[seed % 4], random = kotlin.random.Random(seed.toLong()))
            session.openOrFold(open = true)

            when (session.state) {
                is HandState.HandEndedPreflop -> continue
                is HandState.FacingThreeBet -> session.respondToThreeBet(ThreeBetResponse.CALL)
                else -> {}
            }
            if (session.state is HandState.HandEndedPreflop) continue

            val guessState = session.state
            assertTrue(guessState is HandState.RangeGuess, "expected RangeGuess for seed $seed, got $guessState")
            session.submitRangeGuess(setOf(HandClass(14, 14, null)))
            assertTrue(session.state is HandState.RangeGuessResult)
            session.proceedToFlopDecision()

            var guardRails = 0
            while (session.state !is HandState.HandEnded && guardRails < 15) {
                when (val s = session.state) {
                    is HandState.StreetDecision -> session.chooseStreetAction(FlopAction.entries[(seed + guardRails) % 3])
                    is HandState.FacingBet -> session.respondToBet(FacingBetAction.entries[(seed + guardRails) % 3])
                    else -> error("unexpected state mid-hand: $s")
                }
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
