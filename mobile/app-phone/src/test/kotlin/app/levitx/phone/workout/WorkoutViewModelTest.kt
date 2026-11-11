package app.levitx.phone.workout

import app.levitx.core.coach.Effort
import app.levitx.phone.MainDispatcherRule
import app.levitx.phone.coachState
import app.levitx.phone.startedWorkout
import app.levitx.testing.FakeCoachRepository
import app.levitx.testing.FakeEquipmentRepository
import app.levitx.testing.FakeTrainingClock
import app.levitx.testing.FakeWorkoutDraftRepository
import org.junit.Assert.assertEquals
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
        viewModel.logSet(active.nextSet.target, Effort.RIGHT)
        val saved = checkNotNull(drafts.current)
        assertEquals(1, saved.logged.sumOf { it.size })
        assertTrue(active.run.isResting)
        assertEquals(active.run, saved)
    }

    @Test
    fun noSetCanBeLoggedDuringRest() {
        viewModel.logSet(active.nextSet.target, Effort.RIGHT)
        viewModel.logSet(active.nextSet.target, Effort.RIGHT)
        assertEquals(1, active.run.logged.sumOf { it.size })
        viewModel.endRest()
        assertNull(active.run.restEndsAtElapsedMillis)
    }

    @Test
    fun finishingHandsTheWorkoutToTheCoachAndClearsTheDraft() {
        viewModel.logSet(active.nextSet.target, Effort.RIGHT)
        viewModel.finish()
        assertEquals(WorkoutUiState.Finished::class, viewModel.uiState.value::class)
        assertEquals(1, (viewModel.uiState.value as WorkoutUiState.Finished).sets)
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
        viewModel.logSet(active.nextSet.target, Effort.RIGHT, addedKg = Double.NaN, assistanceKg = -5.0)
        val set = checkNotNull(drafts.current).logged.flatten().single()
        assertEquals(0.0, set.addedKg, 0.0)
        assertEquals(0.0, set.assistanceKg, 0.0)
    }
}
