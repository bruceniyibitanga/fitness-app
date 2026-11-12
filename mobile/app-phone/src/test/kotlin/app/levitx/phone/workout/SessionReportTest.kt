package app.levitx.phone.workout

import app.levitx.core.coach.Coach
import app.levitx.core.coach.WorkoutRun
import app.levitx.core.skills.SkillCatalogue
import app.levitx.testing.newcomer
import app.levitx.testing.skillSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionReportTest {
    private val coach = Coach()
    private val gate = SkillCatalogue.firstPullUp.gates[0]

    /** The skill session with every set done at [value], reps to spare, over [minutes]. */
    private fun report(value: Int, minutes: Int = 30, bodyweightKg: Double? = 75.0): SessionReport {
        var run = WorkoutRun("w", skillSession(gate, target = 4), startedAtEpochMillis = 0)
        while (!run.isComplete) {
            run = run.logSet(value, repsInReserve = 3, nowElapsedMillis = 0).endRest()
        }
        val result = checkNotNull(run.result(finishedAtEpochMillis = minutes * 60_000L))
        val before = newcomer()
        return sessionReport(before, coach.record(before, result), result, bodyweightKg)
    }

    @Test
    fun countsEverySetAndTheTimeTaken() {
        val report = report(value = 4, minutes = 42)
        assertEquals(5, report.sets)
        assertEquals(42, report.minutes)
        assertEquals(listOf(3, 2), report.exercises.map { it.sets.size })
    }

    @Test
    fun aGateBestIsReportedAsANewBest() {
        val report = report(value = 4)
        val best = report.bests.single { it.name == gate.title }
        assertEquals(4, best.value)
    }

    @Test
    fun withoutBodyweightNoKilogramsAreClaimed() {
        assertNull(report(value = 4, bodyweightKg = null).kgMoved)
    }

    @Test
    fun theCoachsChangesComeFromTheModelNotAGuess() {
        val report = report(value = 4)
        assertTrue(report.changes.all { change -> report.exercises.any { it.name == change.name } })
    }

    @Test
    fun aBestThatReachesTheGateIsMarkedAsPassingIt() {
        val passing = report(value = gate.target)
        assertTrue(passing.bests.single { it.name == gate.title }.passedGate)
        assertTrue(report(value = 4).bests.none { it.passedGate })
    }
}
