package app.levitx.wear.session

import app.levitx.core.skills.GateMeasure

/** What the watch session screen shows. */
sealed interface SessionUiState {
    /** The session is being loaded, planned or saved. */
    data object Loading : SessionUiState

    /** Data couldn't be unlocked. */
    data object Unavailable : SessionUiState

    /** There is no session in progress. The screen closes itself. */
    data object NoSession : SessionUiState

    /**
     * A session is ready, and the person hasn't yet been asked whether the
     * watch may read their heart rate. Asked once, at the moment it is needed.
     */
    data object AskTracking : SessionUiState

    /**
     * A set to do now.
     *
     * @property setNumber 1-based number of this set.
     * @property setCount Sets in the session.
     * @property measure Whether the set counts reps or seconds.
     * @property target Reps or seconds to aim for.
     * @property isBestEffort True for the last, all-out set.
     * @property repsInReserve Reps to leave in the tank on work sets.
     * @property canSaveEarly True once a set is logged, so ending now saves something.
     * @property heartRate Heart rate in beats per minute, or null when it isn't being read.
     */
    data class Working(
        val setNumber: Int,
        val setCount: Int,
        val measure: GateMeasure,
        val target: Int,
        val isBestEffort: Boolean,
        val repsInReserve: Int,
        val canSaveEarly: Boolean,
        val heartRate: Int? = null,
    ) : SessionUiState

    /**
     * Resting before the next set.
     *
     * @property restEndsAtElapsedMillis When rest ends, on the device's elapsed (since boot) clock.
     * @property restSeconds The full length of the rest, for the progress ring.
     * @property nextSetNumber 1-based number of the set that follows.
     * @property setCount Sets in the session.
     * @property measure Whether the next set counts reps or seconds.
     * @property nextTarget Reps or seconds to aim for next.
     * @property heartRate Heart rate in beats per minute, or null when it isn't being read.
     * @property buzzOnScreen True when the screen gives the end-of-rest buzz itself. False
     *   while the workout service is running, because the service then gives it.
     */
    data class Resting(
        val restEndsAtElapsedMillis: Long,
        val restSeconds: Int,
        val nextSetNumber: Int,
        val setCount: Int,
        val measure: GateMeasure,
        val nextTarget: Int,
        val heartRate: Int? = null,
        val buzzOnScreen: Boolean = true,
    ) : SessionUiState

    /**
     * The session is saved.
     *
     * @property gateTitle The gate trained.
     * @property measure Whether the gate counts reps or seconds.
     * @property sessionBest Best set that counts toward the gate.
     * @property isNewBest True when this session set a new best.
     * @property passedGate True when this session passed the gate.
     * @property usedBand True if any set was band-assisted.
     */
    data class Finished(
        val gateTitle: String,
        val measure: GateMeasure,
        val sessionBest: Int,
        val isNewBest: Boolean,
        val passedGate: Boolean,
        val usedBand: Boolean,
    ) : SessionUiState
}
