package trainer.engine.content

/** All lesson text below is written fresh for this app; it does not quote or paraphrase any
 * specific course, book, or instructor. */
object Lessons {

    val all: List<Lesson> = listOf(
        Lesson(
            id = "position",
            title = "Seat Matters",
            summary = "Acting after your opponents gives you more information than acting before them. " +
                "That single fact should steer how wide you play from each seat.",
            keyPoints = listOf(
                "Early seats act first all postflop streets — play tighter there.",
                "Late seats (cutoff, button) act last — open a wider range.",
                "The blinds are forced in with weak hands and stay out of position for the rest of the hand — defend carefully.",
                "The same hand can be a clear raise on the button and a clear fold under the gun.",
            ),
            terms = listOf(
                Term("In position", "Acting after your opponent on every remaining street."),
                Term("Out of position", "Acting before your opponent on every remaining street."),
                Term("Open-raise", "The first raise preflop, before anyone else has entered the pot."),
            ),
        ),
        Lesson(
            id = "ranges",
            title = "Think in Ranges",
            summary = "Strong players stop guessing a single hand and instead picture the whole set of hands " +
                "an opponent could hold in a spot. Decisions get sharper once you compare range against range.",
            keyPoints = listOf(
                "A range is every hand your opponent could logically have reached this point with.",
                "Bigger bets usually mean a tighter, more polarized range: strong hands and bluffs, fewer in-between hands.",
                "Whoever's range connects better with a board has a range advantage on that board.",
                "Track tendencies over many hands rather than reading one action in isolation.",
            ),
            terms = listOf(
                Term("Polarized range", "A range made of strong value hands and weak bluffs, with little in between."),
                Term("Range advantage", "Having more strong hands than your opponent given the board that came out."),
            ),
        ),
        Lesson(
            id = "cbet",
            title = "Continuation Betting",
            summary = "A continuation bet keeps the pressure on after you were the preflop raiser. It works best " +
                "when the board favors your range and the opponent's range mostly missed.",
            keyPoints = listOf(
                "You can profitably bet many boards even without a strong hand if your range is ahead overall.",
                "Dry, uncoordinated boards favor the preflop raiser — bet smaller and more often.",
                "Wet, coordinated boards give calling ranges more equity — bet bigger but less often, or check back more.",
                "A continuation bet is a bet about ranges, not a promise your specific hand is good.",
            ),
            terms = listOf(
                Term("Continuation bet (c-bet)", "A bet made by the player who raised preflop, continuing the aggression after the flop."),
                Term("Dry board", "A flop with few straight or flush possibilities."),
                Term("Wet board", "A flop with many straight and flush possibilities."),
            ),
        ),
        Lesson(
            id = "checkraise",
            title = "Check-Raising",
            summary = "Checking then raising lets the out-of-position player seize initiative without betting into " +
                "an opponent who might not even bet. It should carry both strong hands and a few bluffs.",
            keyPoints = listOf(
                "A check-raise needs the opponent to bet first, so it only works against bettors.",
                "Mixing genuine strong hands with occasional bluffs keeps the check-raise unpredictable.",
                "Boards where your checking range contains hidden strength (sets, two pair) are the best spots to check-raise as a bluff too.",
                "Check-raising every strong hand and never bluffing makes the play easy to read and avoid.",
            ),
            terms = listOf(
                Term("Check-raise", "To check, then raise after an opponent bets into you."),
                Term("Checking range", "All the hands with which a player checks in a given spot."),
            ),
        ),
        Lesson(
            id = "threebet",
            title = "Three-Betting",
            summary = "Re-raising before the flop (a three-bet) builds a bigger pot with your best hands and can " +
                "also fold out opponents who opened light, as long as you mix in a few bluffs.",
            keyPoints = listOf(
                "A three-bet range typically has two clusters: premium value hands and speculative bluffs.",
                "Being three-bet doesn't automatically mean the aggressor has a monster — their range depends on their tendencies.",
                "Position still matters after a three-bet: being out of position to a three-bet calls for a tighter continuing range.",
                "Facing frequent three-bets from one player should tighten your own opening range against them specifically.",
            ),
            terms = listOf(
                Term("Three-bet", "A re-raise of an opening raise before the flop."),
                Term("Four-bet", "A re-raise of a three-bet."),
            ),
        ),
        Lesson(
            id = "betsizing",
            title = "Bet Sizing",
            summary = "The size of a bet changes the price your opponent is getting and the message your range is " +
                "sending. Bigger isn't always better — the right size depends on the board and your goal.",
            keyPoints = listOf(
                "Bigger bets charge draws more but also let fewer hands continue, including some you'd beat.",
                "Smaller bets keep more of the opponent's range in but risk less when you're bluffing or thin-value betting.",
                "Sizing should be consistent across your whole range in a spot so it doesn't give away hand strength by itself.",
                "Unusually large or small bets from an otherwise consistent player are a strong tell worth noting.",
            ),
            terms = listOf(
                Term("Overbet", "A bet larger than the current size of the pot."),
                Term("Value bet", "A bet made expecting to be called by a worse hand more often than a better one."),
            ),
        ),
        Lesson(
            id = "potodds",
            title = "Pot Odds & Bluff Math",
            summary = "Two formulas cover most of the on-the-fly math in poker: how much equity you need to call, " +
                "and how often a bluff needs to work to break even.",
            keyPoints = listOf(
                "Required equity to call = bet / (pot + bet + bet).",
                "A quick draw-odds shortcut: outs × 4 for two cards to come, outs × 2 for one card to come.",
                "Break-even bluff frequency = bet / (pot + bet) — how often the bluff must get a fold.",
                "Compare the math to the read: the numbers set a baseline, reads adjust it.",
            ),
            terms = listOf(
                Term("Pot odds", "The ratio between the size of the pot and the size of the bet you're facing."),
                Term("Outs", "Cards that would improve your hand to a likely winner."),
            ),
        ),
        Lesson(
            id = "bluffing",
            title = "Balanced Bluffing",
            summary = "A bluff works by representing a range your opponent believes often enough. Bluffing with a " +
                "plan — a believable story and a sensible frequency — beats bluffing on impulse.",
            keyPoints = listOf(
                "The best bluffs block the hands your opponent needs to call with, or unblock the hands they'd fold.",
                "A believable bluff follows the same betting line a real value hand would take.",
                "If you only ever bet when you're strong, observant opponents will stop paying you off.",
                "Mix your actions with some hands rather than always taking the same line with every hand type — this is what keeps you unpredictable.",
            ),
            terms = listOf(
                Term("Blocker", "A card in your hand that reduces the number of strong hands your opponent can hold."),
                Term("Mixed strategy", "Taking different actions with the same hand type a certain percentage of the time, instead of always the same action."),
            ),
        ),
    )

    fun byId(id: String): Lesson = all.first { it.id == id }
}
