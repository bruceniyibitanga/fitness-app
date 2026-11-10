package app.levitx.phone.startingtest

import androidx.lifecycle.SavedStateHandle
import app.levitx.core.skills.SkillCatalogue
import app.levitx.phone.MainDispatcherRule
import app.levitx.testing.FakeGoalRepository
import app.levitx.testing.FakeProgressRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class StartingTestViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val path = SkillCatalogue.firstPullUp
    private val progress = FakeProgressRepository()
    // Lazy so it is built after the main dispatcher rule has run.
    private val viewModel by lazy { StartingTestViewModel(FakeGoalRepository(path), progress, SavedStateHandle()) }

    @Test
    fun passingAGateMovesToTheNext() = runTest {
        subscribe()
        viewModel.submit(path.gates[0].target)
        assertEquals(1, (viewModel.uiState.value as StartingTestUiState.Testing).gateIndex)
        assertEquals(path.gates[0].target, progress.current[path.gates[0].id])
    }

    @Test
    fun fallingShortEndsTheTestAtThatGate() = runTest {
        subscribe()
        viewModel.submit(path.gates[0].target)
        viewModel.submit(4)
        val done = viewModel.uiState.value as StartingTestUiState.Done
        assertEquals(path.gates[1], done.startingGate)
        assertEquals(4, progress.current[path.gates[1].id])
    }

    @Test
    fun cantDoItYetStartsTrainingAtThatGate() = runTest {
        subscribe()
        viewModel.cantDoItYet()
        assertEquals(path.gates[0], (viewModel.uiState.value as StartingTestUiState.Done).startingGate)
        assertNull(progress.current[path.gates[0].id])
    }

    @Test
    fun passingEveryGateMeansNothingLeftToStart() = runTest {
        subscribe()
        path.gates.forEach { viewModel.submit(it.target) }
        assertNull((viewModel.uiState.value as StartingTestUiState.Done).startingGate)
    }

    private fun TestScope.subscribe() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }
}
