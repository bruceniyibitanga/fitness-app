package app.levitx.wear.session

import app.levitx.core.session.SessionPlanner
import app.levitx.core.skills.GateMeasure
import app.levitx.core.skills.SkillCatalogue
import app.levitx.testing.FakeGoalRepository
import app.levitx.testing.FakeProgressRepository
import app.levitx.testing.FakeSessionDraftRepository
import app.levitx.testing.FakeSessionRepository
import app.levitx.testing.FakeTrainingClock
import app.levitx.wear.MainDispatcherRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val path = SkillCatalogue.muscleUp
    private val dips = path.gates[1]
    private val clock = FakeTrainingClock()
    private val progress = FakeProgressRepository(mapOf(path.gates[0].id to 30, dips.id to 6))
    private val controller = WorkoutController(
        FakeGoalRepository(path),
        progress,
        FakeSessionDraftRepository(),
        FakeSessionRepository(progress),
        clock,
        CoroutineScope(UnconfinedTestDispatcher()),
    )
    private val viewModel by lazy { SessionViewModel(controller, clock) }

    @Test
    fun withNoSessionTheScreenClosesItself() = runTest {
        assertEquals(SessionUiState.NoSession, currentState())
    }

    @Test
    fun showsTheSetToDo() = runTest {
        controller.start()
        val plan = SessionPlanner.planFor(dips, best = 6)

        val state = currentState() as SessionUiState.Working

        assertEquals(1, state.setNumber)
        assertEquals(plan.sets.size, state.setCount)
        assertEquals(plan.sets.first().target, state.target)
        assertEquals(GateMeasure.REPS, state.measure)
        assertFalse(state.isBestEffort)
        assertFalse(state.canSaveEarly)
    }

    @Test
    fun showsRestThenTheNextSet() = runTest {
        controller.start()
        viewModel.logSet(4)

        val resting = currentState() as SessionUiState.Resting
        assertEquals(clock.elapsed + SessionPlanner.REST_SECONDS_REPS * 1_000L, resting.restEndsAtElapsedMillis)
        assertEquals(2, resting.nextSetNumber)

        viewModel.endRest()
        val working = currentState() as SessionUiState.Working
        assertEquals(2, working.setNumber)
        assertTrue(working.canSaveEarly)
    }

    @Test
    fun showsTheSummaryThenClosesWhenDismissed() = runTest {
        controller.start()
        viewModel.logSet(8)
        viewModel.finishEarly()

        val finished = currentState() as SessionUiState.Finished
        assertEquals(dips.title, finished.gateTitle)
        assertEquals(8, finished.sessionBest)
        assertTrue(finished.isNewBest)
        assertFalse(finished.passedGate)

        viewModel.dismissSummary()
        assertEquals(SessionUiState.NoSession, currentState())
    }

    /** Subscribes like a screen would, then returns the latest state. */
    private fun TestScope.currentState(): SessionUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel.uiState.value
    }
}
