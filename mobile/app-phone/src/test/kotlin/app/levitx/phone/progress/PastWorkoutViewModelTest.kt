package app.levitx.phone.progress

import app.levitx.core.history.WorkoutRecord
import app.levitx.phone.MainDispatcherRule
import app.levitx.testing.FakeCoachRepository
import app.levitx.testing.FakeHistoryRepository
import app.levitx.testing.coachState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

// UnconfinedTestDispatcher is still marked experimental in kotlinx-coroutines-test.
@OptIn(ExperimentalCoroutinesApi::class)
class PastWorkoutViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val workout = WorkoutRecord("w1", 0, 60_000, emptyList())
    private val history = FakeHistoryRepository(listOf(workout))

    @Test
    fun showsTheWorkoutThenGoesOnceDeleted() = runTest {
        val viewModel = PastWorkoutViewModel("w1", history, FakeCoachRepository(coachState()))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertEquals(PastWorkoutUiState.Ready(workout), viewModel.uiState.value)
        viewModel.delete()
        assertEquals(PastWorkoutUiState.Deleted, viewModel.uiState.value)
    }

    @Test
    fun anUnknownWorkoutIsNotFound() = runTest {
        val viewModel = PastWorkoutViewModel("gone", history, FakeCoachRepository(coachState()))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertEquals(PastWorkoutUiState.NotFound, viewModel.uiState.value)
    }
}
