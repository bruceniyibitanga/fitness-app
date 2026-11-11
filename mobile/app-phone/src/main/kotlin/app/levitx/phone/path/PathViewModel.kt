package app.levitx.phone.path

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.skills.SkillPath
import app.levitx.data.DataUnavailableException
import app.levitx.data.goal.GoalRepository
import app.levitx.data.progress.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** What the path screen shows. */
sealed interface PathUiState {
    /** Loading. */
    data object Loading : PathUiState

    /** Data couldn't be unlocked. */
    data object Unavailable : PathUiState

    /** No skill goal: general fitness. */
    data object NoGoal : PathUiState

    /**
     * The whole path.
     *
     * @property path The goal.
     * @property currentIndex Index of the gate being worked on, or null when the path is complete.
     * @property bestByGateId The person's best per gate.
     */
    data class Ready(val path: SkillPath, val currentIndex: Int?, val bestByGateId: Map<String, Int>) : PathUiState
}

/** Every gate on the active path with the person's best at each. */
@HiltViewModel
class PathViewModel @Inject constructor(goalRepository: GoalRepository, progressRepository: ProgressRepository) :
    ViewModel() {
    /** The current screen state. */
    val uiState: StateFlow<PathUiState> =
        combine(goalRepository.observeActiveGoal(), progressRepository.observeBests()) { path, bests ->
            if (path == null) {
                PathUiState.NoGoal
            } else {
                PathUiState.Ready(path, path.progressFor(bests).currentIndex, bests)
            }
        }
            .catch { error -> if (error is DataUnavailableException) emit(PathUiState.Unavailable) else throw error }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PathUiState.Loading)
}

private const val STOP_TIMEOUT_MILLIS = 5_000L
