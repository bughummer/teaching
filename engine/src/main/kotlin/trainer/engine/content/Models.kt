package trainer.engine.content

data class Term(val name: String, val definition: String)

data class Lesson(
    val id: String,
    val title: String,
    val summary: String,
    val keyPoints: List<String>,
    val terms: List<Term>,
)

data class ActionOption(val id: String, val label: String)

/**
 * A single-decision drill: show a situation, offer a few actions, score the pick.
 * Deliberately one decision point per scenario so a session is a quick burst of reps,
 * not a full hand played street by street.
 */
data class Scenario(
    val id: String,
    val lessonId: String,
    val prompt: String,
    val heroCards: String,
    val board: String?,
    val potBeforeAction: Int,
    val betFacing: Int?,
    val options: List<ActionOption>,
    val correctOptionId: String,
    val explanation: String,
)
