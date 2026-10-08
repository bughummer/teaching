package trainer.engine.content

import kotlin.test.Test
import kotlin.test.assertTrue

class ContentTest {
    @Test
    fun `every lesson has at least one scenario`() {
        for (lesson in Lessons.all) {
            val scenarios = Scenarios.forLesson(lesson.id)
            assertTrue(scenarios.isNotEmpty(), "lesson ${lesson.id} has no scenarios")
        }
    }

    @Test
    fun `every scenario references a real lesson and a valid correct option`() {
        val lessonIds = Lessons.all.map { it.id }.toSet()
        for (scenario in Scenarios.all) {
            assertTrue(scenario.lessonId in lessonIds, "unknown lesson id on ${scenario.id}")
            assertTrue(
                scenario.options.any { it.id == scenario.correctOptionId },
                "correctOptionId not found among options on ${scenario.id}",
            )
            assertTrue(scenario.options.size >= 2, "need at least two options on ${scenario.id}")
        }
    }

    @Test
    fun `scenario ids are unique`() {
        val ids = Scenarios.all.map { it.id }
        assertTrue(ids.size == ids.toSet().size, "duplicate scenario ids found")
    }
}
