package app.levitx.core.skills

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SkillPathTest {
    private val path = SkillCatalogue.muscleUp

    @Test
    fun startsAtTheFirstGateWithNoResults() {
        val expected = PathProgress(currentIndex = 0, gatesRemaining = path.gates.size, currentFraction = 0.0)
        assertEquals(expected, path.progressFor(emptyMap()))
    }

    @Test
    fun reportsProgressTowardTheCurrentGate() {
        val progress = path.progressFor(
            mapOf("muscle_up.dead_hang" to 30, "muscle_up.dips" to 10, "muscle_up.pull_ups" to 8),
        )
        assertEquals(2, progress.currentIndex)
        assertEquals(4, progress.gatesRemaining)
        assertEquals(0.8, progress.currentFraction, absoluteTolerance = 0.001)
    }

    @Test
    fun laterGateDoesNotCountWhileAnEarlierOneIsOpen() {
        val progress = path.progressFor(mapOf("muscle_up.dead_hang" to 10, "muscle_up.pull_ups" to 15))
        assertEquals(0, progress.currentIndex)
    }

    @Test
    fun isCompleteWhenEveryGateIsPassed() {
        val progress = path.progressFor(path.gates.associate { it.id to it.target })
        assertTrue(progress.isComplete)
        assertEquals(0, progress.gatesRemaining)
        assertEquals(1.0, progress.currentFraction)
    }

    @Test
    fun treatsANegativeBestAsZero() {
        assertEquals(0.0, path.progressFor(mapOf("muscle_up.dead_hang" to -5)).currentFraction)
    }

    @Test
    fun rejectsDuplicateGateIdsAndTargetsBelowOne() {
        val gate = path.gates.first()
        assertFailsWith<IllegalArgumentException> { SkillPath("x", "X", listOf(gate, gate)) }
        assertFailsWith<IllegalArgumentException> { gate.copy(target = 0) }
    }

    @Test
    fun catalogueFindsPathsByIdAndEveryGateIdIsUnique() {
        assertEquals(SkillCatalogue.firstPullUp, SkillCatalogue.byId("first_pull_up"))
        assertEquals(null, SkillCatalogue.byId("nope"))
        val ids = SkillCatalogue.all.flatMap { path -> path.gates.map { it.id } }
        assertEquals(ids.size, ids.toSet().size)
    }
}
