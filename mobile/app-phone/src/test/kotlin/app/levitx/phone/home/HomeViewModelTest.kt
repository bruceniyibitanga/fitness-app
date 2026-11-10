package app.levitx.phone.home

import app.levitx.core.skills.PathProgress
import app.levitx.core.skills.SkillCatalogue
import app.levitx.core.skills.SkillPath
import app.levitx.data.DataUnavailableException
import app.levitx.data.progress.InMemoryProgressRepository
import app.levitx.data.progress.ProgressRepository
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
    private val repository = InMemoryProgressRepository()

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
    fun newPersonSeesTheFirstGate() = runTest {
        val state = readyState(HomeViewModel(repository))
        assertEquals(path.gates.first(), state.currentGate)
        assertEquals(1, state.gateNumber)
        assertEquals(path.gates.size, state.gateCount)
    }

    @Test
    fun passingAGateMovesToTheNextOne() = runTest {
        val viewModel = HomeViewModel(repository)
        val first = path.gates.first()
        repository.recordBest(first.id, first.target)
        assertEquals(2, readyState(viewModel).gateNumber)
    }

    @Test
    fun completedPathHasNoCurrentGate() = runTest {
        path.gates.forEach { repository.recordBest(it.id, it.target) }
        val state = readyState(HomeViewModel(repository))
        assertNull(state.currentGate)
        assertEquals(path.gates.size, state.gateNumber)
    }

    @Test
    fun lockedDataShowsUnavailableInsteadOfCrashing() = runTest {
        val locked = object : ProgressRepository {
            override fun observeProgress(path: SkillPath): Flow<PathProgress> = flow {
                throw DataUnavailableException("locked")
            }

            override suspend fun recordBest(gateId: String, best: Int) = Unit
        }
        assertEquals(HomeUiState.Unavailable, currentState(HomeViewModel(locked)))
    }

    /** Subscribes like a screen would, then returns the latest state. */
    private fun TestScope.currentState(viewModel: HomeViewModel): HomeUiState {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel.uiState.value
    }

    private fun TestScope.readyState(viewModel: HomeViewModel): HomeUiState.Ready =
        currentState(viewModel) as HomeUiState.Ready
}
