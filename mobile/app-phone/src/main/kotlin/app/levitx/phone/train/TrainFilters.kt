package app.levitx.phone.train

import app.levitx.core.coach.Equipment
import app.levitx.core.coach.SkillTraining
import app.levitx.core.coach.WorkoutFocus
import app.levitx.core.intervals.IntervalCatalogue
import app.levitx.core.intervals.IntervalKind
import app.levitx.core.intervals.IntervalWorkout
import app.levitx.core.skills.SkillLevel

/**
 * A chip in the Train tab's top row: everything, one part of the body, or
 * cardio or mobility.
 *
 * @property focus The single workouts it narrows to, or null.
 * @property kind The cardio or mobility workouts it narrows to, or null.
 */
enum class TrainChip(val focus: WorkoutFocus? = null, val kind: IntervalKind? = null) {
    /** Everything. */
    ALL,

    /** Full body workouts. */
    FULL_BODY(focus = WorkoutFocus.FULL_BODY),

    /** Upper body workouts. */
    UPPER(focus = WorkoutFocus.UPPER),

    /** Pull workouts. */
    PULL(focus = WorkoutFocus.PULL),

    /** Push workouts. */
    PUSH(focus = WorkoutFocus.PUSH),

    /** Leg workouts. */
    LEGS(focus = WorkoutFocus.LEGS),

    /** Core workouts. */
    CORE(focus = WorkoutFocus.CORE_ONLY),

    /** Cardio workouts only. */
    CARDIO(kind = IntervalKind.CARDIO),

    /** Mobility workouts only. */
    MOBILITY(kind = IntervalKind.MOBILITY),
}

/**
 * A filter under the chips. Any number can be on at once, except that
 * [NO_KIT] and [MY_KIT] replace each other.
 */
enum class TrainFilter {
    /** Needs no kit at all. */
    NO_KIT,

    /** Needs only kit the person has. */
    MY_KIT,

    /** Takes less than [SHORT_MINUTES] minutes. */
    SHORT,

    /** Nothing jumps. */
    QUIET,

    /** Made for someone new to training. */
    BEGINNER,
}

/** "Under 20 min" means fewer than this many minutes. */
const val SHORT_MINUTES = 20

/** [this] with [filter] switched on or off. Turning one kit filter on turns the other off. */
fun Set<TrainFilter>.toggled(filter: TrainFilter): Set<TrainFilter> = when {
    filter in this -> this - filter
    filter == TrainFilter.NO_KIT -> this - TrainFilter.MY_KIT + filter
    filter == TrainFilter.MY_KIT -> this - TrainFilter.NO_KIT + filter
    else -> this + filter
}

/**
 * What the Train tab lists after the chip and filters.
 *
 * @property programs Skill programs.
 * @property workouts Single workouts built by the coach.
 * @property intervals Cardio and mobility workouts.
 */
data class TrainLists(
    val programs: List<ProgramSummary>,
    val workouts: List<WorkoutSummary>,
    val intervals: List<IntervalWorkout>,
) {
    /** True when nothing at all is left to show. */
    val isEmpty: Boolean
        get() = programs.isEmpty() && workouts.isEmpty() && intervals.isEmpty()
}

/**
 * Narrows [ready] to [chip] and [filters].
 *
 * Programs show under All and the body chips. They last weeks, so the
 * time filter hides them; none of them jumps, so Quiet keeps them all.
 * Single workouts are built from the person's own kit and level, so My kit
 * and Beginner keep them all; the other filters look at what the coach
 * picked today, and hide a workout with nothing safe to do.
 */
fun trainLists(ready: TrainUiState.Ready, chip: TrainChip, filters: Set<TrainFilter>): TrainLists {
    val strength = chip.kind == null
    return TrainLists(
        programs = if (strength) ready.programs.filter { it.fits(filters, ready.owned) } else emptyList(),
        workouts = if (strength) {
            ready.workouts.filter { (chip.focus == null || it.focus == chip.focus) && it.fits(filters) }
        } else {
            emptyList()
        },
        intervals = IntervalCatalogue.all.filter { workout ->
            (chip == TrainChip.ALL || workout.kind == chip.kind) && workout.fits(filters, ready.owned)
        },
    )
}

private fun ProgramSummary.fits(filters: Set<TrainFilter>, owned: Set<Equipment>): Boolean {
    val kit = SkillTraining.kitFor(path)
    return filters.all { filter ->
        when (filter) {
            TrainFilter.NO_KIT -> kit.isEmpty()
            TrainFilter.MY_KIT -> owned.containsAll(kit)
            TrainFilter.SHORT -> false
            TrainFilter.QUIET -> true
            TrainFilter.BEGINNER -> path.level == SkillLevel.BEGINNER
        }
    }
}

private fun WorkoutSummary.fits(filters: Set<TrainFilter>): Boolean {
    val plans = session?.plans.orEmpty()
    return filters.all { filter ->
        when (filter) {
            TrainFilter.MY_KIT, TrainFilter.BEGINNER -> true
            TrainFilter.NO_KIT -> plans.isNotEmpty() && plans.all { it.exercise.equipment.isEmpty() }
            TrainFilter.SHORT -> plans.isNotEmpty() && checkNotNull(session).estimatedMinutes < SHORT_MINUTES
            TrainFilter.QUIET -> plans.isNotEmpty() && plans.none { it.exercise.isJumping }
        }
    }
}

private fun IntervalWorkout.fits(filters: Set<TrainFilter>, owned: Set<Equipment>): Boolean = filters.all { filter ->
    when (filter) {
        TrainFilter.NO_KIT -> equipment.isEmpty()
        TrainFilter.MY_KIT -> owned.containsAll(equipment)
        TrainFilter.SHORT -> minutes < SHORT_MINUTES
        TrainFilter.QUIET -> isQuiet
        TrainFilter.BEGINNER -> forBeginners
    }
}
