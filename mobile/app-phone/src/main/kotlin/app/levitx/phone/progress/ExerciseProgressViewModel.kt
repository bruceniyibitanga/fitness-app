package app.levitx.phone.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.data.DefaultDispatcher
import app.levitx.data.coach.CoachRepository
import app.levitx.data.history.HistoryRepository
import app.levitx.phone.isExpectedDataFailure
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/** Keep collecting through a rotation. */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/** One exercise's progress: best sets over time, records and recent workouts. */
@HiltViewModel(assistedFactory = ExerciseProgressViewModel.Factory::class)
class ExerciseProgressViewModel @AssistedInject constructor(
    @Assisted exerciseId: String,
    history: HistoryRepository,
    coachRepository: CoachRepository,
    @param:DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    /** The current screen state. */
    val uiState: StateFlow<ExerciseProgressUiState> =
        combine(history.observeWorkouts(), coachRepository.observeState()) { workouts, state ->
            if (state == null) {
                ExerciseProgressUiState.Loading
            } else {
                exerciseProgressState(exerciseId, workouts, state.model)
            }
        }
            .flowOn(defaultDispatcher)
            .catch { error ->
                if (error.isExpectedDataFailure()) emit(ExerciseProgressUiState.Unavailable) else throw error
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                ExerciseProgressUiState.Loading,
            )

    /** Makes one for an exercise. */
    @AssistedFactory
    interface Factory {
        /** The view model for [exerciseId]. */
        fun create(exerciseId: String): ExerciseProgressViewModel
    }
}
