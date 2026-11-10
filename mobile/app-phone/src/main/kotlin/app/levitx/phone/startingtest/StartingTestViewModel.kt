package app.levitx.phone.startingtest

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.skills.Gate
import app.levitx.core.skills.SkillPath
import app.levitx.data.DataUnavailableException
import app.levitx.data.goal.GoalRepository
import app.levitx.data.progress.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the starting test screen shows. */
sealed interface StartingTestUiState {
    /** Loading the goal. */
    data object Loading : StartingTestUiState

    /** Data couldn't be unlocked. */
    data object Unavailable : StartingTestUiState

    /**
     * Testing one gate.
     *
     * @property path The goal.
     * @property gateIndex 0-based index of [gate] on the path.
     * @property gate The gate being tested.
     */
    data class Testing(val path: SkillPath, val gateIndex: Int, val gate: Gate) : StartingTestUiState

    /**
     * Test over.
     *
     * @property path The goal.
     * @property startingGate The first gate not passed, where training starts; null if every gate passed.
     */
    data class Done(val path: SkillPath, val startingGate: Gate?) : StartingTestUiState
}

private const val KEY_GATE_INDEX = "gateIndex"
private const val KEY_DONE = "done"

/**
 * Walks through the gates of the active goal in order, recording the
 * person's best at each, and stops at the first gate they don't pass.
 * Later gates can't count until earlier ones are passed, so testing them
 * would only tire the person out.
 *
 * Where the test is up to lives in [SavedStateHandle], so it survives the
 * app being killed in the background mid-test.
 */
@HiltViewModel
class StartingTestViewModel @Inject constructor(
    goalRepository: GoalRepository,
    private val progressRepository: ProgressRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val gateIndex = savedState.getStateFlow(KEY_GATE_INDEX, 0)
    private val done = savedState.getStateFlow(KEY_DONE, false)

    /** The current screen state. */
    val uiState: StateFlow<StartingTestUiState> =
        combine(goalRepository.observeActiveGoal(), gateIndex, done) { path, index, isDone ->
            when {
                path == null -> StartingTestUiState.Loading
                isDone || index >= path.gates.size -> StartingTestUiState.Done(path, path.gates.getOrNull(index))
                else -> StartingTestUiState.Testing(path, index, path.gates[index])
            }
        }
            .catch { error -> if (error is DataUnavailableException) emit(StartingTestUiState.Unavailable) else throw error }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), StartingTestUiState.Loading)

    /**
     * Records [result] for the gate being tested. Moves to the next gate if
     * it passed, otherwise ends the test with this gate as the starting point.
     */
    fun submit(result: Int) {
        val state = uiState.value as? StartingTestUiState.Testing ?: return
        viewModelScope.launch {
            if (result > 0) progressRepository.recordBest(state.gate.id, result)
            if (result >= state.gate.target) {
                savedState[KEY_GATE_INDEX] = state.gateIndex + 1
            } else {
                savedState[KEY_DONE] = true
            }
        }
    }

    /** The person can't do the current gate yet: training starts here. */
    fun cantDoItYet() {
        savedState[KEY_DONE] = true
    }
}

private const val STOP_TIMEOUT_MILLIS = 5_000L
