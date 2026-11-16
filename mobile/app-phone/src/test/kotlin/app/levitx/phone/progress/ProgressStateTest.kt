package app.levitx.phone.progress

import app.levitx.core.coach.MovementPattern
import app.levitx.core.history.ExerciseRecord
import app.levitx.core.history.WorkoutRecord
import app.levitx.core.session.LoggedSet
import app.levitx.core.skills.SkillCatalogue
import app.levitx.testing.newcomer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressStateTest {
    private val day = 24L * 60 * 60 * 1000
    private val now = 100 * day

    private fun set(value: Int) = LoggedSet(value, repsInReserve = 2, isBestEffort = false)

    private fun workout(dayNumber: Int, vararg exercises: Pair<String, Int>) = WorkoutRecord(
        id = "w$dayNumber",
        startedAtEpochMillis = dayNumber * day,
        finishedAtEpochMillis = dayNumber * day + 40 * 60_000L,
        exercises = exercises.map { (id, reps) -> ExerciseRecord(id, listOf(set(reps), set(reps - 1)), 80.0) },
    )

    private val workouts = listOf(
        workout(10, "horizontal_push.knees" to 12, "core.plank" to 30),
        workout(80, "horizontal_push.push_up" to 8),
        workout(95, "horizontal_push.push_up" to 10, "core.plank" to 40),
    )

    @Test
    fun theRangeDecidesWhichWorkoutsCount() {
        val month = progressState(workouts, newcomer(), ProgressRange.FOUR_WEEKS, now)
        val all = progressState(workouts, newcomer(), ProgressRange.ALL, now)
        assertEquals(2, checkNotNull(month.headline).points.size)
        assertEquals(3, checkNotNull(all.headline).points.size)
        assertEquals(3, month.workoutCount)
    }

    @Test
    fun withoutAGoalFigureTheMovementWithMostFiguresLeads() {
        val state = progressState(workouts, newcomer(), ProgressRange.ALL, now)
        assertEquals(MovementPattern.HORIZONTAL_PUSH, checkNotNull(state.headline).pattern)
        assertTrue(checkNotNull(state.headline).changeKg > 0.0)
    }

    @Test
    fun everyMovementShowsItsLatestExerciseAndHoldsShowTheirBest() {
        val moves = progressState(workouts, newcomer(), ProgressRange.ALL, now).moves.associateBy { it.pattern }
        assertEquals("horizontal_push.push_up", moves.getValue(MovementPattern.HORIZONTAL_PUSH).exerciseId)
        val core = moves.getValue(MovementPattern.CORE)
        assertNull(core.kgNow)
        assertEquals(40, core.best)
    }

    @Test
    fun noWorkoutsMeansNothingToLeadWith() {
        val state = progressState(emptyList(), newcomer(), ProgressRange.ALL, now)
        assertNull(state.headline)
        assertTrue(state.moves.isEmpty())
        assertEquals(0, state.workoutCount)
    }

    @Test
    fun aPassedGateIsListed() {
        val hang = SkillCatalogue.firstPullUp.gates.first()
        val model = newcomer().copy(gateBests = mapOf(hang.id to hang.target))
        assertEquals(listOf(hang), progressState(workouts, model, ProgressRange.ALL, now).gates.map { it.gate })
    }

    @Test
    fun anExercisesRecordsAndRecentWorkoutsComeFromItsHistory() {
        val state = exerciseProgressState("horizontal_push.push_up", workouts, newcomer())
            as ExerciseProgressUiState.Ready
        assertEquals(10, state.records.bestSet.value)
        assertEquals(workouts[2].finishedAtEpochMillis, state.records.bestSet.atEpochMillis)
        assertEquals(19, state.records.mostInOneGo.value)
        assertEquals(listOf(8, 10), state.bars.map { it.value })
        assertEquals(listOf("w95", "w80"), state.recent.map { it.workout.id })
    }

    @Test
    fun anExerciseNeverDoneIsNotFound() {
        assertEquals(
            ExerciseProgressUiState.NotFound,
            exerciseProgressState("vertical_pull.pull_up", workouts, newcomer()),
        )
    }

    @Test
    fun theMonthStartsOnMondayAndMarksTrainedAndPlannedDays() {
        // 1 November 2026 is a Sunday.
        val month = YearMonth.of(2026, 11)
        val trainedOn = LocalDate.of(2026, 11, 3).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val record = WorkoutRecord("n", trainedOn, trainedOn + 1, emptyList())
        val state = historyState(
            month = month,
            workouts = listOf(record),
            trainingDays = setOf(DayOfWeek.MONDAY),
            today = LocalDate.of(2026, 11, 12),
            zone = ZoneOffset.UTC,
        )
        assertEquals(0, state.days.size % 7)
        assertEquals(6, state.days.indexOfFirst { it.date != null })
        assertTrue(state.days.first { it.date?.dayOfMonth == 3 }.trained)
        assertTrue(state.days.first { it.date?.dayOfMonth == 16 }.planned)
        assertFalse(state.days.first { it.date?.dayOfMonth == 9 }.planned)
        assertTrue(state.days.first { it.date?.dayOfMonth == 12 }.isToday)
        assertFalse(state.canGoForward)
        assertFalse(state.canGoBack)
        assertEquals(listOf(record), state.workouts)
    }
}
