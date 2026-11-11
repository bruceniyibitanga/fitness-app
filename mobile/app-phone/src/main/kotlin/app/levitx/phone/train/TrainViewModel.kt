package app.levitx.phone.train

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.coach.Coach
import app.levitx.data.coach.CoachRepository
import app.levitx.data.time.TrainingClock
import app.levitx.phone.isExpectedDataFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Keep collecting for this long after the screen stops listening, so a
 * rotation doesn't restart the flow. Coming back later works the workouts out
 * again for the new time, so recovery is always current.
 */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/** The Train tab: skill programs, and a workout for each part of the body. */
@HiltViewModel
class TrainViewModel @Inject constructor(
    coachRepository: CoachRepository,
    private val coach: Coach,
    private val clock: TrainingClock,
) : ViewModel() {
    /** The current screen state. */
    val uiState: StateFlow<TrainUiState> = coachRepository.observeState()
        .map { state ->
            if (state == null) {
                TrainUiState.Loading
            } else {
                TrainUiState.Ready(
                    programs = programsFor(state.model),
                    workouts = workoutsFor(state.model, coach, clock.currentTimeMillis()),
                )
            }
        }
        .catch { error -> if (error.isExpectedDataFailure()) emit(TrainUiState.Unavailable) else throw error }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TrainUiState.Loading)
}
