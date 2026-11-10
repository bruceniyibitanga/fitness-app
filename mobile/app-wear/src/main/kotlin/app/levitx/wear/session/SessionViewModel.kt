package app.levitx.wear.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.session.SessionRun
import app.levitx.data.time.TrainingClock
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Keep collecting briefly after the screen stops listening, so ambient mode doesn't restart the flow. */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Shows the session the [WorkoutController] is running and passes taps to it.
 *
 * The session itself isn't kept here. It belongs to the controller, so it
 * carries on when this screen is swiped away and is still there on return.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val controller: WorkoutController,
    private val clock: TrainingClock,
) : ViewModel() {
    /** The current screen state. */
    val uiState: StateFlow<SessionUiState> = controller.state
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SessionUiState.Loading)

    /** Milliseconds on the rest clock, for the screen's countdown. */
    fun elapsedRealtimeMillis(): Long = clock.elapsedRealtimeMillis()

    /** Records the set just done. */
    fun logSet(value: Int) = controller.logSet(value)

    /** Ends rest early, or after it runs out. */
    fun endRest() = controller.endRest()

    /** Ends the session now, saving any sets done. */
    fun finishEarly() = controller.finishEarly()

    /** Leaves the summary. */
    fun dismissSummary() = controller.dismissSummary()
}

private fun WorkoutState.toUiState(): SessionUiState = when (this) {
    WorkoutState.Loading, WorkoutState.Starting -> SessionUiState.Loading

    WorkoutState.Unavailable -> SessionUiState.Unavailable

    is WorkoutState.Idle -> SessionUiState.NoSession

    is WorkoutState.Running -> run.toUiState()

    is WorkoutState.Finished -> SessionUiState.Finished(
        gateTitle = gate.title,
        measure = gate.measure,
        sessionBest = sessionBest,
        isNewBest = isNewBest,
        passedGate = passedGate,
        usedBand = usedBand,
    )
}

private fun SessionRun.toUiState(): SessionUiState {
    val restEndsAt = restEndsAtElapsedMillis
    // No next set means every set is logged and the session is being saved.
    val next = nextSet ?: return SessionUiState.Loading
    return if (restEndsAt != null) {
        SessionUiState.Resting(
            restEndsAtElapsedMillis = restEndsAt,
            restSeconds = plan.restSeconds,
            nextSetNumber = nextSetIndex + 1,
            setCount = plan.sets.size,
            measure = plan.gate.measure,
            nextTarget = next.target,
        )
    } else {
        SessionUiState.Working(
            setNumber = nextSetIndex + 1,
            setCount = plan.sets.size,
            measure = plan.gate.measure,
            target = next.target,
            isBestEffort = next.isBestEffort,
            repsInReserve = plan.repsInReserve,
            canSaveEarly = logged.isNotEmpty(),
        )
    }
}
