package trainer.engine

object PokerMath {
    /** Minimum equity needed to profitably call a bet, given the pot before your call and the bet facing you. */
    fun requiredEquity(potBeforeCall: Int, bet: Int): Double {
        require(potBeforeCall >= 0 && bet > 0)
        return bet.toDouble() / (potBeforeCall + bet + bet)
    }

    /** Rough equity for a draw using the rule of 4 (two cards to come) or 2 (one card to come). */
    fun drawEquityEstimate(outs: Int, cardsToCome: Int): Double {
        val multiplier = if (cardsToCome >= 2) 4 else 2
        return (outs * multiplier).coerceAtMost(100).toDouble() / 100.0
    }

    /** Fraction of the time a bet needs to work for a pure bluff to break even. */
    fun breakEvenBluffFrequency(potBeforeBet: Int, bet: Int): Double {
        require(potBeforeBet >= 0 && bet > 0)
        return bet.toDouble() / (potBeforeBet + bet)
    }
}
