package app.levitx.wear.home

import app.levitx.core.skills.SkillCatalogue
import app.levitx.data.DataUnavailableException
import app.levitx.data.progress.ProgressRepository
import app.levitx.testing.FakeGoalRepository
import app.levitx.testing.FakeProgressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val path = SkillCatalogue.muscleUp
    private val progress = FakeProgressRepository()

    @Before
    fun setUp() {
        // viewModelScope runs on the main dispatcher, which unit tests don't have.
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun noGoalAsksForOneOnThePhone() = runTest {
        assertEquals(HomeUiState.NoGoal, currentState(HomeViewModel(FakeGoalRepository(), progress)))
    }

    @Test
    fun showsTheCurrentGate() = runTest {
        val first = path.gates.first()
        progress.recordBest(first.id, first.target)
        val state = currentState(HomeViewModel(FakeGoalRepository(path), progress)) as HomeUiState.Ready
        assertEquals(path.gates[1].title, state.gateTitle)
        assertEquals(2, state.gateNumber)
    }

    @Test
    fun completedPathHasNoCurrentGate() = runTest {
        path.gates.forEach { progress.recordBest(it.id, it.target) }
        val state = currentState(HomeViewModel(FakeGoalRepository(path), progress)) as HomeUiState.Ready
        assertNull(state.gateTitle)
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
