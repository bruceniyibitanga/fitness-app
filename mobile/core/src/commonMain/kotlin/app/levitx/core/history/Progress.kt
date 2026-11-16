package app.levitx.core.history

import app.levitx.core.coach.ExerciseCatalogue
import app.levitx.core.coach.MovementPattern
import app.levitx.core.session.LoggedSet
import app.levitx.core.skills.Gate

private const val MILLIS_PER_WEEK = 7L * 24 * 60 * 60 * 1000

/*
 * What the progress screens show, worked out from stored workouts. Nothing
 * here is estimated: every figure comes from sets the person logged, and an
 * exercise without a load figure simply has no kilograms.
 */

/**
 * The heaviest rep of a movement in one workout.
 *
 * @property atEpochMillis When the workout finished.
 * @property kgPerRep Kilograms per rep of the heaviest set.
 * @property exerciseId The exercise it was on.
 * @property isNewRung True when this is the first time on a harder rung of the
 *   movement's ladder than any before it in the series.
 */
public data class LoadPoint(
    val atEpochMillis: Long,
    val kgPerRep: Double,
    val exerciseId: String,
    val isNewRung: Boolean,
)

/**
 * One time an exercise was done.
 *
 * @property workout The workout it was part of.
 * @property record The exercise within it.
 */
public data class ExerciseVisit(val workout: WorkoutRecord, val record: ExerciseRecord)

/**
 * A gate passed, and when.
 *
 * @property gate The gate.
 * @property atEpochMillis When the workout that passed it finished, or null
 *   when the coach has it as passed but no stored workout shows it (from
 *   before history was kept).
 */
public data class PassedGate(val gate: Gate, val atEpochMillis: Long?)

/** Progress figures from a person's stored workouts. */
public object Progress {
    /**
     * The heaviest rep of [pattern] in each workout since [fromEpochMillis],
     * oldest first. Workouts without a load figure for it are left out.
     *
     * @param workouts Every stored workout, in any order.
     */
    public fun loadSeries(
        workouts: List<WorkoutRecord>,
        pattern: MovementPattern,
        fromEpochMillis: Long,
    ): List<LoadPoint> {
        var topRung = -1
        return workouts
            .filter { it.finishedAtEpochMillis >= fromEpochMillis }
            .sortedBy { it.finishedAtEpochMillis }
            .mapNotNull { workout ->
                val heaviest = workout.exercises
                    .filter { it.exercise?.pattern == pattern }
                    .mapNotNull { record -> record.topKgPerRep?.let { record to it } }
                    .maxByOrNull { (_, kg) -> kg }
                    ?: return@mapNotNull null
                val (record, kg) = heaviest
                val rung = rungOf(pattern, record.exerciseId)
                // The first point sets the starting rung; only a later climb is marked.
                val isNewRung = topRung >= 0 && rung > topRung
                topRung = maxOf(topRung, rung)
                LoadPoint(workout.finishedAtEpochMillis, kg, record.exerciseId, isNewRung)
            }
    }

    /** Every time [exerciseId] was done, newest first. */
    public fun visits(workouts: List<WorkoutRecord>, exerciseId: String): List<ExerciseVisit> = workouts
        .sortedByDescending { it.finishedAtEpochMillis }
        .flatMap { workout ->
            workout.exercises.filter { it.exerciseId == exerciseId }.map { ExerciseVisit(workout, it) }
        }

    /**
     * Workouts in each of the last [weeks] weeks, oldest first, counting
     * seven-day windows back from [nowEpochMillis].
     */
    public fun workoutsPerWeek(workouts: List<WorkoutRecord>, weeks: Int, nowEpochMillis: Long): List<Int> =
        (weeks - 1 downTo 0).map { weeksBack ->
            val end = nowEpochMillis - weeksBack * MILLIS_PER_WEEK
            workouts.count { it.finishedAtEpochMillis in (end - MILLIS_PER_WEEK + 1)..end }
        }

    /**
     * The gates in [passed], each with the first stored workout that reached
     * its target with a set that counts toward a gate, oldest first.
     */
    public fun passedGates(workouts: List<WorkoutRecord>, passed: List<Gate>): List<PassedGate> {
        val oldestFirst = workouts.sortedBy { it.finishedAtEpochMillis }
        return passed
            .map { gate ->
                val first = oldestFirst.firstOrNull { workout ->
                    workout.exercises.any { record ->
                        record.exerciseId == gate.id && record.sets.any { it.reaches(gate) }
                    }
                }
                PassedGate(gate, first?.finishedAtEpochMillis)
            }
            .sortedBy { it.atEpochMillis ?: Long.MIN_VALUE }
    }

    /** Where [exerciseId] sits on [pattern]'s ladder, from 0, or -1 when it isn't on it. */
    private fun rungOf(pattern: MovementPattern, exerciseId: String): Int =
        ExerciseCatalogue.ladder(pattern).rungs.indexOfFirst { rung -> rung.options.any { it.id == exerciseId } }

    private fun LoggedSet.reaches(gate: Gate): Boolean = countsTowardGate && value >= gate.target
}
