package app.levitx.core.session

import app.levitx.core.skills.SkillCatalogue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TrainingDayTest {
    private val path = SkillCatalogue.firstPullUp

    @Test
    fun newPersonTrainsTheFirstGate() {
        val day = path.trainingDay(emptyMap())
        assertEquals(path.gates.first(), day.currentGate)
        assertEquals(0, day.currentBest)
        assertEquals(1, day.gateNumber)
        assertEquals(path.gates.first(), day.plan?.gate)
    }

    @Test
    fun planUsesTheBestAtTheCurrentGate() {
        val first = path.gates[0]
        val second = path.gates[1]
        val day = path.trainingDay(mapOf(first.id to first.target, second.id to 6))
        assertEquals(second, day.currentGate)
        assertEquals(6, day.currentBest)
        assertEquals(SessionPlanner.planFor(second, 6), day.plan)
    }

    @Test
    fun completePathHasNoPlan() {
        val day = path.trainingDay(path.gates.associate { it.id to it.target })
        assertNull(day.currentGate)
        assertNull(day.plan)
        assertEquals(path.gates.size, day.gateNumber)
    }

    @Test
    fun sessionBestIsItsBiggestSet() {
        val session = CompletedSession(
            gateId = "g",
            startedAtEpochMillis = 0,
            finishedAtEpochMillis = 1,
            sets = listOf(LoggedSet(5, 2, false), LoggedSet(8, 0, true), LoggedSet(5, 1, false)),
        )
        assertEquals(8, session.best)
    }

    @Test
    fun sessionNeedsASetAndSaneTimes() {
        assertFailsWith<IllegalArgumentException> { CompletedSession("g", 0, 1, emptyList()) }
        assertFailsWith<IllegalArgumentException> { CompletedSession("g", 2, 1, listOf(LoggedSet(1, null, true))) }
        assertFailsWith<IllegalArgumentException> { LoggedSet(-1, null, false) }
    }
}
