package app.levitx.phone.progress

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.CoachTuning
import app.levitx.core.coach.MovementPattern
import app.levitx.core.coach.SkillTraining
import app.levitx.core.coach.VolumeTargets
import app.levitx.core.coach.currentGate
import app.levitx.core.history.LoadPoint
import app.levitx.core.history.PassedGate
import app.levitx.core.history.Progress
import app.levitx.core.history.WorkoutRecord
import app.levitx.core.skills.GateMeasure
import app.levitx.core.skills.SkillCatalogue

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
private const val MILLIS_PER_HOUR = 60L * 60 * 1000

private const val FOUR_WEEKS_DAYS = 28
private const val THIRTEEN_WEEKS_DAYS = 91

/** Weeks of workouts in the sessions grid. */
internal const val WEEKS_SHOWN = 12

/**
 * How far back the progress screen looks.
 *
 * @property days Days back, or null for everything.
 */
enum class ProgressRange(val days: Int?) {
    /** The last four weeks. */
    FOUR_WEEKS(FOUR_WEEKS_DAYS),

    /** The last three months: thirteen weeks. */
    THREE_MONTHS(THIRTEEN_WEEKS_DAYS),

    /** Everything. */
    ALL(null),
}

/**
 * The movement the progress screen leads with, and its heaviest rep each workout.
 *
 * @property pattern The movement.
 * @property points Oldest first. At least one.
 */
data class Headline(val pattern: MovementPattern, val points: List<LoadPoint>) {
    /** Kilograms per rep in the latest workout. */
    val nowKg: Double
        get() = points.last().kgPerRep

    /** The change since the first workout in range. */
    val changeKg: Double
        get() = points.last().kgPerRep - points.first().kgPerRep
}

/**
 * One movement in "Every movement".
 *
 * @property pattern The movement.
 * @property exerciseId The exercise last done for it.
 * @property exerciseName Its name.
 * @property measure Reps or seconds.
 * @property best The best set of that last time, for exercises without kilograms.
 * @property kgNow Kilograms per rep last time, or null with no figure.
 * @property changeKg The change in range, or null with fewer than two figures.
 */
data class MoveRow(
    val pattern: MovementPattern,
    val exerciseId: String,
    val exerciseName: String,
    val measure: GateMeasure,
    val best: Int,
    val kgNow: Double?,
    val changeKg: Double?,
)

/**
 * Hard sets of one movement in the coach's week, against its plan.
 *
 * @property pattern The movement.
 * @property done Hard sets done, with indirect work counted as a share, as the coach counts them.
 * @property planned The coach's weekly target for it.
 */
data class WeekSets(val pattern: MovementPattern, val done: Double, val planned: Double)

/** The progress screen. */
sealed interface ProgressUiState {
    /** Reading the history. */
    data object Loading : ProgressUiState

    /** The data can't be read on this device right now. */
    data object Unavailable : ProgressUiState

    /**
     * Ready to show.
     *
     * @property range How far back it looks.
     * @property headline The lead movement's load over time, or null with no kilogram figures in range.
     * @property moves Every movement trained in range.
     * @property weekSets Hard sets this week per movement the plan trains.
     * @property weeks Workouts in each of the last [WEEKS_SHOWN] weeks, oldest first.
     * @property gates Gates passed, oldest first.
     * @property workoutCount Every workout stored, in any range.
     */
    data class Ready(
        val range: ProgressRange,
        val headline: Headline?,
        val moves: List<MoveRow>,
        val weekSets: List<WeekSets>,
        val weeks: List<Int>,
        val gates: List<PassedGate>,
        val workoutCount: Int,
    ) : ProgressUiState
}

/**
 * The progress screen for [workouts] and [model], looking back over [range]
 * from [nowEpochMillis].
 */
fun progressState(
    workouts: List<WorkoutRecord>,
    model: AthleteModel,
    range: ProgressRange,
    nowEpochMillis: Long,
): ProgressUiState.Ready {
    val from = range.days?.let { nowEpochMillis - it * MILLIS_PER_DAY } ?: Long.MIN_VALUE
    val inRange = workouts.filter { it.finishedAtEpochMillis >= from }
    val series = MovementPattern.entries.associateWith { Progress.loadSeries(inRange, it, from) }
    return ProgressUiState.Ready(
        range = range,
        headline = headline(model, series),
        moves = MovementPattern.entries.mapNotNull { moveRow(it, inRange, series.getValue(it)) },
        weekSets = weekSets(model, nowEpochMillis),
        weeks = Progress.workoutsPerWeek(workouts, WEEKS_SHOWN, nowEpochMillis),
        gates = Progress.passedGates(workouts, passedGates(model)),
        workoutCount = workouts.size,
    )
}

/** The goal's movement if it has figures, otherwise the movement with the most. */
private fun headline(model: AthleteModel, series: Map<MovementPattern, List<LoadPoint>>): Headline? {
    val goal = model.currentGate()?.let(SkillTraining::exerciseFor)?.pattern
    val pattern = goal?.takeIf { series[it].orEmpty().isNotEmpty() }
        ?: series.filterValues { it.isNotEmpty() }.maxByOrNull { it.value.size }?.key
    return pattern?.let { Headline(it, series.getValue(it)) }
}

private fun moveRow(pattern: MovementPattern, workouts: List<WorkoutRecord>, points: List<LoadPoint>): MoveRow? {
    val latest = workouts
        .sortedByDescending { it.finishedAtEpochMillis }
        .firstNotNullOfOrNull { workout -> workout.exercises.firstOrNull { it.exercise?.pattern == pattern } }
    val exercise = latest?.exercise ?: return null
    return MoveRow(
        pattern = pattern,
        exerciseId = exercise.id,
        exerciseName = exercise.name,
        measure = exercise.measure,
        best = latest.bestSet?.value ?: 0,
        kgNow = points.lastOrNull()?.kgPerRep,
        changeKg = if (points.size > 1) points.last().kgPerRep - points.first().kgPerRep else null,
    )
}

private fun weekSets(model: AthleteModel, nowEpochMillis: Long): List<WeekSets> {
    val weekStart = nowEpochMillis - CoachTuning.WEEK_WINDOW_HOURS * MILLIS_PER_HOUR
    val planned = VolumeTargets.weekly(model.profile)
    return MovementPattern.entries
        .map { WeekSets(it, model.hardSets(it, weekStart, nowEpochMillis), planned[it] ?: 0.0) }
        .filter { it.done > 0.0 || it.planned > 0.0 }
}

private fun passedGates(model: AthleteModel) = SkillCatalogue.all
    .flatMap { it.gates }
    .filter { (model.gateBests[it.id] ?: 0) >= it.target }
