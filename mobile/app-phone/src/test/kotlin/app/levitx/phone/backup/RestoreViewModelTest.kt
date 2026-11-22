package app.levitx.phone.backup

import app.levitx.data.backup.BackupProblem
import app.levitx.data.backup.RestoreResult
import app.levitx.phone.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// UnconfinedTestDispatcher is still marked experimental in kotlinx-coroutines-test.
@OptIn(ExperimentalCoroutinesApi::class)
class RestoreViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule(UnconfinedTestDispatcher())

    private val files = FakeBackupFiles()
    private val typed = "Abacus, zoom t-shirt\nabstract yo-yo felt-tip"
    private val words = listOf("abacus", "zoom", "t-shirt", "abstract", "yo-yo", "felt-tip")

    private fun chosen() = RestoreViewModel(files).also { it.chosen("content://backup") }

    @Test
    fun sixListWordsRestoreTheChosenFile() = runTest {
        val model = chosen()
        model.restore(typed)
        assertEquals(RestoreUiState.Restored, model.uiState.value)
        assertEquals(listOf("content://backup" to words), files.restored)
    }

    @Test
    fun anythingButSixListWordsIsNotTried() = runTest {
        val model = chosen()
        model.restore("abacus zoom")
        model.restore("abacus zoom t-shirt abstract yo-yo levitx")
        assertTrue(files.restored.isEmpty())
        assertEquals(RestoreUiState.Words(), model.uiState.value)
    }

    @Test
    fun nothingIsTriedBeforeAFileIsChosen() = runTest {
        RestoreViewModel(files).restore(typed)
        assertTrue(files.restored.isEmpty())
    }

    @Test
    fun eachProblemSaysWhatWentWrong() = runTest {
        val expected = mapOf(
            RestoreResult.Failed(BackupProblem.WRONG_WORDS_OR_DAMAGED) to RestoreIssue.WRONG_WORDS,
            RestoreResult.Failed(BackupProblem.NOT_A_BACKUP) to RestoreIssue.NOT_A_BACKUP,
            RestoreResult.Failed(BackupProblem.NEWER_VERSION) to RestoreIssue.NEWER_VERSION,
            RestoreResult.Failed(BackupProblem.UNREADABLE_CONTENTS) to RestoreIssue.UNUSABLE,
            RestoreResult.Failed(BackupProblem.TOO_LARGE) to RestoreIssue.TOO_LARGE,
            RestoreResult.WorkoutInProgress to RestoreIssue.WORKOUT_IN_PROGRESS,
            null to RestoreIssue.UNREADABLE_FILE,
        )
        expected.forEach { (result, issue) ->
            files.restoreResult = result
            val model = chosen()
            model.restore(typed)
            assertEquals("$result", RestoreUiState.Words(issue), model.uiState.value)
        }
    }

    @Test
    fun aSecondTryAfterWrongWordsCanWork() = runTest {
        files.restoreResult = RestoreResult.Failed(BackupProblem.WRONG_WORDS_OR_DAMAGED)
        val model = chosen()
        model.restore(typed)
        files.restoreResult = RestoreResult.Restored
        model.restore(typed)
        assertEquals(RestoreUiState.Restored, model.uiState.value)
    }
}
