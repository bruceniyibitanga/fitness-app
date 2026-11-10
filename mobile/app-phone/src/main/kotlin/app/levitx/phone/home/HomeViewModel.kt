package app.levitx.phone.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.skills.PathProgress
import app.levitx.core.skills.SkillCatalogue
import app.levitx.core.skills.SkillPath
import app.levitx.data.progress.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Keep collecting for this long after the screen stops listening, so a
 * rotation (which briefly drops the collector) doesn't restart the flow.
 */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Turns the person's progress into [HomeUiState] for the home screen.
 *
 * Muscle-up is the only path until the goal picker arrives in Phase 2.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(progressRepository: ProgressRepository) : ViewModel() {
    private val path: SkillPath = SkillCatalogue.muscleUp

    /** The current screen state. Starts as [HomeUiState.Loading]. */
    val uiState: StateFlow<HomeUiState> = progressRepository.observeProgress(path)
        .map { progress -> progress.toUiState(path) }
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
        currentGate = index?.let { path.gates[it] },
        gateNumber = (index ?: path.gates.lastIndex) + 1,
        gateCount = path.gates.size,
        gateFraction = currentFraction.toFloat(),
    )
}
