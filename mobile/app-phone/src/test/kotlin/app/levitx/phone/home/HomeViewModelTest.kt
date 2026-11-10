package app.levitx.phone.home

import app.levitx.core.skills.SkillCatalogue
import app.levitx.data.DataUnavailableException
import app.levitx.data.progress.ProgressRepository
import app.levitx.phone.MainDispatcherRule
import app.levitx.testing.FakeGoalRepository
import app.levitx.testing.FakeProgressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val path = SkillCatalogue.muscleUp
    private val progress = FakeProgressRepository()

    @Test
    fun noGoalShowsNoGoal() = runTest {
        assertEquals(HomeUiState.NoGoal, currentState(HomeViewModel(FakeGoalRepository(), progress)))
    }

    @Test
    fun newPersonTrainsTheFirstGate() = runTest {
        val state = currentState(HomeViewModel(FakeGoalRepository(path), progress)) as HomeUiState.Ready
        assertEquals(path.gates.first(), state.day.currentGate)
        assertEquals(1, state.day.gateNumber)
    }

    @Test
    fun passingAGateMovesToTheNextOne() = runTest {
        val viewModel = HomeViewModel(FakeGoalRepository(path), progress)
        val first = path.gates.first()
        progress.recordBest(first.id, first.target)
        assertEquals(2, (currentState(viewModel) as HomeUiState.Ready).day.gateNumber)
    }

    @Test
    fun completedPathHasNoPlan() = runTest {
        path.gates.forEach { progress.recordBest(it.id, it.target) }
        val state = currentState(HomeViewModel(FakeGoalRepository(path), progress)) as HomeUiState.Ready
        assertNull(state.day.plan)
    }

    @Test
    fun lockedDataShowsUnavailableInsteadOfCrashing() = runTest {
        val locked = object : ProgressRepository {
            override fun observeBests(): Flow<Map<String, Int>> = flow { throw DataUnavailableException("locked") }

            override suspend fun recordBest(gateId: String, best: Int) = Unit
        }
        assertEquals(HomeUiState.Unavailable, currentState(HomeViewModel(FakeGoalRepository(path), locked)))
    }

    /** Subscribes like a screen would, then returns the latest state. */
    private fun TestScope.currentState(viewModel: HomeViewModel): HomeUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel.uiState.value
    }
}
