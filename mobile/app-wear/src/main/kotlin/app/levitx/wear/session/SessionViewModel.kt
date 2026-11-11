package app.levitx.wear.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.session.SessionRun
import app.levitx.data.time.TrainingClock
import app.levitx.wear.workout.WorkoutMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Keep collecting briefly after the screen stops listening, so ambient mode doesn't restart the flow. */
private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Shows the session the [WorkoutController] is running and passes taps to it.
 *
 * The session itself isn't kept here. It belongs to the controller, so it
 * carries on when this screen is swiped away and is still there on return.
 * Heart rate and the off-screen rest alert come from the [WorkoutMonitor].
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val controller: WorkoutController,
    private val clock: TrainingClock,
    private val monitor: WorkoutMonitor,
) : ViewModel() {
    /** The current screen state. */
    val uiState: StateFlow<SessionUiState> =
        combine(controller.state, monitor.heartRate, monitor.isActive, monitor.hasAsked) {
                workout,
                heartRate,
                isTracked,
                hasAsked,
            ->
            // Asked once per app start, when a session is about to run and the
            // answer isn't already yes. Permission can only change through the
            // prompt, which ends in markAsked(), so this is re-checked then.
            val shouldAsk = workout is WorkoutState.Running && !hasAsked && !monitor.canTrack()
            if (shouldAsk) {
                SessionUiState.AskTracking
            } else {
                workout.toUiState(heartRate = heartRate.takeIf { isTracked }, isTracked = isTracked)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SessionUiState.Loading)

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

    /**
     * Makes sure the workout service is running, if the person allowed it.
     * The screen calls this whenever a session is on screen, which is the
     * only time Android lets a workout start.
     */
    fun ensureTracking() = monitor.ensureStarted()

    /** The person answered the heart rate prompt, either way. The session carries on. */
    fun onTrackingAnswered() {
        monitor.markAsked()
        monitor.ensureStarted()
    }
}

private fun WorkoutState.toUiState(heartRate: Int?, isTracked: Boolean): SessionUiState = when (this) {
    WorkoutState.Loading, WorkoutState.Starting -> SessionUiState.Loading

    WorkoutState.Unavailable -> SessionUiState.Unavailable

    is WorkoutState.Idle -> SessionUiState.NoSession

    is WorkoutState.Running -> run.toUiState(heartRate, isTracked)

    is WorkoutState.Finished -> SessionUiState.Finished(
        gateTitle = gate.title,
        measure = gate.measure,
        sessionBest = sessionBest,
        isNewBest = isNewBest,
        passedGate = passedGate,
        usedBand = usedBand,
    )
}

private fun SessionRun.toUiState(heartRate: Int?, isTracked: Boolean): SessionUiState {
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
            heartRate = heartRate,
            buzzOnScreen = !isTracked,
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
            heartRate = heartRate,
        )
    }
}
