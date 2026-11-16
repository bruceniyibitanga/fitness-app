package app.levitx.phone.settings

import app.levitx.core.load.WeightUnit
import app.levitx.data.DataUnavailableException
import app.levitx.phone.MainDispatcherRule
import app.levitx.testing.FakeSettingsRepository
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// UnconfinedTestDispatcher is still marked experimental in kotlinx-coroutines-test.
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val settings = FakeSettingsRepository()

    private class FakeExport(private val failure: Exception? = null) : DataExport {
        val written = mutableListOf<String>()

        override suspend fun exportTo(uri: String) {
            failure?.let { throw it }
            written += uri
        }
    }

    private class FakeEraser(private val failure: Exception? = null) : DataEraser {
        var erased = false

        override suspend fun eraseEverything() {
            failure?.let { throw it }
            erased = true
        }
    }

    @Test
    fun changesAreSavedAndShown() = runTest {
        val viewModel = SettingsViewModel(settings, FakeExport(), FakeEraser())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.setLeadInSeconds(5)
        viewModel.setSounds(false)
        viewModel.setBuzzes(false)
        viewModel.setUnits(WeightUnit.LB)
        val shown = viewModel.uiState.value.settings
        assertEquals(5, shown.leadInSeconds)
        assertEquals(WeightUnit.LB, shown.units)
        assertFalse(shown.sounds)
        assertFalse(shown.buzzes)
    }

    @Test
    fun anExportGoesToTheChosenFile() = runTest {
        val export = FakeExport()
        val viewModel = SettingsViewModel(settings, export, FakeEraser())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.export("content://picked/levitx-export.zip")
        assertEquals(listOf("content://picked/levitx-export.zip"), export.written)
        assertEquals(DataTask.EXPORTED, viewModel.uiState.value.task)
    }

    @Test
    fun aFileThatCantBeWrittenSaysSo() = runTest {
        val viewModel = SettingsViewModel(settings, FakeExport(IOException("full")), FakeEraser())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.export("content://picked/file.zip")
        assertEquals(DataTask.EXPORT_FAILED, viewModel.uiState.value.task)
    }

    @Test
    fun deletingEverythingEndsInDeleted() = runTest {
        val eraser = FakeEraser()
        val viewModel = SettingsViewModel(settings, FakeExport(), eraser)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.deleteEverything()
        assertTrue(eraser.erased)
        assertEquals(DataTask.DELETED, viewModel.uiState.value.task)
    }

    @Test
    fun lockedDataCantBeDeletedAndSaysSo() = runTest {
        val viewModel = SettingsViewModel(settings, FakeExport(), FakeEraser(DataUnavailableException("locked")))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        viewModel.deleteEverything()
        assertEquals(DataTask.DELETE_FAILED, viewModel.uiState.value.task)
    }
}
