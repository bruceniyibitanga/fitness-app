package app.levitx.phone.exercise

import app.levitx.core.coach.ExerciseCatalogue
import app.levitx.core.coach.MovementPattern
import app.levitx.core.coach.currentExercise
import app.levitx.phone.MainDispatcherRule
import app.levitx.testing.FakeCoachRepository
import app.levitx.testing.FakeTrainingClock
import app.levitx.testing.FakeWorkoutDraftRepository
import app.levitx.testing.coachState
import app.levitx.testing.newcomer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// UnconfinedTestDispatcher is still marked experimental in kotlinx-coroutines-test.
@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule(UnconfinedTestDispatcher())

    private val onSplitSquats = newcomer().copy(
        rungs = mapOf(
            MovementPattern.SQUAT to ExerciseCatalogue.ladder(MovementPattern.SQUAT).rungs.indexOfFirst { rung ->
                rung.options.any { it.id == "squat.split" }
            },
        ),
    )
    private val coachRepository = FakeCoachRepository(coachState(onSplitSquats), FakeWorkoutDraftRepository())

    private fun viewModel(id: String) = ExerciseViewModel(id, coachRepository, FakeTrainingClock())

    @Test
    fun trainingInsteadMakesItTheOneInThePlan() = runTest {
        val lunge = viewModel("squat.reverse_lunge")
        lunge.trainInstead()
        val model = checkNotNull(coachRepository.current).model
        assertEquals("squat.reverse_lunge", model.currentExercise(MovementPattern.SQUAT)?.id)
        val state = coachRepository.observeState().first()?.let {
            exerciseState(it.model, "squat.reverse_lunge", nowEpochMillis = 0L)
        } as ExerciseUiState.Ready
        assertEquals(SwapState.CURRENT, state.swap)
    }

    @Test
    fun anExerciseAboveTheirLevelChangesNothing() = runTest {
        viewModel("squat.bulgarian").trainInstead()
        assertEquals(onSplitSquats.preferred, checkNotNull(coachRepository.current).model.preferred)
    }

    @Test
    fun anUnknownIdChangesNothing() = runTest {
        viewModel("nope").trainInstead()
        assertEquals(onSplitSquats.preferred, checkNotNull(coachRepository.current).model.preferred)
    }
}
