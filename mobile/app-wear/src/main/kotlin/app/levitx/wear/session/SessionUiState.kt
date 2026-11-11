package app.levitx.wear.session

import app.levitx.core.skills.GateMeasure

/** What the watch workout screen shows. */
sealed interface SessionUiState {
    /** The workout is being loaded, set up or saved. */
    data object Loading : SessionUiState

    /** Data couldn't be unlocked. */
    data object Unavailable : SessionUiState

    /** There is no workout in progress. The screen closes itself. */
    data object NoSession : SessionUiState

    /**
     * A workout is ready, and the person hasn't yet been asked whether the
     * watch may read their heart rate. Asked once, at the moment it is needed.
     */
    data object AskTracking : SessionUiState

    /**
     * A set to do now.
     *
     * @property exerciseName What to do.
     * @property exerciseNumber 1-based position of the exercise in the workout.
     * @property exerciseCount Exercises in the workout.
     * @property setNumber 1-based number of this set within the exercise.
     * @property setCount Sets of this exercise.
     * @property measure Whether the set counts reps or seconds.
     * @property target Reps or seconds to aim for.
     * @property isPerSide True when the set is done once on each side.
     * @property isBestEffort True for an all-out set.
     * @property repsInReserve Reps to leave in the tank on work sets.
     * @property canSaveEarly True once a set is logged, so ending now saves something.
     * @property heartRate Heart rate in beats per minute, or null when it isn't being read.
     */
    data class Working(
        val exerciseName: String,
        val exerciseNumber: Int,
        val exerciseCount: Int,
        val setNumber: Int,
        val setCount: Int,
        val measure: GateMeasure,
        val target: Int,
        val isPerSide: Boolean,
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
     * @property nextExerciseName The exercise the next set belongs to.
     * @property isNewExercise True when the next set starts a new exercise.
     * @property nextSetNumber 1-based number of the next set within its exercise.
     * @property setCount Sets of that exercise.
     * @property measure Whether the next set counts reps or seconds.
     * @property nextTarget Reps or seconds to aim for next.
     * @property heartRate Heart rate in beats per minute, or null when it isn't being read.
     * @property buzzOnScreen True when the screen gives the end-of-rest buzz itself. False
     *   while the workout service is running, because the service then gives it.
     */
    data class Resting(
        val restEndsAtElapsedMillis: Long,
        val restSeconds: Int,
        val nextExerciseName: String,
        val isNewExercise: Boolean,
        val nextSetNumber: Int,
        val setCount: Int,
        val measure: GateMeasure,
        val nextTarget: Int,
        val heartRate: Int? = null,
        val buzzOnScreen: Boolean = true,
    ) : SessionUiState

    /**
     * The workout is saved.
     *
     * @property sets Sets done.
     * @property newBest The biggest new best on a skill gate, if there was one.
     * @property usedBand True if any set was band-assisted.
     */
    data class Finished(val sets: Int, val newBest: GateBest? = null, val usedBand: Boolean = false) : SessionUiState
}
