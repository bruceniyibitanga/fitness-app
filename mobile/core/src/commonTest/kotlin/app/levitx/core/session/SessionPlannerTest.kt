package app.levitx.core.session

import app.levitx.core.skills.SkillCatalogue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SessionPlannerTest {
    private val pullUps = SkillCatalogue.muscleUp.gates.first { it.id == "muscle_up.pull_ups" }
    private val deadHang = SkillCatalogue.muscleUp.gates.first { it.id == "muscle_up.dead_hang" }

    @Test
    fun workSetsAreAboutSeventyPercentOfBest() {
        val plan = SessionPlanner.planFor(pullUps, best = 7)
        val work = plan.sets.filterNot { it.isBestEffort }
        assertEquals(SessionPlanner.WORK_SETS, work.size)
        assertTrue(work.all { it.target == 5 })
    }

    @Test
    fun lastSetIsABestEffortOnePastTheBest() {
        val plan = SessionPlanner.planFor(pullUps, best = 7)
        assertEquals(PlannedSet(target = 8, isBestEffort = true), plan.sets.last())
    }

    @Test
    fun bestEffortNeverAimsPastTheGate() {
        val plan = SessionPlanner.planFor(pullUps, best = pullUps.target)
        assertEquals(pullUps.target, plan.sets.last().target)
    }

    @Test
    fun someoneWhoCantDoOneStillGetsASet() {
        val plan = SessionPlanner.planFor(pullUps, best = 0)
        assertTrue(plan.sets.all { it.target >= 1 })
    }

    @Test
    fun holdsHaveAMinimumLengthAndShorterRest() {
        val plan = SessionPlanner.planFor(deadHang, best = 4)
        assertTrue(plan.sets.all { it.target >= SessionPlanner.MIN_HOLD_SECONDS })
        assertEquals(SessionPlanner.REST_SECONDS_HOLDS, plan.restSeconds)
    }

    @Test
    fun repSetsRestLonger() {
        assertEquals(SessionPlanner.REST_SECONDS_REPS, SessionPlanner.planFor(pullUps, best = 7).restSeconds)
    }

    @Test
    fun rejectsANegativeBest() {
        assertFailsWith<IllegalArgumentException> { SessionPlanner.planFor(pullUps, best = -1) }
    }
}
