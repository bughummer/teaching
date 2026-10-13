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

    @Test
    fun `two-opponent hands guess each active opponent's range in turn before the flop decision`() {
        for (seed in 0 until 60) {
            val session = HandSession(listOf(Archetype.STATION, Archetype.ROCK), random = kotlin.random.Random(seed.toLong()))
            session.openOrFold(open = true)
            val afterOpen = session.state
            if (afterOpen is HandState.HandEndedPreflop) continue // both folded

            var guessesSeen = 0
            var state = session.state
            while (state is HandState.RangeGuess) {
                guessesSeen++
                session.submitRangeGuess(setOf(HandClass(14, 14, null)))
                assertTrue(session.state is HandState.RangeGuessResult)
                session.proceedToFlopDecision()
                state = session.state
            }
            assertTrue(guessesSeen in 1..2, "expected 1 or 2 range guesses, got $guessesSeen for seed $seed")
            assertTrue(state is HandState.StreetDecision, "expected StreetDecision after range guesses, got $state")
            assertTrue(
                state.equityByOpponent.size == guessesSeen,
                "expected one equity entry per active opponent ($guessesSeen), got ${state.equityByOpponent.size}",
            )
            return
        }
    }

    @Test
    fun `two-opponent hands never reach a three-bet or a villain lead`() {
        for (seed in 0 until 60) {
            val session = HandSession(listOf(Archetype.SHARK, Archetype.SHARK), random = kotlin.random.Random(seed.toLong()))
            session.openOrFold(open = true)
            assertTrue(session.state !is HandState.FacingThreeBet, "multi-way should never face a three-bet")

            var guardRails = 0
            while (session.state !is HandState.HandEnded && guardRails < 20) {
                when (val s = session.state) {
                    is HandState.RangeGuess -> {
                        session.submitRangeGuess(emptySet())
                        session.proceedToFlopDecision()
                    }
                    is HandState.RangeGuessResult -> session.proceedToFlopDecision()
                    is HandState.StreetDecision -> session.chooseStreetAction(FlopAction.entries[guardRails % 3])
                    is HandState.FacingBet -> error("multi-way should never face a villain-led bet, got $s")
                    is HandState.HandEndedPreflop -> break
                    else -> error("unexpected state: $s")
                }
                guardRails++
            }
        }
    }

    @Test
    fun `two-opponent showdown samples distinct cards for each villain`() {
        for (seed in 0 until 100) {
            val session = HandSession(listOf(Archetype.STATION, Archetype.STATION), random = kotlin.random.Random(seed.toLong()))
            session.openOrFold(open = true)
            if (session.state is HandState.HandEndedPreflop) continue

            while (session.state is HandState.RangeGuess) {
                session.submitRangeGuess(emptySet())
                session.proceedToFlopDecision()
            }
            while (session.state !is HandState.HandEnded) {
                session.chooseStreetAction(FlopAction.CHECK)
            }
            // reaching here without HandEvaluator/Deck throwing on a card collision is the assertion
            assertTrue(session.state is HandState.HandEnded)
            return
        }
    }
}
