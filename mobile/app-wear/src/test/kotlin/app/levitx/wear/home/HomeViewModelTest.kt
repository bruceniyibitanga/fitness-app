package app.levitx.wear.home

import app.levitx.core.session.CompletedSession
import app.levitx.core.session.LoggedSet
import app.levitx.core.session.SessionPlanner
import app.levitx.core.skills.GateMeasure
import app.levitx.core.skills.SkillCatalogue
import app.levitx.data.DataUnavailableException
import app.levitx.data.equipment.Band
import app.levitx.data.progress.ProgressRepository
import app.levitx.data.session.StoredSession
import app.levitx.testing.FakeEquipmentRepository
import app.levitx.testing.FakeGoalRepository
import app.levitx.testing.FakeProgressRepository
import app.levitx.testing.FakeSessionDraftRepository
import app.levitx.testing.FakeSessionRepository
import app.levitx.testing.FakeTrainingClock
import app.levitx.wear.MainDispatcherRule
import app.levitx.wear.session.WorkoutController
import kotlinx.coroutines.CoroutineScope
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
    private val deadHang = path.gates[0]
    private val dips = path.gates[1]
    private val progress = FakeProgressRepository()
    private val sessions = FakeSessionRepository(progress)
    private val equipment = FakeEquipmentRepository(bands = listOf(Band("b1", "Red", 20.0)))

    private fun viewModel(
        goal: FakeGoalRepository = FakeGoalRepository(path),
        progressRepository: ProgressRepository = progress,
    ): HomeViewModel {
        val controller = WorkoutController(
            goal,
            progress,
            FakeSessionDraftRepository(),
            sessions,
            FakeTrainingClock(),
            CoroutineScope(UnconfinedTestDispatcher()),
        )
        return HomeViewModel(goal, progressRepository, equipment, sessions, controller)
    }

    @Test
    fun noGoalAsksForOneOnThePhone() = runTest {
        assertEquals(HomeUiState.NoGoal, currentState(viewModel(goal = FakeGoalRepository())))
    }

    @Test
    fun showsTheCurrentGateAndTodaysSession() = runTest {
        progress.recordBest(deadHang.id, deadHang.target)
        progress.recordBest(dips.id, 6)
        val plan = SessionPlanner.planFor(dips, best = 6)

        val state = currentState(viewModel()) as HomeUiState.Ready

        assertEquals(dips.title, state.gateTitle)
        assertEquals(2, state.gateNumber)
        assertEquals(TodaySummary(SessionPlanner.WORK_SETS, plan.sets.first().target, GateMeasure.REPS), state.today)
        assertFalse(state.isSessionRunning)
    }

    @Test
    fun loadIsOnlyOfferedForGatesThatTrackIt() = runTest {
        // The first gate is a hold, which has no load to choose.
        assertNull((currentState(viewModel()) as HomeUiState.Ready).load)

        progress.recordBest(deadHang.id, deadHang.target)
        val state = currentState(viewModel()) as HomeUiState.Ready
        assertEquals(LoadSummary(addedKg = 0.0, bandName = null), state.load)
    }

    @Test
    fun aRunningSessionCanBeResumed() = runTest {
        val viewModel = viewModel()
        viewModel.startSession()

        val state = currentState(viewModel) as HomeUiState.Ready

        assertTrue(state.isSessionRunning)
        assertNull(state.load)
    }

    @Test
    fun countsSessionsWaitingForThePhone() = runTest {
        val session = CompletedSession(dips.id, 1, 2, listOf(LoggedSet(5, null, true)))
        sessions.saveAndQueue(StoredSession("00000000-0000-4000-8000-000000000001", session))

        assertEquals(1, (currentState(viewModel()) as HomeUiState.Ready).waitingToSync)
    }

    @Test
    fun completedPathHasNoSessionToStart() = runTest {
        path.gates.forEach { progress.recordBest(it.id, it.target) }
        val state = currentState(viewModel()) as HomeUiState.Ready
        assertNull(state.gateTitle)
        assertNull(state.today)
    }

    @Test
    fun lockedDataShowsUnavailableInsteadOfCrashing() = runTest {
        val locked = object : ProgressRepository {
            override fun observeBests(): Flow<Map<String, Int>> = flow { throw DataUnavailableException("locked") }

            override suspend fun recordBest(gateId: String, best: Int) = Unit
        }
        assertEquals(HomeUiState.Unavailable, currentState(viewModel(progressRepository = locked)))
    }

    /** Subscribes like a screen would, then returns the latest state. */
    private fun TestScope.currentState(viewModel: HomeViewModel): HomeUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel.uiState.value
    }
}
