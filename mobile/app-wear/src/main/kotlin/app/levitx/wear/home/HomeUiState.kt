package app.levitx.wear.home

import app.levitx.core.skills.GateMeasure

/** What the watch home screen shows, in one immutable value. */
sealed interface HomeUiState {
    /** Progress hasn't loaded yet. */
    data object Loading : HomeUiState

    /** The person's data couldn't be unlocked on this device. Nothing was changed. */
    data object Unavailable : HomeUiState

    /** No goal on the watch yet. It arrives from the phone. */
    data object NoGoal : HomeUiState

    /**
     * The next gate and today's session, short enough to read in a glance.
     *
     * @property skillName The goal, for example "Muscle-up".
     * @property gateTitle The gate to work on next, or null when the path is complete.
     * @property gateNumber 1-based position of the current gate.
     * @property gateCount Gates on the path.
     * @property today Today's session, or null when the path is complete.
     * @property isSessionRunning True when a session is in progress and can be resumed.
     * @property load The load the next session will use, or null when this gate has no load to choose.
     * @property waitingToSync Finished sessions the phone hasn't confirmed yet.
     */
    data class Ready(
        val skillName: String,
        val gateTitle: String?,
        val gateNumber: Int,
        val gateCount: Int,
        val today: TodaySummary? = null,
        val isSessionRunning: Boolean = false,
        val load: LoadSummary? = null,
        val waitingToSync: Int = 0,
    ) : HomeUiState
}

/**
 * Today's session in one line.
 *
 * @property workSets Work sets before the best-effort set.
 * @property workTarget Reps or seconds per work set.
 * @property measure Whether sets count reps or seconds.
 */
data class TodaySummary(val workSets: Int, val workTarget: Int, val measure: GateMeasure)

/**
 * The load chosen for the next session.
 *
 * @property addedKg Vest or belt weight. 0 for none.
 * @property bandName The chosen band's name, or null for no band.
 */
data class LoadSummary(val addedKg: Double, val bandName: String?)
