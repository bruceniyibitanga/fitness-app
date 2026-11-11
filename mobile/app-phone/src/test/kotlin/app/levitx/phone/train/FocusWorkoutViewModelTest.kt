package app.levitx.phone.train

import app.levitx.core.coach.Coach
import app.levitx.core.coach.WorkoutFocus
import app.levitx.phone.MainDispatcherRule
import app.levitx.phone.coachState
import app.levitx.phone.workout.WorkoutStarter
import app.levitx.testing.FakeCoachRepository
import app.levitx.testing.FakeTrainingClock
import app.levitx.testing.FakeWorkoutDraftRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// UnconfinedTestDispatcher is still marked experimental in kotlinx-coroutines-test.
@OptIn(ExperimentalCoroutinesApi::class)
class FocusWorkoutViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val drafts = FakeWorkoutDraftRepository()
    private val clock = FakeTrainingClock()

    private fun viewModelFor(focus: String) = FocusWorkoutViewModel(
        focus,
        FakeCoachRepository(coachState(), drafts),
        drafts,
        Coach(),
        clock,
        WorkoutStarter(drafts, clock),
    )

    @Test
    fun startingSavesTheWorkoutAsTheOneInProgress() = runTest {
        val viewModel = viewModelFor(WorkoutFocus.LEGS.name)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val ready = viewModel.uiState.value as FocusWorkoutUiState.Ready
        assertFalse(ready.hasWorkoutInProgress)

        assertTrue(viewModel.start())
        assertEquals(ready.session, drafts.current?.session)
        assertTrue((viewModel.uiState.value as FocusWorkoutUiState.Ready).hasWorkoutInProgress)
    }

    @Test
    fun anUnknownFocusShowsNothingToStart() = runTest {
        val viewModel = viewModelFor("NOT_A_FOCUS")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertEquals(FocusWorkoutUiState.NotFound, viewModel.uiState.value)
        assertFalse(viewModel.start())
    }
}
