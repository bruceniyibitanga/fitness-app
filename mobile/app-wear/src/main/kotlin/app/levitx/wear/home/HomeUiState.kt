package app.levitx.wear.home

/** What the watch home screen shows, in one immutable value. */
sealed interface HomeUiState {
    /** Progress hasn't loaded yet. */
    data object Loading : HomeUiState

    /**
     * The next gate, short enough to read in a glance.
     *
     * @property skillName The goal, for example "Muscle-up".
     * @property gateTitle The gate to work on next, or null when the path is complete.
     * @property gateNumber 1-based position of the current gate.
     * @property gateCount Gates on the path.
     */
    data class Ready(val skillName: String, val gateTitle: String?, val gateNumber: Int, val gateCount: Int) :
        HomeUiState
}
