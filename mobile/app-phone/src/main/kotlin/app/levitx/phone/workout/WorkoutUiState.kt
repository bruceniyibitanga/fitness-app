package app.levitx.phone.workout

import app.levitx.core.coach.ExercisePlan
import app.levitx.core.coach.WorkoutRun
import app.levitx.core.session.LoggedSet
import app.levitx.core.session.PlannedSet
import app.levitx.core.skills.Gate
import app.levitx.core.skills.GateMeasure
import app.levitx.core.skills.SkillCatalogue
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
     * @property timer The hold or negative timer, while it runs.
     * @property counted What the timer counted once stopped, as the starting
     *   figure for logging. The person can correct it first.
     * @property soundsOn True when timers beep. Off for a quiet gym.
     * @property gateBests The person's best per gate, to tell when a set passes one.
     * @property pending A set that would pass a gate, waiting for the person to
     *   confirm it met the standard.
     * @property passed A gate just passed, to mark before carrying on.
     */
    data class Active(
        val run: WorkoutRun,
        val bodyweightKg: Double?,
        val bands: List<Band>,
        val timer: SetTimer? = null,
        val counted: Int? = null,
        val soundsOn: Boolean = true,
        val gateBests: Map<String, Int> = emptyMap(),
        val pending: PendingSet? = null,
        val passed: GatePass? = null,
    ) : WorkoutUiState {
        /** The exercise up next. */
        val plan: ExercisePlan
            get() = checkNotNull(run.current) { "an active workout always has an exercise up next" }

        /** The set up next. */
        val nextSet: PlannedSet
            get() = checkNotNull(run.nextSet) { "an active workout always has a set up next" }

        /** The gate the exercise up next trains, or null when it isn't skill work. */
        val gate: Gate?
            get() = plan.gateId?.let(SkillCatalogue::gateById)

        /** True when the exercise has a load to show and vests or bands make sense for it. */
        val tracksLoad: Boolean
            get() = plan.exercise.loadProfile != null

        /** True when the set is timed: a hold, or slow negatives. */
        val isTimed: Boolean
            get() = timerKind != null

        /** The timer this set uses, or null for sets counted by hand. */
        val timerKind: TimerKind?
            get() = when {
                plan.exercise.measure == GateMeasure.HOLD_SECONDS -> TimerKind.HOLD
                plan.exercise.lowerSeconds != null -> TimerKind.NEGATIVE
                else -> null
            }

        /** The set just logged, while resting after it, or null. */
        val lastSet: LastSet?
            get() {
                if (!run.isResting) return null
                val index = if (run.logged[run.exerciseIndex].isEmpty()) run.exerciseIndex - 1 else run.exerciseIndex
                val sets = run.logged.getOrNull(index).orEmpty()
                val plan = run.session.plans.getOrNull(index)
                return if (sets.isEmpty() || plan == null) null else LastSet(plan, sets.size, sets.last())
            }

        /** True when the set just logged can be taken back. */
        val canUndo: Boolean
            get() = run.undoLastSet() != run
    }

    /** Saving the finished workout. */
    data object Saving : WorkoutUiState

    /**
     * The workout is saved.
     *
     * @property report What was done, new bests and what the coach changed.
     */
    data class Finished(val report: SessionReport) : WorkoutUiState
}

/**
 * The set just logged.
 *
 * @property plan Its exercise.
 * @property number Which set of the exercise it was, from 1.
 * @property set What was done.
 */
data class LastSet(val plan: ExercisePlan, val number: Int, val set: LoggedSet)

/** What can be done during rest. */
enum class RestAction {
    /** End rest and go to the next set. */
    END,

    /** Rest 15 seconds longer. */
    LONGER,

    /** Rest 15 seconds less. */
    SHORTER,

    /** Take back the set just logged. */
    UNDO,
}

/** What can be done with a hold or negative timer. */
enum class TimerAction {
    /** Start the lead-in, then the timer. */
    START,

    /** Skip the rest of the lead-in and start timing now. */
    NOW,

    /** Stop it, keeping what it counted to log. */
    STOP,

    /** Stop it and forget what it counted. */
    CANCEL,
}
