package app.levitx.phone.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.session.trainingDay
import app.levitx.data.DataUnavailableException
import app.levitx.data.equipment.EquipmentRepository
import app.levitx.data.goal.GoalRepository
import app.levitx.data.progress.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Keep collecting for this long after the screen stops listening, so a
 * rotation (which briefly drops the collector) doesn't restart the flow.
 */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/** Combines the active goal, the person's bests and whether bodyweight is known into [HomeUiState]. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    goalRepository: GoalRepository,
    progressRepository: ProgressRepository,
    equipmentRepository: EquipmentRepository,
) : ViewModel() {
    /** The current screen state. Starts as [HomeUiState.Loading]. */
    val uiState: StateFlow<HomeUiState> =
        combine(
            goalRepository.observeActiveGoal(),
            progressRepository.observeBests(),
            equipmentRepository.observeBodyweightKg(),
        ) { path, bests, bodyweight ->
            if (path == null) HomeUiState.NoGoal else HomeUiState.Ready(path.trainingDay(bests), bodyweight != null)
        }
            // Only the expected failure becomes a screen state. Anything else is
            // a bug and should crash loudly rather than hide behind an error message.
            .catch { error -> if (error is DataUnavailableException) emit(HomeUiState.Unavailable) else throw error }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = HomeUiState.Loading,
            )
}
