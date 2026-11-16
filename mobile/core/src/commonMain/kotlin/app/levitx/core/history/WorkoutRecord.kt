package app.levitx.core.history

import app.levitx.core.coach.Exercise
import app.levitx.core.coach.ExerciseLookup
import app.levitx.core.load.calculateEffectiveLoad
import app.levitx.core.session.LoggedSet
import app.levitx.core.skills.GateMeasure

private const val MILLIS_PER_MINUTE = 60_000L

/**
 * One exercise in a finished workout, as it was stored.
 *
 * @property exerciseId The exercise's id. Usually in the catalogue, but a
 *   workout from an older version may name one that has since gone.
 * @property sets The sets, in order.
 * @property bodyweightKg Bodyweight at the time, or null if it wasn't known.
 */
public data class ExerciseRecord(val exerciseId: String, val sets: List<LoggedSet>, val bodyweightKg: Double?) {
    /** The exercise, or null when it is no longer in the catalogue. */
    public val exercise: Exercise?
        get() = ExerciseLookup.byId(exerciseId)

    /** The set with the most reps or seconds, or null with no sets. */
    public val bestSet: LoggedSet?
        get() = sets.maxByOrNull { it.value }

    /**
     * Kilograms one rep of [set] moved, or null when that can't be worked out:
     * no bodyweight at the time, an exercise without a load figure, or a hold.
     */
    public fun kgPerRep(set: LoggedSet): Double? {
        val profile = exercise?.takeIf { it.measure == GateMeasure.REPS }?.loadProfile
        val bodyweight = bodyweightKg
        if (profile == null || bodyweight == null) return null
        return calculateEffectiveLoad(bodyweight, profile, set.modifiers).totalKg
    }

    /** The most kilograms per rep of any set, or null when there is no figure. */
    public val topKgPerRep: Double?
        get() = sets.mapNotNull(::kgPerRep).maxOrNull()

    /** Kilograms moved over every rep, or null when there is no figure. */
    public val kgMoved: Double?
        get() = sets.mapNotNull { set -> kgPerRep(set)?.times(set.value) }.takeIf { it.isNotEmpty() }?.sum()
}

/**
 * A finished workout, as it was stored: when, and every exercise done.
 *
 * @property id The workout's id.
 * @property startedAtEpochMillis When it started, in UTC milliseconds.
 * @property finishedAtEpochMillis When it finished, in UTC milliseconds.
 * @property exercises The exercises done, in order.
 */
public data class WorkoutRecord(
    val id: String,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long,
    val exercises: List<ExerciseRecord>,
) {
    /** How long it took, in whole minutes. */
    public val minutes: Int
        get() = ((finishedAtEpochMillis - startedAtEpochMillis) / MILLIS_PER_MINUTE).toInt()

    /** Sets done. */
    public val sets: Int
        get() = exercises.sumOf { it.sets.size }

    /** Kilograms moved, or null when no exercise has a figure. */
    public val kgMoved: Double?
        get() = exercises.mapNotNull { it.kgMoved }.takeIf { it.isNotEmpty() }?.sum()
}
