package app.levitx.core.coach

import app.levitx.core.session.LoggedSet
import app.levitx.core.skills.SkillCatalogue
import kotlin.math.roundToInt

/** Sessions in the simulation start at 6 pm, so same-day spacing is never the thing being tested. */
private const val SESSION_HOUR = 18

/** The start of day [day] of a simulation, at session time, in UTC milliseconds. */
internal fun atDay(day: Int): Long = day * MILLIS_PER_DAY + SESSION_HOUR * MILLIS_PER_HOUR

/**
 * A made-up person for the coach to train.
 *
 * Their real strength is one number per movement pattern, in rungs of that
 * pattern's ladder: at strength 2.0 they can just do the bottom of rung 2's
 * range, at 3.0 the top of it. Skill gates work the same way on [skill]. Hard
 * training nudges the numbers up, time off lets them slip, and what they can
 * do in a set follows from them. It is a deliberately simple response model:
 * the point is to check the coach behaves sensibly, not to model physiology.
 *
 * @property strength Strength per pattern, in rungs.
 * @property skill Strength on the goal's gates, in gates.
 * @property gain Strength gained per hard session of a pattern.
 * @property capacityFactor Scales what they can do on the day. Below 1 for a bad spell.
 */
internal class SimulatedAthlete(
    private val strength: MutableMap<MovementPattern, Double>,
    private var skill: Double,
    private val gain: Double,
    var capacityFactor: Double = 1.0,
) {
    /** Does [session] as well as they can and reports it the way the apps would. */
    fun perform(session: PlannedSession, day: Int, choice: SuggestionChoice? = null): SessionResult {
        val results = session.plans.map { plan ->
            ExerciseResult(plan, plan.sets.map { doSet(plan, it.target, it.isBestEffort) })
        }
        session.plans.filter { it.isHard }.forEach(::adapt)
        val started = atDay(day)
        return SessionResult(results, started, started + session.estimatedMinutes * MILLIS_PER_MINUTE, choice)
    }

    /** Strength slips a little for every day off past a week. */
    fun detrain(daysOff: Int) {
        val lost = (daysOff - DAYS_PER_WEEK).coerceAtLeast(0) * LOSS_PER_DAY_OFF
        strength.keys.toList().forEach { strength[it] = (strength.getValue(it) - lost).coerceAtLeast(0.0) }
        skill = (skill - lost).coerceAtLeast(0.0)
    }

    private fun doSet(plan: ExercisePlan, target: Int, isBestEffort: Boolean): LoggedSet {
        val able = (capacity(plan) * capacityFactor).roundToInt().coerceAtLeast(0)
        val done = if (isBestEffort) able else minOf(target, able)
        return LoggedSet(value = done, repsInReserve = able - done, isBestEffort = isBestEffort)
    }

    /** The most they could do in one set of this exercise today. */
    private fun capacity(plan: ExercisePlan): Double {
        val gateId = plan.gateId
        return if (gateId != null) {
            val path = SkillCatalogue.all.first { path -> path.gates.any { it.id == gateId } }
            val index = path.gates.indexOfFirst { it.id == gateId }
            (skill - index) * path.gates[index].target
        } else {
            val exercise = plan.exercise
            val rung = ExerciseCatalogue.ladder(exercise.pattern).rungs.indexOfFirst { exercise in it.options }
            val above = (strength[exercise.pattern] ?: 0.0) - rung
            exercise.range.first + above * (exercise.range.last - exercise.range.first)
        }
    }

    private fun adapt(plan: ExercisePlan) {
        val pattern = plan.exercise.pattern
        if (plan.gateId != null) skill += gain else strength[pattern] = (strength[pattern] ?: 0.0) + gain
    }

    companion object {
        private const val MILLIS_PER_MINUTE = 60_000L
        private const val LOSS_PER_DAY_OFF = 0.02

        /** Someone with the same strength on every pattern. */
        fun even(strength: Double, skill: Double, gain: Double): SimulatedAthlete =
            SimulatedAthlete(MovementPattern.entries.associateWith { strength }.toMutableMap(), skill, gain)
    }
}

/**
 * What one simulated day of training produced.
 *
 * @property day The day number.
 * @property recommendation What the coach proposed.
 * @property done The session actually done, or null on a rest.
 * @property model The model before the session, which the invariants are checked against.
 */
internal data class SimulatedDay(
    val day: Int,
    val recommendation: Recommendation,
    val done: PlannedSession?,
    val model: AthleteModel,
)

/**
 * Runs [athlete] through the coach on each of [days], taking the coach's
 * suggested choice whenever it offers one. [before] runs first each day, for
 * weigh-ins, pain and bad spells.
 */
internal fun simulate(
    start: AthleteModel,
    athlete: SimulatedAthlete,
    days: List<Int>,
    coach: Coach = Coach(),
    before: (day: Int, model: AthleteModel) -> AthleteModel = { _, model -> model },
): Pair<AthleteModel, List<SimulatedDay>> {
    var model = start
    var lastDay: Int? = null
    val log = days.map { day ->
        lastDay?.let { athlete.detrain(day - it) }
        lastDay = day
        model = before(day, model)
        val asked = model
        val recommendation = coach.recommend(model, atDay(day))
        val suggestion = recommendation.suggestion
        val choice = suggestion?.recommended
        val session = when (choice) {
            SuggestionChoice.REST -> null
            SuggestionChoice.LIGHTER -> suggestion.lighter
            else -> recommendation.session
        }
        model = when (session) {
            null -> coach.recordRest(model, atDay(day))
            else -> coach.record(model, athlete.perform(session, day, choice))
        }
        SimulatedDay(day, recommendation, session, asked)
    }
    return model to log
}

/** Days for [weeks] weeks of training on the given [weekdays] (0 is the first day of each week). */
internal fun trainingDays(weeks: Int, vararg weekdays: Int): List<Int> =
    (0 until weeks).flatMap { week -> weekdays.map { week * DAYS_PER_WEEK + it } }
