package app.levitx.phone.home

import app.levitx.core.session.TrainingDay

/** Everything the home screen shows, in one immutable value. */
sealed interface HomeUiState {
    /** Progress hasn't loaded yet. */
    data object Loading : HomeUiState

    /** The person's data couldn't be unlocked on this device. Nothing was changed. */
    data object Unavailable : HomeUiState

    /** No goal chosen yet. */
    data object NoGoal : HomeUiState

    /**
     * The person's place on their path and today's session.
     *
     * @property day Path, progress and today's plan, worked out by `:core`.
     * @property hasBodyweight False until a bodyweight is entered; the screen then asks for one.
     */
    data class Ready(val day: TrainingDay, val hasBodyweight: Boolean = true) : HomeUiState
}
