package app.levitx.phone.workout

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.ExerciseResult
import app.levitx.core.coach.SessionResult
import app.levitx.core.coach.currentExercise
import app.levitx.core.load.calculateEffectiveLoad
import app.levitx.core.session.LoggedSet
import app.levitx.core.skills.GateMeasure
import app.levitx.core.skills.SkillCatalogue
import kotlin.math.roundToInt

private const val MILLIS_PER_MINUTE = 60_000L

/**
 * What the summary at the end of a workout shows, worked out by comparing the
 * coach's model before and after it learned from the session. So "what
 * changes next time" is what the coach actually changed, not a guess.
 *
 * @property sets Sets done.
 * @property minutes How long the session took.
 * @property kgMoved Kilograms moved across every rep that has a load figure,
 *   or null when there is no bodyweight or no such rep.
 * @property bests New bests on gates and exercises.
 * @property exercises Each exercise done, with its sets.
 * @property changes What the coach changed for next time.
 */
data class SessionReport(
    val sets: Int,
    val minutes: Int,
    val kgMoved: Int?,
    val bests: List<ReportBest>,
    val exercises: List<ReportExercise>,
    val changes: List<ReportChange>,
)

/**
 * A new best.
 *
 * @property name The gate or exercise.
 * @property measure Reps or seconds.
 * @property value The new best.
 * @property was The best before, or null for a gate's first.
 * @property passedGate True when this best passed a skill gate.
 */
data class ReportBest(
    val name: String,
    val measure: GateMeasure,
    val value: Int,
    val was: Int?,
    val passedGate: Boolean = false,
)

/**
 * One exercise as it was done.
 *
 * @property name The exercise.
 * @property measure Reps or seconds.
 * @property sets Its sets, in order.
 */
data class ReportExercise(val name: String, val measure: GateMeasure, val sets: List<LoggedSet>)

/** Something the coach changed for next time. */
sealed interface ReportChange {
    /** The exercise. */
    val name: String

    /** The target went from [from] to [to], in reps or seconds as [measure] says. */
    data class Target(override val name: String, val measure: GateMeasure, val from: Int, val to: Int) : ReportChange

    /** The target stayed at [target], in reps or seconds as [measure] says. */
    data class Held(override val name: String, val measure: GateMeasure, val target: Int) : ReportChange

    /** The movement moved up a rung, to [next]. */
    data class MovedUp(override val name: String, val next: String) : ReportChange

    /** The movement moved down a rung, to [next]. */
    data class MovedDown(override val name: String, val next: String) : ReportChange
}

/**
 * The report for [result], from the model [before] the coach learned from it
 * and the model [after].
 *
 * @param bodyweightKg Bodyweight the workout used for load per rep, or null.
 */
internal fun sessionReport(
    before: AthleteModel,
    after: AthleteModel,
    result: SessionResult,
    bodyweightKg: Double?,
): SessionReport {
    val done = result.results.filter { !it.skipped && it.sets.isNotEmpty() }
    return SessionReport(
        sets = done.sumOf { it.sets.size },
        minutes = ((result.finishedAtEpochMillis - result.startedAtEpochMillis) / MILLIS_PER_MINUTE).toInt(),
        kgMoved = bodyweightKg?.let { kgMoved(done, it) },
        bests = gateBests(before, result) + exerciseBests(before, done),
        exercises = done.map { ReportExercise(it.plan.exercise.name, it.plan.exercise.measure, it.sets) },
        changes = done.mapNotNull { change(before, after, it) }.distinctBy { it.name },
    )
}

private fun gateBests(before: AthleteModel, result: SessionResult): List<ReportBest> =
    result.newGateBests(before.gateBests).mapNotNull { (gateId, best) ->
        SkillCatalogue.gateById(gateId)?.let { gate ->
            ReportBest(gate.title, gate.measure, best, before.gateBests[gateId], passedGate = best >= gate.target)
        }
    }

/** Exercises beaten on a previous best. A first time isn't a best: there was nothing to beat. */
private fun exerciseBests(before: AthleteModel, done: List<ExerciseResult>): List<ReportBest> = done
    .filter { SkillCatalogue.gateById(it.plan.exercise.id) == null }
    .mapNotNull { exercise ->
        val was = before.exercises[exercise.plan.exercise.id]?.best ?: 0
        val now = exercise.sets.maxOf { it.value }
        if (was > 0 &&
            now > was
        ) {
            ReportBest(exercise.plan.exercise.name, exercise.plan.exercise.measure, now, was)
        } else {
            null
        }
    }

private fun change(before: AthleteModel, after: AthleteModel, done: ExerciseResult): ReportChange? {
    val exercise = done.plan.exercise
    val pattern = exercise.pattern
    val next = after.currentExercise(pattern)
    val from = before.exercises[exercise.id]?.target
    val to = after.exercises[exercise.id]?.target
    return when {
        after.rung(pattern) > before.rung(pattern) && next != null -> ReportChange.MovedUp(exercise.name, next.name)
        after.rung(pattern) < before.rung(pattern) && next != null -> ReportChange.MovedDown(exercise.name, next.name)
        from == null || to == null -> null
        from == to -> ReportChange.Held(exercise.name, exercise.measure, to)
        else -> ReportChange.Target(exercise.name, exercise.measure, from, to)
    }
}

private fun kgMoved(done: List<ExerciseResult>, bodyweightKg: Double): Int? {
    val loaded = done.filter { it.plan.exercise.loadProfile != null && it.plan.exercise.measure == GateMeasure.REPS }
    if (loaded.isEmpty()) return null
    return loaded.sumOf { exercise ->
        val profile = checkNotNull(exercise.plan.exercise.loadProfile)
        exercise.sets.sumOf { set ->
            set.value * calculateEffectiveLoad(bodyweightKg, profile, set.modifiers).totalKg
        }
    }.roundToInt()
}
