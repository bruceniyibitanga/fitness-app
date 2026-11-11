package app.levitx.core.coach

import app.levitx.core.session.LoggedSet
import app.levitx.core.session.PlannedSet

/** Kit that lets every pattern be trained, so tests about other things aren't about kit. */
internal val fullKit: Set<Equipment> = setOf(Equipment.PULL_UP_BAR, Equipment.LOW_BAR)

/** A first pull-up plan with the usual answers. */
internal fun pullUpProfile(
    style: PlanStyle = PlanStyle.ALL_ROUND,
    sessionsPerWeek: Int = 3,
    sessionMinutes: Int = 45,
    equipment: Set<Equipment> = fullKit,
): CoachProfile = CoachProfile("first_pull_up", style, sessionsPerWeek, sessionMinutes, equipment)

/** Someone who has just finished setup and can't do a pull-up yet. */
internal fun newcomer(profile: CoachProfile = pullUpProfile()): AthleteModel =
    AthleteModel.starting(profile, PullUpAnswer.NOT_YET)

/** A catalogue exercise by id. Fails the test if the id is wrong. */
internal fun exercise(id: String): Exercise = checkNotNull(ExerciseCatalogue.byId(id)) { "no exercise $id" }

/** Three hard work sets of [exercise] at [target], optionally ending in a best-effort set. */
internal fun hardPlan(exercise: Exercise, target: Int, bestEffort: Boolean = false): ExercisePlan {
    val work = List(if (bestEffort) 2 else 3) { PlannedSet(target, isBestEffort = false) }
    val sets = if (bestEffort) work + PlannedSet(exercise.range.last, isBestEffort = true) else work
    return ExercisePlan(exercise, sets, restSeconds = 120, repsInReserve = 2, intensity = Intensity.HARD)
}

/** A session of just [plans]. */
internal fun sessionOf(vararg plans: ExercisePlan): PlannedSession = PlannedSession(plans.toList())

/** [plan] done with every set at [value] and rated [effort]. */
internal fun done(plan: ExercisePlan, value: Int, effort: Effort? = Effort.RIGHT): ExerciseResult = ExerciseResult(
    plan,
    plan.sets.map { LoggedSet(value, effort?.repsInReserve, it.isBestEffort) },
)

/** A finished session of [results] on [day]. */
internal fun finished(day: Int, vararg results: ExerciseResult, choice: SuggestionChoice? = null): SessionResult =
    SessionResult(results.toList(), atDay(day), atDay(day) + MILLIS_PER_HOUR, choice)

/** The decision context for [model] on [day]. */
internal fun contextOn(day: Int, model: AthleteModel): CoachContext = CoachContext(model, atDay(day))
