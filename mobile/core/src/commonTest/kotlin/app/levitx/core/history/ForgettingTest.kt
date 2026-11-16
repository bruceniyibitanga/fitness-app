package app.levitx.core.history

import app.levitx.core.coach.ExerciseMark
import app.levitx.core.coach.ExerciseState
import app.levitx.core.coach.MovementPattern
import app.levitx.core.coach.SessionRecord
import app.levitx.core.coach.newcomer
import app.levitx.core.session.LoggedSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ForgettingTest {
    private val gateId = "first_pull_up.scapular_pulls"
    private val ladderId = "horizontal_push.push_up"

    private fun workout(at: Long, gateReps: Int, pushUps: Int, assistanceKg: Double = 0.0) = WorkoutRecord(
        id = "w$at",
        startedAtEpochMillis = at - 1,
        finishedAtEpochMillis = at,
        exercises = listOf(
            ExerciseRecord(gateId, listOf(LoggedSet(gateReps, 0, true, assistanceKg = assistanceKg)), 80.0),
            ExerciseRecord(ladderId, listOf(LoggedSet(pushUps, 2, false)), 80.0),
        ),
    )

    private val first = workout(1_000, gateReps = 7, pushUps = 10)
    private val mistake = workout(2_000, gateReps = 12, pushUps = 30)

    private val model = newcomer().copy(
        gateBests = mapOf(gateId to 12, "other.gate" to 5),
        exercises = mapOf(
            ladderId to ExerciseState(
                target = 12,
                best = 30,
                timesTrained = 2,
                marks = listOf(ExerciseMark(1_000, 10, null, true), ExerciseMark(2_000, 30, null, true)),
            ),
        ),
        history = listOf(
            SessionRecord(
                1_000,
                mapOf(MovementPattern.HORIZONTAL_PUSH to 1),
                mapOf(MovementPattern.HORIZONTAL_PUSH to 1.0),
                listOf(ladderId),
                40,
            ),
            SessionRecord(
                2_000,
                mapOf(MovementPattern.HORIZONTAL_PUSH to 1),
                mapOf(MovementPattern.HORIZONTAL_PUSH to 1.0),
                listOf(ladderId),
                40,
            ),
        ),
    )

    @Test
    fun theWorkoutLeavesTheWeekAndItsBestsGoBack() {
        val after = Forgetting.forget(model, mistake, remaining = listOf(first))
        assertEquals(listOf(1_000L), after.history.map { it.finishedAtEpochMillis })
        assertEquals(7, after.gateBests[gateId])
        assertEquals(5, after.gateBests["other.gate"])
        val pushUps = after.exercises.getValue(ladderId)
        assertEquals(10, pushUps.best)
        assertEquals(1, pushUps.timesTrained)
        assertEquals(listOf(1_000L), pushUps.marks.map { it.atEpochMillis })
        // The target the coach moved stays: undoing it would be a guess.
        assertEquals(12, pushUps.target)
    }

    @Test
    fun aGateWithNoStrictSetLeftHasNoBest() {
        val assisted = workout(1_000, gateReps = 9, pushUps = 10, assistanceKg = 10.0)
        val after = Forgetting.forget(model, mistake, remaining = listOf(assisted))
        assertFalse(gateId in after.gateBests)
    }
}
