package app.levitx.phone.backup

import app.levitx.data.backup.RecoveryWords
import app.levitx.phone.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// UnconfinedTestDispatcher is still marked experimental in kotlinx-coroutines-test.
@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val mainDispatcher = MainDispatcherRule(dispatcher)

    private val files = FakeBackupFiles()

    private fun viewModel() = BackupViewModel(files, dispatcher)

    private fun BackupViewModel.words(): List<String> = (uiState.value as BackupUiState.Words).words

    @Test
    fun itStartsWithSixWordsFromTheList() = runTest {
        val words = viewModel().words()
        assertEquals(RecoveryWords.COUNT, words.size)
        assertTrue(words.all(RecoveryWords::isWord))
    }

    @Test
    fun theCheckAsksForTwoDifferentWordsAndKnowsThem() = runTest {
        val model = viewModel()
        val words = model.words()
        model.writtenDown()
        val check = model.uiState.value as BackupUiState.Check
        assertNotEquals(check.first, check.second)
        assertTrue(model.matches(check.first, "  ${words[check.first].uppercase()} "))
        assertFalse(model.matches(check.second, words[check.first].takeIf { it != words[check.second] } ?: "nope"))
    }

    @Test
    fun savingWritesThoseWordsOnce() = runTest {
        val model = viewModel()
        val words = model.words()
        model.writtenDown()
        model.save("content://file")
        model.save("content://file")
        assertEquals(BackupUiState.Saved, model.uiState.value)
        assertEquals(listOf("content://file" to words), files.written)
    }

    @Test
    fun aFailedSaveGoesBackToTheCheckAndSaysSo() = runTest {
        files.writeFails = true
        val model = viewModel()
        model.writtenDown()
        model.save("content://file")
        assertTrue((model.uiState.value as BackupUiState.Check).saveFailed)
    }

    @Test
    fun savingBeforeTheCheckDoesNothing() = runTest {
        val model = viewModel()
        model.save("content://file")
        assertTrue(files.written.isEmpty())
    }

    @Test
    fun goingBackShowsTheSameWords() = runTest {
        val model = viewModel()
        val words = model.words()
        model.writtenDown()
        model.showWordsAgain()
        assertEquals(words, model.words())
    }
}
