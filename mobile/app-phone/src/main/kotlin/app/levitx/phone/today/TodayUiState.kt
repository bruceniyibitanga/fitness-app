package app.levitx.phone.today

import app.levitx.core.coach.Recommendation

/** What the Today tab shows. */
sealed interface TodayUiState {
    /** The coach's state hasn't loaded yet. */
    data object Loading : TodayUiState

    /** The data couldn't be read on this device. Nothing was changed. */
    data object Unavailable : TodayUiState

    /**
     * The coach's plan for the next session.
     *
     * @property nowEpochMillis When the plan was worked out, for the date heading.
     * @property recommendation The session, why, and any offer to ease off.
     * @property bodyweightKg Bodyweight for kilograms per rep, or null when none is recorded.
     * @property hasWorkoutInProgress True when a workout was started and not finished.
     * @property sessionsLastWeek Sessions finished in the last seven days.
     * @property sessionsPerWeek Sessions a week the person said they'd train.
     */
    data class Ready(
        val nowEpochMillis: Long,
        val recommendation: Recommendation,
        val bodyweightKg: Double?,
        val hasWorkoutInProgress: Boolean,
        val sessionsLastWeek: Int = 0,
        val sessionsPerWeek: Int = 0,
    ) : TodayUiState
}
