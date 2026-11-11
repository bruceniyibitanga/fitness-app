package app.levitx.wear.home

/** What the watch home screen shows, in one immutable value. */
sealed interface HomeUiState {
    /** Nothing has loaded yet. */
    data object Loading : HomeUiState

    /** The person's data couldn't be unlocked on this device. Nothing was changed. */
    data object Unavailable : HomeUiState

    /**
     * The next session, short enough to read in a glance.
     *
     * @property goalName The goal, for example "Muscle-up", or null for general fitness.
     * @property next The next session the phone's coach planned, or null when there is none on the watch.
     * @property isSessionRunning True when a workout is in progress and can be resumed.
     * @property load The load for the next session's skill work, or null when there is none to choose.
     * @property waitingToSync Finished workouts the phone hasn't confirmed yet.
     */
    data class Ready(
        val goalName: String? = null,
        val next: NextSummary? = null,
        val isSessionRunning: Boolean = false,
        val load: LoadSummary? = null,
        val waitingToSync: Int = 0,
    ) : HomeUiState
}

/**
 * The load chosen for the next session's skill work.
 *
 * @property addedKg Vest or belt weight. 0 for none.
 * @property bandName The chosen band's name, or null for no band.
 */
data class LoadSummary(val addedKg: Double, val bandName: String?)
