package trainer.engine

enum class Suit(val symbol: String) { CLUBS("♣"), DIAMONDS("♦"), HEARTS("♥"), SPADES("♠") }

/** rank 2..14, where 14 = Ace */
data class Card(val rank: Int, val suit: Suit) {
    init {
        require(rank in 2..14) { "rank must be 2..14, got $rank" }
    }

    val label: String
        get() = when (rank) {
            14 -> "A"; 13 -> "K"; 12 -> "Q"; 11 -> "J"; 10 -> "T"
            else -> rank.toString()
        }

    override fun toString() = "$label${suit.symbol}"
}

fun fullDeck(): List<Card> =
    Suit.entries.flatMap { suit -> (2..14).map { rank -> Card(rank, suit) } }

class Deck(seed: Long? = null) {
    private val rng = if (seed != null) kotlin.random.Random(seed) else kotlin.random.Random.Default
    private val cards = fullDeck().shuffled(rng).toMutableList()

    fun draw(count: Int): List<Card> {
        require(count <= cards.size) { "not enough cards left" }
        val drawn = cards.take(count)
        repeat(count) { cards.removeAt(0) }
        return drawn
    }
}
