package app.levitx.phone.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.history.WeekReview
import app.levitx.core.history.WeekReviews
import app.levitx.data.DefaultDispatcher
import app.levitx.data.coach.CoachPrompt
import app.levitx.data.coach.CoachRepository
import app.levitx.data.history.HistoryRepository
import app.levitx.data.time.TrainingClock
import app.levitx.phone.handlingDataFailures
import app.levitx.phone.isExpectedDataFailure
import app.levitx.phone.ui.WeekBounds
import app.levitx.phone.ui.weekBounds
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Keep collecting through a rotation. */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/** What the week review shows. */
sealed interface WeekReviewUiState {
    /** Still reading. */
    data object Loading : WeekReviewUiState

    /** The encrypted data couldn't be opened. */
    data object Unavailable : WeekReviewUiState

    /**
     * Last week, looked back on.
     *
     * @property review The figures and notes.
     * @property week Where the week starts and ends.
     */
    data class Ready(val review: WeekReview, val week: WeekBounds) : WeekReviewUiState
}

/**
 * The week review: last week, Monday to Sunday, from the workouts stored in
 * the encrypted database. Opening it counts as seeing it, so Today stops
 * offering it until the next week ends.
 */
@HiltViewModel
class WeekReviewViewModel @Inject constructor(
    history: HistoryRepository,
    private val coachRepository: CoachRepository,
    private val clock: TrainingClock,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val week = weekBounds(clock.currentTimeMillis(), ZoneId.systemDefault())

    /** The current screen state. */
    val uiState: StateFlow<WeekReviewUiState> =
        combine(history.observeWorkouts(), coachRepository.observeState()) { workouts, state ->
            if (state == null) {
                WeekReviewUiState.Loading
            } else {
                WeekReviewUiState.Ready(
                    WeekReviews.of(state.model, workouts, week.lastWeekFromEpochMillis, week.thisWeekFromEpochMillis),
                    week,
                )
            }
        }
            // Comparing every set with every earlier one is CPU work; keep it off the main thread.
            .flowOn(defaultDispatcher)
            .catch { error -> if (error.isExpectedDataFailure()) emit(WeekReviewUiState.Unavailable) else throw error }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), WeekReviewUiState.Loading)

    init {
        viewModelScope.launch {
            handlingDataFailures(onFailure = {}) {
                coachRepository.markShown(CoachPrompt.WEEK_REVIEW, clock.currentTimeMillis())
            }
        }
    }
}
