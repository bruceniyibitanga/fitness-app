package app.levitx.phone.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.data.DefaultDispatcher
import app.levitx.data.coach.CoachRepository
import app.levitx.data.history.HistoryRepository
import app.levitx.data.time.TrainingClock
import app.levitx.phone.isExpectedDataFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Keep collecting through a rotation. */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/** History: every workout, a month at a time, from the encrypted database on this phone. */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    history: HistoryRepository,
    coachRepository: CoachRepository,
    private val clock: TrainingClock,
    @param:DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {
    // Months and days are the person's own, so they follow the phone's time zone.
    private val zone: ZoneId = ZoneId.systemDefault()
    private val month = MutableStateFlow(YearMonth.from(today()))

    /** The current screen state. */
    val uiState: StateFlow<HistoryUiState> =
        combine(history.observeWorkouts(), coachRepository.observeState(), month) { workouts, state, month ->
            // Typed as the screen state, so the catch below can emit Unavailable too.
            val ready: HistoryUiState = historyState(month, workouts, state?.trainingDays.orEmpty(), today(), zone)
            ready
        }
            .flowOn(defaultDispatcher)
            .catch { error -> if (error.isExpectedDataFailure()) emit(HistoryUiState.Unavailable) else throw error }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HistoryUiState.Loading)

    /** Shows the month before, or after when [forward]. Never past this month. */
    fun moveMonth(forward: Boolean) {
        month.update { shown ->
            val next = if (forward) shown.plusMonths(1) else shown.minusMonths(1)
            if (next > YearMonth.from(today())) shown else next
        }
    }

    private fun today(): LocalDate = Instant.ofEpochMilli(clock.currentTimeMillis()).atZone(zone).toLocalDate()
}
