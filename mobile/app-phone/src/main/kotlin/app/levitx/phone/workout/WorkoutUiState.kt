package app.levitx.phone.workout

import app.levitx.core.coach.ExercisePlan
import app.levitx.core.coach.WorkoutRun
import app.levitx.core.session.PlannedSet
import app.levitx.core.skills.GateMeasure
import app.levitx.data.equipment.Band

/** What the workout screen shows. */
sealed interface WorkoutUiState {
    /** Reading the workout in progress. */
    data object Loading : WorkoutUiState

    /** The data couldn't be read on this device. Nothing was changed. */
    data object Unavailable : WorkoutUiState

    /** There is no workout in progress, or it ended with nothing done. */
    data object NoWorkout : WorkoutUiState

    /**
     * A workout in progress.
     *
     * @property run The workout. Never complete: a complete one is saved straight away.
     * @property bodyweightKg Bodyweight for kilograms per rep, or null when none is recorded.
     * @property bands The person's bands, for assisted sets.
     */
    data class Active(val run: WorkoutRun, val bodyweightKg: Double?, val bands: List<Band>) : WorkoutUiState {
        /** The exercise up next. */
        val plan: ExercisePlan
            get() = checkNotNull(run.current) { "an active workout always has an exercise up next" }

        /** The set up next. */
        val nextSet: PlannedSet
            get() = checkNotNull(run.nextSet) { "an active workout always has a set up next" }

        /** True when the exercise has a load to show and vests or bands make sense for it. */
        val tracksLoad: Boolean
            get() = plan.exercise.loadProfile != null
    }

    /** Saving the finished workout. */
    data object Saving : WorkoutUiState

    /**
     * The workout is saved.
     *
     * @property sets Sets done.
     * @property newBest A new best on a skill gate, if there was one.
     */
    data class Finished(val sets: Int, val newBest: NewBest? = null) : WorkoutUiState
}

/**
 * A new best on a skill gate.
 *
 * @property gateTitle The gate, as the path screen names it.
 * @property measure Reps or seconds.
 * @property value The new best.
 */
data class NewBest(val gateTitle: String, val measure: GateMeasure, val value: Int)
