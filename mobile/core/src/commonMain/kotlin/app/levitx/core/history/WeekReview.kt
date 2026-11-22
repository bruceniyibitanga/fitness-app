package app.levitx.core.history

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.BodyArea
import app.levitx.core.coach.CoachTuning
import app.levitx.core.coach.Effort
import app.levitx.core.coach.MovementPattern
import app.levitx.core.coach.VolumeTargets
import app.levitx.core.session.LoggedSet
import app.levitx.core.skills.SkillCatalogue
import kotlin.math.roundToInt

/**
 * A look back at one week of strength training, for the week review.
 *
 * Everything here is a fact about the week, or about the block as it stands.
 * Nothing predicts what the coach will plan, because the coach decides that
 * session by session.
 *
 * @property sessionsDone Strength workouts finished in the week. Cardio and mobility don't count.
 * @property sessionsPlanned Sessions a week the person said they'd train.
 * @property newBests Exercises where a set beat every earlier set of it (see [WeekReviews.newBests]).
 * @property gatesPassed Skill gates first reached this week.
 * @property hardSets Hard sets per movement against its weekly target.
 * @property notes What's worth saying about the week and the block, most useful first.
 * @property noticed The one thing the coach noticed and acted on, or null when nothing stood out.
 */
public data class WeekReview(
    val sessionsDone: Int,
    val sessionsPlanned: Int,
    val newBests: Int,
    val gatesPassed: Int,
    val hardSets: List<PatternSets>,
    val notes: List<WeekNote>,
    val noticed: Noticed? = null,
) {
    /** Every hard set in the week. */
    public val totalHardSets: Int
        get() = hardSets.sumOf { it.done }.roundToInt()
}

/**
 * Hard sets of one movement in a week.
 *
 * @property pattern The movement.
 * @property done Hard sets done, counting accessories as part sets as the coach does.
 * @property target The weekly target for the person's plan.
 */
public data class PatternSets(val pattern: MovementPattern, val done: Double, val target: Double)

/** Something worth saying at the end of a week. */
public sealed interface WeekNote {
    /**
     * A movement fell short of its weekly target.
     *
     * @property pattern The movement.
     * @property sets How many hard sets short, rounded, and at least one.
     */
    public data class ShortOf(val pattern: MovementPattern, val sets: Int) : WeekNote

    /**
     * The block's lighter week is due soon.
     *
     * @property sessions Sessions left before it, if no early signs bring it forward.
     */
    public data class LighterWeekDue(val sessions: Int) : WeekNote

    /**
     * The lighter week is under way.
     *
     * @property sessionsLeft Lighter sessions left before a new block starts.
     */
    public data class LighterWeekNow(val sessionsLeft: Int) : WeekNote
}

/**
 * The one thing the coach noticed in a week and what it did about it, so the
 * adapting shows over a week and not only session by session. Every kind is
 * a fact from the model, never a guess about how the person feels.
 */
public sealed interface Noticed {
    /**
     * An area is on light work or paused, and the plan works around it.
     *
     * @property area The first such area, in [BodyArea] order.
     */
    public data class WorkingAround(val area: BodyArea) : Noticed

    /** They took the easier day the coach offered during the week. */
    public data object TookEasierDay : Noticed

    /**
     * An exercise's last two sessions this week both hit the target at Easy,
     * so its target went up twice as fast (see the coach's progression).
     *
     * @property exerciseId The exercise.
     */
    public data class FeltEasy(val exerciseId: String) : Noticed

    /**
     * How many of the exercises trained hard this week hit every target.
     *
     * @property hit Exercises where every session this week hit the target.
     * @property trained Exercises trained hard this week.
     */
    public data class HitTargets(val hit: Int, val trained: Int) : Noticed
}

/** Builds week reviews and says when one is due. */
public object WeekReviews {
    /** The most "short of target" notes, so the review stays short. */
    private const val MAX_SHORT_NOTES = 2

    /** Sessions in a row at Easy that make a target climb faster. */
    private const val EASY_TWICE = 2

    /**
     * The review of the week from [fromEpochMillis] up to [toEpochMillis].
     *
     * @param workouts Every stored workout, in any order.
     */
    public fun of(
        model: AthleteModel,
        workouts: List<WorkoutRecord>,
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): WeekReview {
        val week = strengthWorkouts(workouts).filter { it.finishedAtEpochMillis in fromEpochMillis until toEpochMillis }
        val targets = VolumeTargets.weekly(model.profile)
        val hardSets = MovementPattern.strength
            .map { PatternSets(it, model.hardSets(it, fromEpochMillis, toEpochMillis), targets[it] ?: 0.0) }
            .filter { it.done > 0.0 || it.target > 0.0 }
        return WeekReview(
            sessionsDone = week.size,
            sessionsPlanned = model.profile.sessionsPerWeek,
            newBests = newBests(workouts, fromEpochMillis, toEpochMillis),
            gatesPassed = gatesPassed(model, workouts, fromEpochMillis, toEpochMillis),
            hardSets = hardSets,
            notes = shortNotes(hardSets) + listOfNotNull(blockNote(model)),
            noticed = noticed(model, fromEpochMillis, toEpochMillis),
        )
    }

