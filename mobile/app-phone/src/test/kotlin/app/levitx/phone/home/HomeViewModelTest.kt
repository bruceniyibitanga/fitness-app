package app.levitx.phone.home

import app.levitx.core.skills.SkillCatalogue
import app.levitx.data.progress.InMemoryProgressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    /** Subscribes like a screen would, then returns the latest ready state. */
    private fun TestScope.readyState(viewModel: HomeViewModel): HomeUiState.Ready {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        return viewModel.uiState.value as HomeUiState.Ready
    }
}
