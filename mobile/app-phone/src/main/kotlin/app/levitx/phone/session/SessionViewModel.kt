package app.levitx.phone.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.session.CompletedSession
import app.levitx.core.session.LoggedSet
import app.levitx.core.session.TrainingDay
import app.levitx.core.session.trainingDay
import app.levitx.data.DataUnavailableException
import app.levitx.data.goal.GoalRepository
import app.levitx.data.progress.ProgressRepository
import app.levitx.data.session.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val KEY_STARTED_AT = "startedAt"
private const val KEY_VALUES = "values"
private const val KEY_RESERVES = "reserves"
private const val KEY_REST_ENDS_AT = "restEndsAt"

/** Stored in place of a null reps-in-reserve, since saved state holds plain int arrays. */
private const val NO_RESERVE = -1
private const val NOT_RESTING = -1L
private const val MILLIS_PER_SECOND = 1_000L

/**
 * Runs one session: shows each set, records what the person did, times the
 * rest in between, then saves the whole session in one go.
 *
 * The plan is fixed when the session starts, so the targets don't move mid-
 * session. Sets done and the rest end time are kept in [SavedStateHandle], so
 * a session survives the app being killed in the background between sets.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val progressRepository: ProgressRepository,
    private val sessionRepository: SessionRepository,
    private val clock: TrainingClock,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val state = MutableStateFlow<SessionUiState>(SessionUiState.Loading)

    /** The current screen state. */
    val uiState: StateFlow<SessionUiState> = state.asStateFlow()

    private lateinit var day: TrainingDay

    init {
        viewModelScope.launch {
            try {
                val today = combine(goalRepository.observeActiveGoal(), progressRepository.observeBests()) { path, bests ->
                    path?.trainingDay(bests)
                }.first()
                if (today?.plan == null) {
                    state.value = SessionUiState.NothingToTrain
                } else {
                    day = today
                    if (!savedState.contains(KEY_STARTED_AT)) savedState[KEY_STARTED_AT] = clock.currentTimeMillis()
                    publish()
                }
            } catch (error: DataUnavailableException) {
                state.value = SessionUiState.Unavailable
            }
        }
    }

    /** Milliseconds on the rest clock, for the screen's countdown. */
    fun elapsedRealtimeMillis(): Long = clock.elapsedRealtimeMillis()

    /**
     * Records the set just done. Starts rest, or saves the session if it was
     * the last set.
     *
     * @param value Reps or seconds done.
     * @param repsInReserve Reps the person felt they had left, or null for holds.
     */
    fun logSet(value: Int, repsInReserve: Int?) {
        val active = state.value as? SessionUiState.Active ?: return
        if (active.restEndsAtElapsedMillis != null) return
        savedState[KEY_VALUES] = values() + value.coerceAtLeast(0)
        savedState[KEY_RESERVES] = reserves() + (repsInReserve?.coerceAtLeast(0) ?: NO_RESERVE)
        if (values().size >= active.plan.sets.size) {
            finish()
        } else {
            savedState[KEY_REST_ENDS_AT] = clock.elapsedRealtimeMillis() + active.plan.restSeconds * MILLIS_PER_SECOND
            publish()
        }
    }

    /** Ends rest early, or after it runs out. */
    fun endRest() {
        savedState[KEY_REST_ENDS_AT] = NOT_RESTING
        publish()
    }

    /** Ends the session now. Saves whatever sets were done; with none, there is nothing to save. */
    fun finish() {
        if (values().isEmpty()) {
            state.value = SessionUiState.NothingToTrain
            return
        }
        val active = state.value as? SessionUiState.Active ?: return
        val session = CompletedSession(
            gateId = active.plan.gate.id,
            startedAtEpochMillis = savedState[KEY_STARTED_AT] ?: clock.currentTimeMillis(),
            finishedAtEpochMillis = clock.currentTimeMillis(),
            sets = loggedSets(active.plan.sets.map { it.isBestEffort }),
        )
        viewModelScope.launch {
            try {
                sessionRepository.saveCompletedSession(session)
                state.value = SessionUiState.Finished(active.plan.gate, day.currentBest, session.best)
            } catch (error: DataUnavailableException) {
                state.value = SessionUiState.Unavailable
            }
        }
    }

    private fun publish() {
        val plan = day.plan ?: return
        val done = values().size
        val restEndsAt = savedState.get<Long>(KEY_REST_ENDS_AT)?.takeIf { it != NOT_RESTING }
        state.value = SessionUiState.Active(
            plan = plan,
            setIndex = done.coerceAtMost(plan.sets.lastIndex),
            logged = loggedSets(plan.sets.map { it.isBestEffort }),
            restEndsAtElapsedMillis = restEndsAt,
        )
    }

    private fun loggedSets(bestEffortFlags: List<Boolean>): List<LoggedSet> {
        val reserves = reserves()
        return values().mapIndexed { index, value ->
            LoggedSet(
                value = value,
                repsInReserve = reserves.getOrNull(index)?.takeIf { it != NO_RESERVE },
                isBestEffort = bestEffortFlags.getOrElse(index) { false },
            )
        }
    }

    private fun values(): IntArray = savedState[KEY_VALUES] ?: IntArray(0)

    private fun reserves(): IntArray = savedState[KEY_RESERVES] ?: IntArray(0)
}