    /**
     * What stood out in the week from [fromEpochMillis] up to [toEpochMillis],
     * most important first: an area being worked around, an easier day taken,
     * an exercise that felt easy twice running, then how many hit their
     * targets. Null when nothing was trained hard and nothing else applies.
     */
    public fun noticed(model: AthleteModel, fromEpochMillis: Long, toEpochMillis: Long): Noticed? {
        val sore = BodyArea.entries.firstOrNull { it in model.pain }
        val eased = model.lastEasedAtEpochMillis?.let { it in fromEpochMillis until toEpochMillis } == true
        val weekMarks = model.exercises
            .mapValues { (_, state) -> state.marks.filter { it.atEpochMillis in fromEpochMillis until toEpochMillis } }
            .filterValues { it.isNotEmpty() }
        val easy = weekMarks.entries.firstOrNull { (_, marks) ->
            marks.size >= EASY_TWICE && marks.takeLast(EASY_TWICE).all { it.hitTarget && it.effort == Effort.EASY }
        }
        return when {
            sore != null -> Noticed.WorkingAround(sore)
            eased -> Noticed.TookEasierDay
            easy != null -> Noticed.FeltEasy(easy.key)
            weekMarks.isEmpty() -> null
            else -> Noticed.HitTargets(weekMarks.count { (_, marks) -> marks.all { it.hitTarget } }, weekMarks.size)
        }
    }

    /**
     * True when the review of the week before [thisWeekFromEpochMillis] hasn't
     * been seen and that week had hard strength work to look back on. Uses
     * the coach's own log of hard sets, so it needs no workout history.
     *
     * @param lastWeekFromEpochMillis When that week started.
     */
    public fun isDue(model: AthleteModel, lastWeekFromEpochMillis: Long, thisWeekFromEpochMillis: Long): Boolean {
        val seen = model.weekReviewedAtEpochMillis?.let { it >= thisWeekFromEpochMillis } == true
        val trained = MovementPattern.strength.any {
            model.hardSets(it, lastWeekFromEpochMillis, thisWeekFromEpochMillis) > 0.0
        }
        return !seen && trained
    }

    /**
     * Exercises with a set in the week that no earlier set of the same
     * exercise matched: none had at least as much load and at least as many
     * reps or seconds. Load is added weight less band help, so more reps with
     * less weight isn't a best. An exercise done for the first time this
     * week has nothing to beat, so it doesn't count.
     */
    public fun newBests(workouts: List<WorkoutRecord>, fromEpochMillis: Long, toEpochMillis: Long): Int {
        val oldestFirst = strengthWorkouts(workouts).sortedBy { it.finishedAtEpochMillis }
        val before = oldestFirst.filter { it.finishedAtEpochMillis < fromEpochMillis }
        val during = oldestFirst.filter { it.finishedAtEpochMillis in fromEpochMillis until toEpochMillis }
        val earlier = before.flatMap {
            it.exercises
        }.groupBy({ it.exerciseId }, { it.sets }).mapValues { it.value.flatten() }
        return during.flatMap { it.exercises }
            .groupBy({ it.exerciseId }, { it.sets })
            .count { (id, sets) ->
                val past = earlier[id].orEmpty()
                past.isNotEmpty() && sets.flatten().any { set -> past.none { it.matches(set) } }
            }
    }

    private fun gatesPassed(model: AthleteModel, workouts: List<WorkoutRecord>, from: Long, to: Long): Int {
        val passed = SkillCatalogue.all.flatMap { it.gates }.filter { (model.gateBests[it.id] ?: 0) >= it.target }
        return Progress.passedGates(workouts, passed).count {
            it.atEpochMillis?.let { at -> at in from until to } ==
                true
        }
    }

    private fun shortNotes(hardSets: List<PatternSets>): List<WeekNote> = hardSets
        .map { it.pattern to (it.target - it.done).roundToInt() }
        .filter { (_, short) -> short >= 1 }
        .sortedByDescending { (_, short) -> short }
        .take(MAX_SHORT_NOTES)
        .map { (pattern, short) -> WeekNote.ShortOf(pattern, short) }

    private fun blockNote(model: AthleteModel): WeekNote? {
        val block = model.block
        val perWeek = model.profile.sessionsPerWeek
        val left = CoachTuning.BLOCK_WEEKS * perWeek - block.sessionsDone
        return when {
            block.isLighterWeek -> WeekNote.LighterWeekNow(block.lighterSessionsLeft)
            left in 1..perWeek -> WeekNote.LighterWeekDue(left)
            else -> null
        }
    }

    /** Workouts with at least one strength exercise. */
    private fun strengthWorkouts(workouts: List<WorkoutRecord>): List<WorkoutRecord> =
        workouts.filter { workout -> workout.exercises.any { it.exercise?.pattern?.isStrength == true } }

    /** True when this earlier set was at least as heavy and at least as long or as many reps as [other]. */
    private fun LoggedSet.matches(other: LoggedSet): Boolean = load >= other.load && value >= other.value

    private val LoggedSet.load: Double
        get() = addedKg - assistanceKg
}
