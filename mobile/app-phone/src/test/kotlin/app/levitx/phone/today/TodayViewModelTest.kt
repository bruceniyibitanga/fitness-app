package app.levitx.phone.today

import app.levitx.core.coach.Coach
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// UnconfinedTestDispatcher is still marked experimental in kotlinx-coroutines-test.
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val drafts = FakeWorkoutDraftRepository()
    private val coachRepository = FakeCoachRepository(coachState(), drafts)
    private val clock = FakeTrainingClock()

    // Lazy, so it is built after the rule has swapped in the test main dispatcher.
    private val viewModel by lazy {
        TodayViewModel(coachRepository, drafts, Coach(), clock, WorkoutStarter(drafts, clock))
    }

    private val ready: TodayUiState.Ready
        get() = viewModel.uiState.value as TodayUiState.Ready

    @Test
    fun startingSavesTheSessionAsTheWorkoutInProgress() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val session = checkNotNull(ready.recommendation.session)
        assertFalse(ready.hasWorkoutInProgress)

        assertTrue(viewModel.start())
        val run = checkNotNull(drafts.current)
        assertEquals(session, run.session)
        assertEquals(clock.now, run.startedAtEpochMillis)
        assertTrue(ready.hasWorkoutInProgress)
    }

    @Test
    fun startingAgainResumesInsteadOfStartingOver() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.start()
        val first = checkNotNull(drafts.current).id
        assertTrue(viewModel.start())
        assertEquals(first, drafts.current?.id)
    }

    @Test
    fun restingIsRecordedWithTheCoach() = runTest {
        val before = coachRepository.current?.model
        viewModel.rest()
        assertNotEquals(before, coachRepository.current?.model)
    }
}
