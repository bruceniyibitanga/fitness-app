package app.levitx.phone.equipment

import app.levitx.phone.MainDispatcherRule
import app.levitx.testing.FakeEquipmentRepository
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
class EquipmentViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val equipment = FakeEquipmentRepository()
    private val viewModel by lazy { EquipmentViewModel(equipment) }

    @Test
    fun savesBodyweightAndBands() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.saveBodyweight(74.5)
        viewModel.addBand("Red", 22.5)

        val state = viewModel.uiState.value as EquipmentUiState.Ready
        assertEquals(74.5, state.bodyweightKg)
        assertEquals(listOf("Red"), state.bands.map { it.name })
    }

    @Test
    fun ignoresImpossibleValues() = runTest {
        viewModel.saveBodyweight(5.0)
        viewModel.addBand(" ", 10.0)
        viewModel.addBand("Red", 0.0)
        assertTrue(equipment.bands.isEmpty())
    }

    @Test
    fun removesABand() = runTest {
        viewModel.addBand("Red", 22.5)
        viewModel.deleteBand(equipment.bands.single().id)
        assertTrue(equipment.bands.isEmpty())
    }
}
