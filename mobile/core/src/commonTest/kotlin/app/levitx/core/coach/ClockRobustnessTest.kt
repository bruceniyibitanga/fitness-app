package app.levitx.core.coach

import app.levitx.core.intervals.IntervalCatalogue
import app.levitx.core.session.CompletedSession
import app.levitx.core.session.LoggedSet
import app.levitx.core.session.PlannedSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Wrong clocks and late deliveries. A device's clock can run ahead for a
 * while, and a watch workout can reach the phone days after it was done.
 * Neither may lock the coach up, reorder what happened, or crash it.
 */
class ClockRobustnessTest {
    private val coach = Coach()
    private val squat = exercise("squat.bodyweight")
    private val pushUp = exercise("horizontal_push.knees")

    private fun AthleteModel.after(day: Int, vararg plans: ExercisePlan): AthleteModel =
        coach.record(this, finished(day, *plans.map { done(it, it.workTarget) }.toTypedArray()))

    @Test
    fun aSessionDatedInTheFutureDoesNotKeepItsMovementResting() {
        // The clock was a year ahead when squats were recorded, then put right.
        val model = newcomer().after(day = 365, hardPlan(squat, 12))

        val context = contextOn(day = 3, model)

        assertNull(context.hoursSinceHard(MovementPattern.SQUAT))
        assertNull(context.hoursSinceLastSession())
        assertNull(HardRules.firstBlock(sessionOf(hardPlan(squat, 12)), context))
    }

    @Test
    fun aSessionDatedInTheFutureCountsOnceItsDayComes() {
        val model = newcomer().after(day = 10, hardPlan(squat, 12))

        assertNull(contextOn(day = 9, model).hoursSinceHard(MovementPattern.SQUAT))
        assertEquals(23.0, contextOn(day = 11, model).hoursSinceHard(MovementPattern.SQUAT))
    }

    @Test
    fun aWorkoutThatArrivesLateDoesNotBecomeTheLastSession() {
        // Wednesday's phone workout is recorded first. Monday's watch workout arrives after it.
        val model = newcomer()
            .after(day = 3, hardPlan(pushUp, 8))
            .after(day = 1, hardPlan(squat, 12))

        assertEquals(listOf(1, 3).map { atDay(it) + MILLIS_PER_HOUR }, model.history.map { it.finishedAtEpochMillis })
        val context = contextOn(day = 4, model)
        assertEquals(23.0, context.hoursSinceLastSession())
        assertEquals(listOf(pushUp.id), context.lastSession?.exerciseIds)
        // Squats were three days ago, so they are rested; push-ups were yesterday.
        assertNull(RecoveryRule.blocks(sessionOf(hardPlan(squat, 12)), context))
        assertNotNull(RecoveryRule.blocks(sessionOf(hardPlan(pushUp, 8)), context))
    }

    @Test
    fun aLateWorkoutIsJudgedAgainstWhatCameBeforeItNotAfter() {
        // Three weeks off, then Monday on the watch and Wednesday on the phone. The watch syncs last.
        val before = newcomer().after(day = 1, hardPlan(squat, 12))
        val model = before
            .after(day = 24, hardPlan(pushUp, 8))
            .after(day = 22, hardPlan(squat, 12))

        // Monday's session was the comeback one: a plain count of the block, as if it had arrived on time.
        val onTime = before.after(day = 22, hardPlan(squat, 12)).after(day = 24, hardPlan(pushUp, 8))
        assertEquals(onTime.block, model.block)
        assertEquals(onTime.history, model.history)
    }

    @Test
    fun aResultIsMovedBackToEndNowWhenItsClockWasAhead() {
        val result = finished(day = 10, done(hardPlan(squat, 12), 12))

        val moved = result.endingBy(atDay(4))

        assertEquals(atDay(4), moved.finishedAtEpochMillis)
        assertEquals(MILLIS_PER_HOUR, moved.finishedAtEpochMillis - moved.startedAtEpochMillis)
        assertEquals(result.results, moved.results)
        // One that already ended is left exactly as it is.
        assertEquals(result, result.endingBy(atDay(11)))
    }

    @Test
    fun aStoredSessionIsMovedBackTheSameWay() {
        val session = CompletedSession("cardio.burpees", 5_000, 65_000, listOf(LoggedSet(20, null, false)))

        assertEquals(session.copy(startedAtEpochMillis = 0, finishedAtEpochMillis = 60_000), session.endingBy(60_000))
        assertEquals(session, session.endingBy(65_000))
    }

    @Test
    fun aCardioMoveCanNeverBePlannedSoTheCoachNeverMeetsOne() {
        val burpees = checkNotNull(IntervalCatalogue.moveById("cardio.burpees"))

        assertFailsWith<IllegalArgumentException> {
            ExercisePlan(burpees, listOf(PlannedSet(20, false)), 30, 2, Intensity.HARD)
        }
    }
}
