package app.levitx.core.session

import app.levitx.core.skills.SkillCatalogue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionRunTest {
    private val pullUps = SkillCatalogue.muscleUp.gates.first { it.id == "muscle_up.pull_ups" }
    private val plan = SessionPlanner.planFor(pullUps, best = 6)

    private fun run() = SessionRun(id = "run-1", plan = plan, previousBest = 6, startedAtEpochMillis = 1_000)

    @Test
    fun startsOnTheFirstSetAndNotResting() {
        val run = run()
        assertEquals(plan.sets.first(), run.nextSet)
        assertEquals(0, run.nextSetIndex)
        assertFalse(run.isResting)
        assertFalse(run.isComplete)
    }

    @Test
    fun loggingASetStartsRestOnTheElapsedClock() {
        val run = run().logSet(4, repsInReserve = 2, nowElapsedMillis = 50_000)
        assertEquals(50_000 + plan.restSeconds * 1_000L, run.restEndsAtElapsedMillis)
        assertEquals(1, run.nextSetIndex)
        assertEquals(LoggedSet(4, 2, isBestEffort = false), run.logged.single())
    }

    @Test
    fun cantLogDuringRest() {
        val resting = run().logSet(4, 2, 0)
        assertEquals(resting, resting.logSet(4, 2, 0))
    }

    @Test
    fun setsAreLoggedWithTheCurrentLoad() {
        val run = run().withLoad(SetLoad(addedKg = 5.0)).logSet(4, null, 0).endRest()
            .withLoad(SetLoad(assistanceKg = 20.0)).logSet(6, null, 0)
        assertEquals(5.0, run.logged[0].addedKg)
        assertEquals(20.0, run.logged[1].assistanceKg)
        assertEquals(0.0, run.logged[1].addedKg)
    }

    @Test
    fun theLastSetIsTheBestEffortAndEndsWithoutRest() {
        var run = run()
        repeat(SessionPlanner.WORK_SETS) { run = run.logSet(4, 2, 0).endRest() }
        run = run.logSet(8, 0, 0)
        assertTrue(run.isComplete)
        assertFalse(run.isResting)
        assertNull(run.nextSet)
        assertTrue(run.logged.last().isBestEffort)
        assertEquals(run, run.logSet(3, 0, 0))
    }

    @Test
    fun completingGivesTheSessionToSave() {
        val session = assertNotNull(run().logSet(5, 1, 0).complete(finishedAtEpochMillis = 9_000, bodyweightKg = 74.0))
        assertEquals(pullUps.id, session.gateId)
        assertEquals(1_000, session.startedAtEpochMillis)
        assertEquals(9_000, session.finishedAtEpochMillis)
        assertEquals(74.0, session.bodyweightKg)
        assertEquals(5, session.best)
    }

    @Test
    fun aSessionWithNoSetsHasNothingToSave() {
        assertNull(run().complete(finishedAtEpochMillis = 9_000))
    }

    @Test
    fun aClockMovedBackNeverFinishesBeforeTheStart() {
        val session = assertNotNull(run().logSet(5, 1, 0).complete(finishedAtEpochMillis = 10))
        assertEquals(1_000, session.finishedAtEpochMillis)
    }

    @Test
    fun negativeInputCountsAsZero() {
        val set = run().logSet(-3, -1, 0).logged.single()
        assertEquals(0, set.value)
        assertEquals(0, set.repsInReserve)
    }

    @Test
    fun rejectsImpossibleRuns() {
        assertFailsWith<IllegalArgumentException> { run().copy(id = " ") }
        assertFailsWith<IllegalArgumentException> { run().copy(previousBest = -1) }
        assertFailsWith<IllegalArgumentException> {
            run().copy(logged = List(plan.sets.size + 1) { LoggedSet(1, null, false) })
        }
        assertFailsWith<IllegalArgumentException> { SetLoad(addedKg = -1.0) }
    }

    @Test
    fun gatesCanBeFoundById() {
        assertEquals(pullUps, SkillCatalogue.gateById(pullUps.id))
        assertNull(SkillCatalogue.gateById("nope"))
    }
}
