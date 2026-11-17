package app.levitx.phone.today

import app.levitx.core.coach.Coach
import app.levitx.phone.MainDispatcherRule
import app.levitx.phone.workout.WorkoutStarter
import app.levitx.testing.FakeCoachRepository
import app.levitx.testing.FakeEquipmentRepository
import app.levitx.testing.FakeTrainingClock
import app.levitx.testing.FakeWorkoutDraftRepository
import app.levitx.testing.coachState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
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
    private val equipment = FakeEquipmentRepository()

    // Lazy, so it is built after the rule has swapped in the test main dispatcher.
    private val viewModel by lazy {
        TodayViewModel(coachRepository, drafts, Coach(), clock, WorkoutStarter(drafts, clock), equipment)
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

    @Test
    fun withNoRecentReadingTheWeighInIsAskedBeforeStarting() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertTrue("no reading this week", ready.weighIn != null)
    }

    @Test
    fun savingTheWeighInRecordsItStartsAndWontAskAgainThisWeek() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertTrue(viewModel.answerWeighIn(kg = 74.2))
        assertEquals(74.2, equipment.bodyweightKg)
        assertEquals(clock.now, coachRepository.current?.model?.weighInAskedAtEpochMillis)
        assertTrue(ready.hasWorkoutInProgress)
        assertNull(ready.weighIn)
    }

    @Test
    fun skippingStartsWithoutAReadingAndStillCountsAsAsked() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertTrue(viewModel.answerWeighIn(kg = null))
        assertNull(equipment.bodyweightKg)
        assertEquals(clock.now, coachRepository.current?.model?.weighInAskedAtEpochMillis)
        assertTrue(ready.hasWorkoutInProgress)
    }

    @Test
    fun aWeightOutsideTheLimitsIsNotSaved() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.answerWeighIn(kg = 5.0)
        assertNull(equipment.bodyweightKg)
    }
}
