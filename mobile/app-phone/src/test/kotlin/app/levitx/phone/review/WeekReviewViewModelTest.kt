package app.levitx.phone.review

import app.levitx.phone.MainDispatcherRule
import app.levitx.testing.FakeCoachRepository
import app.levitx.testing.FakeHistoryRepository
import app.levitx.testing.FakeTrainingClock
import app.levitx.testing.FakeWorkoutDraftRepository
import app.levitx.testing.coachState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// UnconfinedTestDispatcher is still marked experimental in kotlinx-coroutines-test.
@OptIn(ExperimentalCoroutinesApi::class)
class WeekReviewViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcher = MainDispatcherRule(dispatcher)

    private val coachRepository = FakeCoachRepository(coachState(), FakeWorkoutDraftRepository())
    private val clock = FakeTrainingClock()

    private val viewModel by lazy {
        WeekReviewViewModel(FakeHistoryRepository(), coachRepository, clock, dispatcher)
    }

    @Test
    fun openingTheReviewCountsAsSeeingIt() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertEquals(clock.now, coachRepository.current?.model?.weekReviewedAtEpochMillis)
    }

    @Test
    fun aWeekWithNoWorkoutsIsStillAReview() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val ready = viewModel.uiState.value as WeekReviewUiState.Ready
        assertEquals(0, ready.review.sessionsDone)
        assertTrue(ready.week.lastWeekFromEpochMillis < ready.week.thisWeekFromEpochMillis)
    }
}
