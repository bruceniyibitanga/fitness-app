package app.levitx.phone.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.data.DefaultDispatcher
import app.levitx.data.backup.RecoveryWords
import app.levitx.phone.handlingDataFailures
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlin.random.Random
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The backup flow, as the "Back up: your six words" artboard. */
sealed interface BackupUiState {
    /** Making the words: a moment while the word list loads. */
    data object Preparing : BackupUiState

    /**
     * Step 1: the words to write down.
     *
     * @property words The six words, in order.
     */
    data class Words(val words: List<String>) : BackupUiState

    /**
     * Step 2: type two of them back, by their place (from 0).
     *
     * @property first The first word asked for.
     * @property second The second word asked for.
     * @property saveFailed True after a save that didn't work, so the screen says so.
     */
    data class Check(val first: Int, val second: Int, val saveFailed: Boolean = false) : BackupUiState

    /** Writing the file: the key takes about a second. */
    data object Saving : BackupUiState

    /** Step 3: saved. */
    data object Saved : BackupUiState
}

/**
 * Makes six new words, checks the person wrote them down, then saves a backup
 * locked with them to the file they choose.
 *
 * The words live only in this ViewModel's memory: never in saved state, logs
 * or the database, so they're gone when the screen is. If Android ends the
 * app part way, the next try makes new words, and nothing was saved with the
 * old ones. Kotlin strings can't be wiped, so the reference is dropped as soon
 * as the screen is done with it.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val files: BackupFiles,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private var words: List<String> = emptyList()
    private val state = MutableStateFlow<BackupUiState>(BackupUiState.Preparing)

    /** The current step. */
    val uiState: StateFlow<BackupUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            // Reading the word list is a little disk work; keep it off the main thread.
            words = withContext(defaultDispatcher) { RecoveryWords.generate() }
            state.value = BackupUiState.Words(words)
        }
    }

    /** They say they've written the words down: ask for two of them, picked at random. */
    fun writtenDown() {
        if (state.value !is BackupUiState.Words) return
        val (first, second) = (0 until RecoveryWords.COUNT).shuffled(Random).take(2).sorted()
        state.value = BackupUiState.Check(first, second)
    }

    /** Back to the words, to look again. */
    fun showWordsAgain() {
        if (state.value is BackupUiState.Check) state.value = BackupUiState.Words(words)
    }

    /** True when [typed] is the word at [index], however it was capitalised or spaced. */
    fun matches(index: Int, typed: String): Boolean = words.getOrNull(index) == typed.trim().lowercase()

    /** Saves the backup to [uri], the file they chose. Once at a time. */
    fun save(uri: String) {
        val check = state.value as? BackupUiState.Check ?: return
        state.value = BackupUiState.Saving
        viewModelScope.launch {
            state.value = handlingDataFailures(onFailure = { check.copy(saveFailed = true) }) {
                try {
                    files.write(uri, words)
                    words = emptyList()
                    BackupUiState.Saved
                } catch (_: IOException) {
                    // Full storage, or the provider went away. Nothing in the app changed; they can pick another place.
                    check.copy(saveFailed = true)
                }
            }
        }
    }

    override fun onCleared() {
        words = emptyList()
    }
}
