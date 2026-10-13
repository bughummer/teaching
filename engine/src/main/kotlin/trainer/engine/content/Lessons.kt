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
            id = "multiway",
            title = "Multi-Way Pots",
            summary = "Every extra player in the pot means your hand needs to be stronger, and bluffs need to fool " +
                "more people at once. The math and the psychology of heads-up poker don't scale up directly.",
            keyPoints = listOf(
                "A hand that's a clear bet heads-up can be a clear check with three or more players in the pot.",
                "Bluffing gets harder with more opponents: you need every single one of them to fold, not just one.",
                "Drawing hands go up in value in multi-way pots because more players means more money behind to win.",
                "Position matters even more multi-way — acting last against a whole field is a bigger edge than against one player.",
            ),
            terms = listOf(
                Term("Multi-way pot", "A pot with three or more players still active."),
                Term("Heads-up pot", "A pot with exactly two players remaining."),
                Term("Protection bet", "A bet made partly to reduce the number of opponents who can draw out on you."),
            ),
        ),
        Lesson(
            id = "overbetting",
            title = "Overbets and Mixed Strategy",
            summary = "Sometimes the standard-sized bet isn't big enough. A well-timed overbet can apply more " +
                "pressure than the pot allows for — but only if your range can credibly hold the nuts there.",
            keyPoints = listOf(
                "An overbet (more than the size of the pot) only works when your range contains hands strong enough to justify it.",
                "Boards where one player's range contains all the strongest possible hands are overbet-friendly.",
                "A mixed strategy means taking different actions with the exact same hand some percentage of the time, not always the same one.",
                "Mixing is what makes you unexploitable: an opponent who can predict your action with certainty can always respond correctly.",
            ),
            terms = listOf(
                Term("Overbet", "A bet larger than the current size of the pot."),
                Term("Nut advantage", "Having a disproportionate share of the best possible hands on a given board."),
                Term("Mixed strategy", "Taking different actions with the same hand type a certain percentage of the time."),
            ),
        ),
        Lesson(
            id = "tableimage",
            title = "Table Image and Metagame",
            summary = "Opponents build a model of you from everything they've seen you do, and you should be doing " +
                "the same to them. Playing against that model — not just the cards — is the metagame.",
            keyPoints = listOf(
                "Your recent history at the table changes how credible your bets look, independent of your actual hand.",
                "If you've been caught bluffing, your value bets get paid off more — lean into betting for value more often.",
                "If you've shown only strong hands down, your bets get more respect — this is the time to bluff more.",
                "Track how your opponents adjust to you over a session, not just how you should adjust to them.",
            ),
            terms = listOf(
                Term("Table image", "The impression of your playing style that opponents have formed from watching you."),
                Term("Metagame", "Strategic adjustments based on history and perception, beyond the current hand's cards."),
            ),
        ),
        Lesson(
            id = "tells",
            title = "Reading Physical and Betting Tells",
            summary = "Players leak information beyond their bets — timing, posture, and small habits can all hint at " +
                "hand strength. Betting patterns are the most reliable tells of all, especially online.",
            keyPoints = listOf(
                "A sudden change from a player's established pattern — in timing or bet size — is one of the most reliable tells.",
                "Relaxed, loose body language often follows a made hand; tension often follows a bluff — but weigh this lightly, it's not universal.",
                "Instant calls or raises often mean a decision was easy, which tells you something about where that hand sits in their range.",
                "Online, bet-sizing habits and timing are nearly all you have — and they're often more reliable than in-person tells anyway.",
            ),
            terms = listOf(
                Term("Tell", "Any behavior that reveals information about a player's hand beyond their bets."),
                Term("Default bet size", "The sizing a player uses automatically, out of habit rather than active thought."),
                Term("Snap decision", "A bet, call, or fold made with almost no hesitation."),
            ),
        ),
        Lesson(
            id = "tilt",
            title = "Managing and Exploiting Tilt",
            summary = "Tilt is a state of impaired decision-making triggered by frustration, usually after a bad beat " +
                "or a string of losses. Managing your own tilt — and spotting it in others — are both profit centers.",
            keyPoints = listOf(
                "The first step in managing tilt is recognizing the emotional state before it changes your decisions.",
                "A standing rule (like a stop-loss, or a mandatory break after a bad beat) removes the need to decide while tilted.",
                "Tilted opponents usually become more loose and aggressive, playing more hands and bluffing more than their baseline.",
                "Widen your continuing ranges against a player you've identified as on tilt — their range is wider than usual.",
            ),
            terms = listOf(
                Term("Tilt", "A state of frustration or emotional reaction that leads to worse decision-making."),
                Term("Stop-loss", "A predetermined limit on losses that ends a session regardless of how you feel in the moment."),
            ),
        ),
        Lesson(
            id = "tournament-early",
            title = "Tournament Strategy: Early and Middle Stages",
            summary = "Deep-stacked early tournament play rewards patience and position, closer to a cash game. As " +
                "stacks shrink relative to the blinds, every decision gets more urgent.",
            keyPoints = listOf(
                "Early stages have deep stacks relative to the blinds — play solid, positional poker and avoid unnecessary big confrontations.",
                "As the blinds climb, your stack-to-blind ratio shrinks, and stealing blinds becomes proportionally more valuable.",
                "Avoid marginal confrontations with other big stacks early — losing a big pot here can cripple your tournament.",
                "Middle stages call for picking up pots other players aren't fighting for, since antes make every pot bigger relative to the blinds.",
            ),
            terms = listOf(
                Term("Stack-to-blind ratio", "A player's chip stack divided by the size of the big blind, a measure of how much room they have to maneuver."),
                Term("Ante", "A small forced bet from every player, common in later tournament levels, that grows the pot independent of the blinds."),
            ),
        ),
        Lesson(
            id = "tournament-bubble",
            title = "Tournament Strategy: The Bubble",
            summary = "The bubble is the stretch right before the money, where min-cashing is one elimination away. " +
                "Stack sizes and survival math start to matter more than raw hand strength.",
            keyPoints = listOf(
                "Short stacks near the bubble often fold far tighter than usual to survive into the money — exploit that by widening your steals against them.",
                "Big stacks can apply maximum pressure on medium stacks on the bubble, since a medium stack has the most to lose by busting now.",
                "Being the short stack on the bubble means recognizing when you're being targeted and finding a spot to double up before being blinded out.",
                "ICM (tournament equity, not just chip count) means a chip isn't always worth the same to every stack size at this stage.",
            ),
            terms = listOf(
                Term("Bubble", "The point in a tournament immediately before the remaining players would be paid."),
                Term("ICM (Independent Chip Model)", "A way of converting chip stacks into real tournament equity, which isn't a straight-line function of chip count."),
            ),
        ),
        Lesson(
            id = "tournament-final",
            title = "Tournament Strategy: Late Stages and the Final Table",
            summary = "Final table play blends short-stack survival math with the reality that the payout jumps get " +
                "much bigger near the top — the decisions here carry more weight than almost anywhere else in poker.",
            keyPoints = listOf(
                "The payout jump between each remaining place grows sharply near the top, so avoiding unnecessary risk can be worth real money, not just chips.",
                "A short stack at the final table should look for a double-up spot rather than waiting to be blinded away to nothing.",
                "A big stack at the final table can bully medium stacks who are playing to survive into a bigger payout.",
                "Universal tournament advice (survive, don't gift chips away, pick your spots) matters most exactly when the stakes feel highest.",
            ),
            terms = listOf(
                Term("Pay jump", "The increase in guaranteed payout between consecutive finishing positions."),
                Term("Final table", "The last table of players remaining in a tournament."),
            ),
        ),
        Lesson(
            id = "cashgame",
            title = "Cash Game Fundamentals and Bankroll",
            summary = "Cash games have no finish line — you can reload or leave anytime stacks reset to a fixed buy-in. " +
                "That changes the math: variance management and game selection matter as much as any single hand.",
            keyPoints = listOf(
                "Always buy in for the maximum allowed — a bigger stack lets you apply more pressure and realize more value from strong hands.",
                "A standard bankroll guideline is to keep at least 20 buy-ins for the stakes you're playing, to survive a losing streak without going broke.",
                "Game selection — picking a soft table over a tough one — often matters more than small strategic refinements.",
                "Prepare mentally for variance: even a well-played session can lose money, and a badly played one can win. Judge decisions, not just results.",
            ),
            terms = listOf(
                Term("Buy-in", "The amount of money exchanged for chips to start or re-enter a cash game."),
                Term("Bankroll", "The total money set aside for playing poker, separate from personal finances."),
                Term("Variance", "The natural short-term swings in results that happen even with a correct, profitable strategy."),
                Term("Game selection", "Choosing which table or game to play based on the skill level of the other players."),
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
