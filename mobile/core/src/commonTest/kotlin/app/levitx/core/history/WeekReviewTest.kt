package app.levitx.core.history

import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachTuning
import app.levitx.core.coach.SessionResult
import app.levitx.core.coach.TrainingBlock
import app.levitx.core.coach.done
import app.levitx.core.coach.newcomer
import app.levitx.core.session.LoggedSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeekReviewTest {
    private val day = 24L * 60 * 60 * 1000
    private val weekStart = 14 * day
    private val weekEnd = 21 * day

    private fun set(value: Int, addedKg: Double = 0.0) =
        LoggedSet(value, repsInReserve = 2, isBestEffort = false, addedKg = addedKg)

    private fun workout(dayNumber: Int, vararg exercises: ExerciseRecord) = WorkoutRecord(
        id = "w$dayNumber",
        startedAtEpochMillis = dayNumber * day,
        finishedAtEpochMillis = dayNumber * day + 45 * 60_000L,
        exercises = exercises.toList(),
    )

    private fun pushUps(vararg sets: LoggedSet) = ExerciseRecord("horizontal_push.push_up", sets.toList(), 80.0)

    private fun burpees() = ExerciseRecord("cardio.burpees", listOf(set(30)), null)

    @Test
    fun onlyStrengthWorkoutsInTheWeekCount() {
        val workouts = listOf(
            workout(10, pushUps(set(8))),
            workout(15, pushUps(set(8))),
            workout(16, burpees()),
            workout(17, pushUps(set(9))),
        )
        val review = WeekReviews.of(newcomer(), workouts, weekStart, weekEnd)
        assertEquals(2, review.sessionsDone)
        assertEquals(newcomer().profile.sessionsPerWeek, review.sessionsPlanned)
    }

    @Test
    fun aBestMeansNoEarlierSetWasAsGoodAtTheSameOrMoreWeight() {
        val before = workout(10, pushUps(set(10), set(6, addedKg = 5.0)))
        fun bestsWith(vararg sets: LoggedSet) =
            WeekReviews.newBests(listOf(before, workout(15, pushUps(*sets))), weekStart, weekEnd)

        assertEquals(1, bestsWith(set(11)), "more reps at bodyweight")
        assertEquals(1, bestsWith(set(7, addedKg = 5.0)), "more reps with the same weight")
        assertEquals(0, bestsWith(set(10)), "matching isn't beating")
        assertEquals(1, bestsWith(set(9, addedKg = 2.5)), "a weight and rep count never done before")
        assertEquals(0, bestsWith(set(5, addedKg = 5.0)), "fewer reps than before at that weight")
        assertEquals(1, bestsWith(set(4, addedKg = 7.5)), "more weight than ever")
    }

    @Test
    fun anExerciseDoneForTheFirstTimeHasNothingToBeat() {
        assertEquals(0, WeekReviews.newBests(listOf(workout(15, pushUps(set(5)))), weekStart, weekEnd))
    }

    @Test
    fun aMovementWellShortOfItsTargetGetsANote() {
        val review = WeekReviews.of(newcomer(), listOf(workout(15, pushUps(set(8)))), weekStart, weekEnd)
        val short = review.notes.filterIsInstance<WeekNote.ShortOf>()
        assertTrue(short.isNotEmpty() && short.size <= 2)
        assertTrue(short.all { it.sets >= 1 })
        assertEquals(short.sortedByDescending { it.sets }, short, "biggest gap first")
    }

    @Test
    fun theBlockNoteFollowsTheBlockRules() {
        val perWeek = newcomer().profile.sessionsPerWeek
        val late = newcomer().copy(block = TrainingBlock(sessionsDone = CoachTuning.BLOCK_WEEKS * perWeek - 2))
        assertTrue(WeekNote.LighterWeekDue(2) in WeekReviews.of(late, emptyList(), weekStart, weekEnd).notes)
        val lighter = newcomer().copy(block = TrainingBlock(lighterSessionsLeft = 1))
        assertTrue(WeekNote.LighterWeekNow(1) in WeekReviews.of(lighter, emptyList(), weekStart, weekEnd).notes)
        val early = newcomer().copy(block = TrainingBlock(sessionsDone = 1))
        assertTrue(WeekReviews.of(early, emptyList(), weekStart, weekEnd).notes.none { it !is WeekNote.ShortOf })
    }

    @Test
    fun aReviewIsDueOnceAfterAWeekWithTraining() {
        assertFalse(WeekReviews.isDue(newcomer(), weekStart, weekEnd), "nothing to look back on")
        val coach = Coach()
        val model = newcomer()
        val plan = checkNotNull(coach.recommend(model, weekStart + day).session)
        val trained = coach.record(
            model,
            SessionResult(
                plan.plans.map { done(it, it.sets.first().target) },
                weekStart + day,
                weekStart + day + 3_600_000L,
            ),
        )
        assertTrue(WeekReviews.isDue(trained, weekStart, weekEnd))
        assertFalse(WeekReviews.isDue(trained.withWeekReviewed(weekEnd + day), weekStart, weekEnd))
        assertTrue(
            WeekReviews.isDue(trained.withWeekReviewed(weekStart + day), weekStart, weekEnd),
            "seen a week earlier",
        )
    }
}
