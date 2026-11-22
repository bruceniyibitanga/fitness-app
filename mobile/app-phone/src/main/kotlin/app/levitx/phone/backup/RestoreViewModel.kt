package app.levitx.phone.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.data.backup.BackupProblem
import app.levitx.data.backup.RecoveryWords
import app.levitx.data.backup.RestoreResult
import app.levitx.phone.handlingDataFailures
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Why the last try didn't restore, for the screen to say in a line. */
enum class RestoreIssue {
    /** The words don't open it, or the file was changed: the two can't be told apart. */
    WRONG_WORDS,

    /** Not a Levitx backup. */
    NOT_A_BACKUP,

    /** Made by a newer Levitx. */
    NEWER_VERSION,

    /** It opened, but what's inside can't be used, or the data couldn't be written. */
    UNUSABLE,

    /** Far bigger than any backup. */
    TOO_LARGE,

    /** A workout is in progress. */
    WORKOUT_IN_PROGRESS,

    /** The file couldn't be read. */
    UNREADABLE_FILE,
}

/** The restore flow, as the "Restore a backup" artboard. */
sealed interface RestoreUiState {
    /** Pick the file. */
    data object Choose : RestoreUiState

    /**
     * Type the six words.
     *
     * @property issue Why the last try didn't work, or null before one.
     */
    data class Words(val issue: RestoreIssue? = null) : RestoreUiState

    /** Unlocking and checking: about a second, then the swap is one transaction. */
    data object Working : RestoreUiState

    /** Everything is back. */
    data object Restored : RestoreUiState
}

/**
 * Restores a backup the person picks, with the six words they type. Nothing on
 * the phone changes unless the words open it and everything inside checks
 * out; the person has already said yes to replacing everything by then.
 *
 * The file's address and the words are held in memory only.
 */
@HiltViewModel
class RestoreViewModel @Inject constructor(private val files: BackupFiles) : ViewModel() {
    private var uri: String? = null
    private val state = MutableStateFlow<RestoreUiState>(RestoreUiState.Choose)

    /** The current step. */
    val uiState: StateFlow<RestoreUiState> = state.asStateFlow()

    /** They picked the file at [uri]. */
    fun chosen(uri: String) {
        this.uri = uri
        state.value = RestoreUiState.Words()
    }

    /**
     * Restores with the words in [typed]. Does nothing unless it is six words
     * from the list (the screen counts them as they're typed) or a restore is
     * already running.
     */
    fun restore(typed: String) {
        val file = uri
        val words = RecoveryWords.parse(typed)
        if (file == null || words == null || state.value == RestoreUiState.Working) return
        state.value = RestoreUiState.Working
        viewModelScope.launch {
            state.value = handlingDataFailures(onFailure = { RestoreUiState.Words(RestoreIssue.UNUSABLE) }) {
                try {
                    stateFor(files.restore(file, words))
                } catch (_: IOException) {
                    RestoreUiState.Words(RestoreIssue.UNREADABLE_FILE)
                }
            }
        }
    }

    private fun stateFor(result: RestoreResult): RestoreUiState = when (result) {
        RestoreResult.Restored -> RestoreUiState.Restored

        RestoreResult.WorkoutInProgress -> RestoreUiState.Words(RestoreIssue.WORKOUT_IN_PROGRESS)

        is RestoreResult.Failed -> RestoreUiState.Words(
            when (result.problem) {
                BackupProblem.WRONG_WORDS_OR_DAMAGED -> RestoreIssue.WRONG_WORDS
                BackupProblem.NOT_A_BACKUP -> RestoreIssue.NOT_A_BACKUP
                BackupProblem.NEWER_VERSION -> RestoreIssue.NEWER_VERSION
                BackupProblem.UNREADABLE_CONTENTS -> RestoreIssue.UNUSABLE
                BackupProblem.TOO_LARGE -> RestoreIssue.TOO_LARGE
            },
        )
    }

    override fun onCleared() {
        uri = null
    }
}
