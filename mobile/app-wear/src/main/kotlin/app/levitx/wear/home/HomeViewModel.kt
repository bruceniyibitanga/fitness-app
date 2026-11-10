package app.levitx.wear.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.skills.PathProgress
import app.levitx.core.skills.SkillCatalogue
import app.levitx.core.skills.SkillPath
import app.levitx.data.DataUnavailableException
import app.levitx.data.progress.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Keep collecting for this long after the screen stops listening, so a
 * brief interruption (like ambient mode) doesn't restart the flow.
 */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Turns the person's progress into [HomeUiState] for the watch.
 *
 * In Phase 3 this screen becomes the start of a session; for now it shows the
 * next gate so the watch reads the same data as the phone.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(progressRepository: ProgressRepository) : ViewModel() {
    private val path: SkillPath = SkillCatalogue.muscleUp

    /** The current screen state. Starts as [HomeUiState.Loading]. */
    val uiState: StateFlow<HomeUiState> = progressRepository.observeProgress(path)
        .map<PathProgress, HomeUiState> { progress -> progress.toUiState(path) }
        // Only the expected failure becomes a screen state. Anything else is
        // a bug and should crash loudly rather than hide behind an error message.
        .catch { error -> if (error is DataUnavailableException) emit(HomeUiState.Unavailable) else throw error }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HomeUiState.Loading,
        )
}

private fun PathProgress.toUiState(path: SkillPath): HomeUiState.Ready {
    val index = currentIndex
    return HomeUiState.Ready(
        skillName = path.name,
        gateTitle = index?.let { path.gates[it].title },
        gateNumber = (index ?: path.gates.lastIndex) + 1,
        gateCount = path.gates.size,
    )
}
