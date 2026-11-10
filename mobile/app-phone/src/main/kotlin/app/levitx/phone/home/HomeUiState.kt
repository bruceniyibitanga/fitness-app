package app.levitx.phone.home

import app.levitx.core.skills.Gate

/** Everything the home screen shows, in one immutable value. */
sealed interface HomeUiState {
    /** Progress hasn't loaded yet. */
    data object Loading : HomeUiState

    /** The person's data couldn't be unlocked on this device. Nothing was changed. */
    data object Unavailable : HomeUiState

    /**
     * The person's place on their path.
     *
     * @property skillName The goal, for example "Muscle-up".
     * @property currentGate The gate to work on next, or null when the path is complete.
     * @property gateNumber 1-based position of [currentGate], for display.
     * @property gateCount Gates on the path.
     * @property gateFraction Progress toward [currentGate], from 0 to 1.
     */
    data class Ready(
        val skillName: String,
        val currentGate: Gate?,
        val gateNumber: Int,
        val gateCount: Int,
        val gateFraction: Float,
    ) : HomeUiState
}
