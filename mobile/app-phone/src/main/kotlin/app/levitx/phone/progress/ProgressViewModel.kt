package app.levitx.phone.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.data.DefaultDispatcher
import app.levitx.data.coach.CoachRepository
import app.levitx.data.history.HistoryRepository
import app.levitx.data.time.TrainingClock
import app.levitx.phone.isExpectedDataFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/** Keep collecting through a rotation. */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Progress: load per rep over time, every movement, this week's hard sets,
 * workouts per week and gates passed. Everything comes from workouts stored
 * in the encrypted database on this phone. Nothing is estimated.
 */
@HiltViewModel
class ProgressViewModel @Inject constructor(
    history: HistoryRepository,
    coachRepository: CoachRepository,
    private val clock: TrainingClock,
    @param:DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val range = MutableStateFlow(ProgressRange.THREE_MONTHS)

    /** The current screen state. */
    val uiState: StateFlow<ProgressUiState> =
        combine(history.observeWorkouts(), coachRepository.observeState(), range) { workouts, state, range ->
            if (state == null) {
                ProgressUiState.Loading
            } else {
                progressState(workouts, state.model, range, clock.currentTimeMillis())
            }
        }
            // Working the figures out is CPU work; keep it off the main thread.
            .flowOn(defaultDispatcher)
            .catch { error -> if (error.isExpectedDataFailure()) emit(ProgressUiState.Unavailable) else throw error }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ProgressUiState.Loading)

    /** Looks back over [newRange] instead. */
    fun chooseRange(newRange: ProgressRange) {
        range.value = newRange
    }
}
