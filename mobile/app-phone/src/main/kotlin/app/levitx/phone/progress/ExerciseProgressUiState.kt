package app.levitx.phone.progress

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.ExerciseLookup
import app.levitx.core.history.ExerciseVisit
import app.levitx.core.history.Progress
import app.levitx.core.history.WorkoutRecord
import app.levitx.core.skills.GateMeasure

/** Workouts in the best-set chart. */
internal const val BARS_SHOWN = 12

/** Workouts in "Recent sessions". */
internal const val RECENT_SHOWN = 4

/**
 * A figure and when it was set.
 *
 * @property value The figure.
 * @property atEpochMillis When the workout that set it finished.
 */
data class Dated<T>(val value: T, val atEpochMillis: Long)

/**
 * An exercise's records.
 *
 * @property bestSet The most reps or seconds in one set.
 * @property heaviestKg The most kilograms per rep, or null with no figure.
 * @property mostInOneGo The most reps or seconds in one workout, all sets added up.
 */
data class Records(val bestSet: Dated<Int>, val heaviestKg: Dated<Double>?, val mostInOneGo: Dated<Int>)

/** One exercise's progress. */
sealed interface ExerciseProgressUiState {
    /** Reading the history. */
    data object Loading : ExerciseProgressUiState

    /** The data can't be read on this device right now. */
    data object Unavailable : ExerciseProgressUiState

    /** No such exercise, or never done. */
    data object NotFound : ExerciseProgressUiState

    /**
     * Ready to show.
     *
     * @property exerciseId The exercise.
     * @property name Its name.
     * @property measure Reps or seconds.
     * @property bars The best set of each of the last [BARS_SHOWN] workouts, oldest first.
     * @property kgPerRep The heaviest rep last time, or null with no figure.
     * @property nextTarget The coach's target for next time, or null when it has none.
     * @property workouts Workouts it was done in.
     * @property records The records.
     * @property recent The last [RECENT_SHOWN] times, newest first.
     */
    data class Ready(
        val exerciseId: String,
        val name: String,
        val measure: GateMeasure,
        val bars: List<Dated<Int>>,
        val kgPerRep: Double?,
        val nextTarget: Int?,
        val workouts: Int,
        val records: Records,
        val recent: List<ExerciseVisit>,
    ) : ExerciseProgressUiState
}

/** [exerciseId]'s progress from [workouts], with the coach's next target from [model]. */
fun exerciseProgressState(
    exerciseId: String,
    workouts: List<WorkoutRecord>,
    model: AthleteModel,
): ExerciseProgressUiState {
    val exercise = ExerciseLookup.byId(exerciseId)
    val visits = Progress.visits(workouts, exerciseId).filter { it.record.sets.isNotEmpty() }
    if (exercise == null || visits.isEmpty()) return ExerciseProgressUiState.NotFound
    return ExerciseProgressUiState.Ready(
        exerciseId = exerciseId,
        name = exercise.name,
        measure = exercise.measure,
        bars = visits.take(BARS_SHOWN).reversed().map { Dated(it.record.bestSet?.value ?: 0, it.at) },
        kgPerRep = visits.first().record.topKgPerRep,
        nextTarget = model.exercises[exerciseId]?.target,
        workouts = visits.size,
        records = records(visits),
        recent = visits.take(RECENT_SHOWN),
    )
}

private fun records(visits: List<ExerciseVisit>): Records {
    // Oldest first, so a tie goes to the first time it was reached.
    val oldestFirst = visits.reversed()
    val best = oldestFirst.maxBy { it.record.bestSet?.value ?: 0 }
    val heaviest = oldestFirst.filter { it.record.topKgPerRep != null }.maxByOrNull { it.record.topKgPerRep ?: 0.0 }
    val most = oldestFirst.maxBy { visit -> visit.record.sets.sumOf { it.value } }
    return Records(
        bestSet = Dated(best.record.bestSet?.value ?: 0, best.at),
        heaviestKg = heaviest?.let { Dated(checkNotNull(it.record.topKgPerRep), it.at) },
        mostInOneGo = Dated(most.record.sets.sumOf { it.value }, most.at),
    )
}

private val ExerciseVisit.at: Long
    get() = workout.finishedAtEpochMillis

/**
 * Where an exercise's progress can go.
 *
 * @property onBack Back to Progress.
 * @property onOpenHowTo The exercise's how-to, by id.
 * @property onOpenWorkout A past workout, by id.
 */
data class ExerciseProgressNavigation(
    val onBack: () -> Unit,
    val onOpenHowTo: (String) -> Unit,
    val onOpenWorkout: (String) -> Unit,
)
