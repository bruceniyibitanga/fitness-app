package app.levitx.phone.workout

import app.levitx.core.coach.Effort
import app.levitx.core.coach.ExerciseLookup
import app.levitx.core.coach.ExercisePlan
import app.levitx.core.coach.Intensity
import app.levitx.core.coach.PlannedSession
import app.levitx.core.coach.WorkoutRun
import app.levitx.core.session.PlannedSet
import app.levitx.phone.MainDispatcherRule
import app.levitx.testing.FakeCoachRepository
import app.levitx.testing.FakeEquipmentRepository
import app.levitx.testing.FakeTrainingClock
import app.levitx.testing.FakeWorkoutDraftRepository
import app.levitx.testing.coachState
import app.levitx.testing.startedWorkout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkoutViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val clock = FakeTrainingClock()
    private val drafts = FakeWorkoutDraftRepository(startedWorkout(nowEpochMillis = clock.now))
    private val coachRepository = FakeCoachRepository(coachState(), drafts)
    private val viewModel by lazy { WorkoutViewModel(coachRepository, drafts, FakeEquipmentRepository(), clock) }

    private val active: WorkoutUiState.Active
        get() = viewModel.uiState.value as WorkoutUiState.Active

    @Test
    fun withNoWorkoutInProgressThereIsNothingToShow() {
        val empty = FakeWorkoutDraftRepository()
        val viewModel = WorkoutViewModel(coachRepository, empty, FakeEquipmentRepository(), clock)
        assertEquals(WorkoutUiState.NoWorkout, viewModel.uiState.value)
    }

    @Test
    fun aLoggedSetIsSavedAndStartsRest() {
        viewModel.logSet(active.nextSet.target, Effort.RIGHT.repsInReserve)
        val saved = checkNotNull(drafts.current)
        assertEquals(1, saved.logged.sumOf { it.size })
        assertTrue(active.run.isResting)
        assertEquals(active.run, saved)
    }

    @Test
    fun noSetCanBeLoggedDuringRest() {
        viewModel.logSet(active.nextSet.target, Effort.RIGHT.repsInReserve)
        viewModel.logSet(active.nextSet.target, Effort.RIGHT.repsInReserve)
        assertEquals(1, active.run.logged.sumOf { it.size })
        viewModel.rest(RestAction.END)
        assertNull(active.run.restEndsAtElapsedMillis)
    }

    @Test
    fun finishingHandsTheWorkoutToTheCoachAndClearsTheDraft() {
        viewModel.logSet(active.nextSet.target, Effort.RIGHT.repsInReserve)
        viewModel.finish()
        assertEquals(WorkoutUiState.Finished::class, viewModel.uiState.value::class)
        assertEquals(1, (viewModel.uiState.value as WorkoutUiState.Finished).report.sets)
        assertEquals(setOf("workout-1"), coachRepository.recordedWorkouts)
        assertNull(drafts.current)
    }

    @Test
    fun finishingWithNothingDoneThrowsTheWorkoutAway() {
        viewModel.finish()
        assertEquals(WorkoutUiState.NoWorkout, viewModel.uiState.value)
        assertTrue(coachRepository.recordedWorkouts.isEmpty())
        assertNull(drafts.current)
    }

    @Test
    fun skippingEveryExerciseEndsTheWorkoutWithNothingSaved() {
        repeat(active.run.session.plans.size) { viewModel.skipExercise() }
        assertEquals(WorkoutUiState.NoWorkout, viewModel.uiState.value)
        assertTrue(coachRepository.recordedWorkouts.isEmpty())
    }

    @Test
    fun impossibleLoadsAreClampedBeforeTheyReachTheRun() {
        viewModel.logSet(active.nextSet.target, Effort.RIGHT.repsInReserve, addedKg = Double.NaN, assistanceKg = -5.0)
        val set = checkNotNull(drafts.current).logged.flatten().single()
        assertEquals(0.0, set.addedKg, 0.0)
        assertEquals(0.0, set.assistanceKg, 0.0)
    }

    @Test
    fun restCanBeMadeLongerOrShorter() {
        viewModel.logSet(active.nextSet.target, Effort.RIGHT.repsInReserve)
        val endsAt = checkNotNull(active.run.restEndsAtElapsedMillis)
        viewModel.rest(RestAction.LONGER)
        assertEquals(endsAt + 15_000, active.run.restEndsAtElapsedMillis)
        viewModel.rest(RestAction.SHORTER)
        viewModel.rest(RestAction.SHORTER)
        assertEquals(endsAt - 15_000, checkNotNull(drafts.current).restEndsAtElapsedMillis)
    }

    @Test
    fun undoTakesBackTheSetJustLoggedAndSavesThat() {
        viewModel.logSet(active.nextSet.target, Effort.RIGHT.repsInReserve)
        assertTrue(active.canUndo)
        viewModel.rest(RestAction.UNDO)
        assertEquals(0, active.run.logged.sumOf { it.size })
        assertNull(active.run.restEndsAtElapsedMillis)
        assertEquals(0, checkNotNull(drafts.current).logged.sumOf { it.size })
    }

    @Test
    fun aCountedSetHasNoTimer() {
        val counted = timedWorkout(firstId = "squat.bodyweight")
        counted.timer(TimerAction.START)
        assertNull((counted.uiState.value as WorkoutUiState.Active).timer)
    }

    @Test
    fun aStoppedHoldFillsInTheSecondsHeld() {
        val holds = timedWorkout(firstId = "core.plank")
        holds.timer(TimerAction.START)
        val timer = checkNotNull((holds.uiState.value as WorkoutUiState.Active).timer)
        assertEquals(TimerKind.HOLD, timer.kind)
        // Through the count-in, then 23.5 seconds of holding.
        clock.elapsed += LEAD_IN_MILLIS + 23_500
        holds.timer(TimerAction.STOP)
        val stopped = holds.uiState.value as WorkoutUiState.Active
        assertNull(stopped.timer)
        assertEquals(23, stopped.counted)
    }

    @Test
    fun negativesArePacedAtTheExercisesLoweringTime() {
        val negatives = timedWorkout(firstId = "vertical_pull.negative")
        negatives.timer(TimerAction.START)
        negatives.timer(TimerAction.NOW)
        val timer = checkNotNull((negatives.uiState.value as WorkoutUiState.Active).timer)
        assertEquals(TimerKind.NEGATIVE, timer.kind)
        assertEquals(5_000L, timer.lowerMillis)
        assertEquals(3, timer.reps)
        assertEquals(clock.elapsed, timer.startsAtElapsedMillis)
    }

    @Test
    fun loggingASetEndsTheTimerAndItsCount() {
        val holds = timedWorkout(firstId = "core.plank")
        holds.timer(TimerAction.START)
        clock.elapsed += LEAD_IN_MILLIS + 20_000
        holds.timer(TimerAction.STOP)
        holds.logSet(20, repsInReserve = null)
        val resting = holds.uiState.value as WorkoutUiState.Active
        assertNull(resting.counted)
        assertNull(resting.timer)
        assertNotNull(resting.lastSet)
    }

    @Test
    fun theChosenLoadIsKeptInTheRun() {
        viewModel.chooseLoad(addedKg = 5.0, assistanceKg = 0.0)
        assertEquals(5.0, checkNotNull(drafts.current).load.addedKg, 0.0)
    }

    /** A view model for a workout that starts with [firstId], three sets of three. */
    private fun timedWorkout(firstId: String): WorkoutViewModel {
        val plan = ExercisePlan(
            exercise = checkNotNull(ExerciseLookup.byId(firstId)),
            sets = List(3) { PlannedSet(3, false) },
            restSeconds = 90,
            repsInReserve = 2,
            intensity = Intensity.HARD,
        )
        val drafts = FakeWorkoutDraftRepository(WorkoutRun("timed", PlannedSession(listOf(plan)), clock.now))
        return WorkoutViewModel(FakeCoachRepository(coachState(), drafts), drafts, FakeEquipmentRepository(), clock)
    }
}
