package app.levitx.phone.skills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.data.coach.CoachRepository
import app.levitx.phone.isExpectedDataFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Keep collecting for this long after the screen stops listening, so a rotation doesn't restart the flow. */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/** What the skills screen shows. */
sealed interface SkillsUiState {
    /** Loading. */
    data object Loading : SkillsUiState

    /** The data couldn't be read on this device. */
    data object Unavailable : SkillsUiState

    /**
     * One row per movement pattern.
     *
     * @property ladders Every pattern's ladder, in catalogue order.
     */
    data class Ready(val ladders: List<LadderStanding>) : SkillsUiState
}

/**
 * Every movement's ladder and where the person is on it.
 *
 * Read-only: rungs only move when the coach sees the work, so there is
 * nothing to edit here. It all comes from the coach's own model, so this
 * screen can never disagree with what Today plans.
 */
@HiltViewModel
class SkillsViewModel @Inject constructor(coachRepository: CoachRepository) : ViewModel() {
    /** The current screen state. */
    val uiState: StateFlow<SkillsUiState> = coachRepository.observeState()
        .map { state -> state?.let { SkillsUiState.Ready(standings(it.model)) } ?: SkillsUiState.Loading }
        .catch { error -> if (error.isExpectedDataFailure()) emit(SkillsUiState.Unavailable) else throw error }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SkillsUiState.Loading)
}
