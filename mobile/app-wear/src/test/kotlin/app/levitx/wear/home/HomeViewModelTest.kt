package app.levitx.wear.home

import app.levitx.core.skills.SkillCatalogue
import app.levitx.core.skills.SkillPath
import app.levitx.data.DataUnavailableException
import app.levitx.data.equipment.Band
import app.levitx.data.goal.GoalRepository
import app.levitx.testing.FakeEquipmentRepository
import app.levitx.testing.FakeGoalRepository
import app.levitx.testing.skillSession
import app.levitx.wear.MainDispatcherRule
import app.levitx.wear.WatchFakes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val path = SkillCatalogue.muscleUp
    private val fakes = WatchFakes()
    private val controller = fakes.controller()
    private val equipment = FakeEquipmentRepository(bands = listOf(Band("b1", "Red", 20.0)))

    private fun viewModel(goal: GoalRepository = FakeGoalRepository(path)) =
        HomeViewModel(goal, fakes.next, equipment, fakes.outbox, controller)

    @Test
    fun showsTheCoachsNextSession() = runTest {
        val state = currentState(viewModel()) as HomeUiState.Ready

        assertEquals(path.name, state.goalName)
        assertEquals(checkNotNull(fakes.next.current).summary(), state.next)
        assertFalse(state.isSessionRunning)
    }

    @Test
    fun generalFitnessHasNoGoalButStillASession() = runTest {
        val state = currentState(viewModel(goal = FakeGoalRepository())) as HomeUiState.Ready

        assertNull(state.goalName)
        assertTrue(state.next != null)
    }

    @Test
    fun withNoSessionFromThePhoneThereIsNothingToStart() = runTest {
        fakes.next.replace(null)
        val state = currentState(viewModel()) as HomeUiState.Ready

        assertNull(state.next)
        assertNull(state.load)
    }

    @Test
    fun loadIsOnlyOfferedWhenTheSkillWorkTracksIt() = runTest {
        val state = currentState(viewModel()) as HomeUiState.Ready
        assertEquals(LoadSummary(addedKg = 0.0, bandName = null), state.load)

        // A dead hang is a hold, which has no load to choose.
        fakes.next.replace(skillSession(path.gates[0], target = 20))
        assertNull((currentState(viewModel()) as HomeUiState.Ready).load)
    }

    @Test
    fun aRunningWorkoutCanBeResumed() = runTest {
        val viewModel = viewModel()
        viewModel.startSession()

        val state = currentState(viewModel) as HomeUiState.Ready

        assertTrue(state.isSessionRunning)
        assertNull(state.load)
    }

    @Test
    fun countsWorkoutsWaitingForThePhone() = runTest {
        val viewModel = viewModel()
        viewModel.startSession()
        controller.logSet(5)
        controller.finishEarly()

        val state = currentState(viewModel) as HomeUiState.Ready

        assertEquals(1, state.waitingToSync)
        // The plan waits for the phone to take the workout in.
        assertNull(state.next)
    }

    @Test
    fun lockedDataShowsUnavailableInsteadOfCrashing() = runTest {
        val locked = object : GoalRepository by FakeGoalRepository() {
            override fun observeActiveGoal(): Flow<SkillPath?> = flow { throw DataUnavailableException("locked") }
        }
        assertEquals(HomeUiState.Unavailable, currentState(viewModel(goal = locked)))
    }

    /** Subscribes like a screen would, then returns the latest state. */
    private fun TestScope.currentState(viewModel: HomeViewModel): HomeUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel.uiState.value
    }
}
