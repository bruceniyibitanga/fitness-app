package app.levitx.core.coach

import app.levitx.core.coach.MovementPattern.CORE
import app.levitx.core.coach.MovementPattern.HINGE
import app.levitx.core.coach.MovementPattern.HORIZONTAL_PULL
import app.levitx.core.coach.MovementPattern.HORIZONTAL_PUSH
import app.levitx.core.coach.MovementPattern.SQUAT
import app.levitx.core.coach.MovementPattern.VERTICAL_PULL
import app.levitx.core.coach.MovementPattern.VERTICAL_PUSH
import app.levitx.core.session.PlannedSet
import app.levitx.core.session.SessionPlanner
import app.levitx.core.skills.GateMeasure

private const val SECONDS_PER_MINUTE = 60

/**
 * The session shapes the coach chooses between, as the order patterns are
 * trained in. The skill work, when there is any, always goes first, and a
 * session is cut from the end when time is short, so order is priority.
 */
internal object SessionTemplates {
    val all: List<List<MovementPattern>> = listOf(
        listOf(VERTICAL_PULL, HORIZONTAL_PUSH, SQUAT, HORIZONTAL_PULL, VERTICAL_PUSH, HINGE, CORE),
        listOf(SQUAT, VERTICAL_PULL, HORIZONTAL_PUSH, HINGE, HORIZONTAL_PULL, VERTICAL_PUSH, CORE),
        listOf(HORIZONTAL_PUSH, VERTICAL_PULL, SQUAT, VERTICAL_PUSH, HORIZONTAL_PULL, HINGE, CORE),
        listOf(VERTICAL_PULL, HORIZONTAL_PUSH, HORIZONTAL_PULL, VERTICAL_PUSH, CORE),
        listOf(VERTICAL_PULL, HORIZONTAL_PULL, SQUAT, HINGE, CORE),
        listOf(HORIZONTAL_PUSH, VERTICAL_PUSH, SQUAT, HINGE, CORE),
        listOf(SQUAT, HINGE, CORE),
    )
}

/**
 * Builds the sessions the coach chooses between: each template with the
 * current skill work in front and without it, all as hard work and fitted to
 * the person's time. It knows nothing about safety. The hard rules decide
 * which of these may be done, and what has to be eased or dropped first.
 */
internal class SessionBuilder(private val templates: List<List<MovementPattern>> = SessionTemplates.all) {
    /** Every candidate session for [context], without duplicates. */
    fun candidates(context: CoachContext): List<PlannedSession> {
        val skill = skillPlan(context)
        return templates
            .flatMap { listOfNotNull(build(it, skill, context), skill?.let { _ -> build(it, null, context) }) }
            .distinct()
    }

    private fun build(template: List<MovementPattern>, skill: ExercisePlan?, context: CoachContext): PlannedSession? {
        val wanted = listOfNotNull(skill) + template.mapNotNull { ladderPlan(it, context) }
        val budget = (context.model.profile.sessionMinutes - CoachTuning.WARM_UP_MINUTES) * SECONDS_PER_MINUTE
        return fit(wanted, budget).takeIf { it.isNotEmpty() }?.let(::PlannedSession)
    }

    /** The session's skill work: the current gate, planned the way gate sessions always are. */
    private fun skillPlan(context: CoachContext): ExercisePlan? {
        val gate = context.currentGate
        val exercise = gate?.let(SkillTraining::exerciseFor)?.takeIf { it.fits(context.model.profile.equipment) }
        return exercise?.let {
            val plan = SessionPlanner.planFor(gate, context.model.gateBests[gate.id] ?: 0)
            // A focused plan does more of the skill each session.
            val isFocused = context.model.profile.style == PlanStyle.FOCUSED
            val extra = plan.sets.take(if (isFocused) CoachTuning.FOCUSED_EXTRA_SKILL_SETS else 0)
            ExercisePlan(it, extra + plan.sets, plan.restSeconds, plan.repsInReserve, Intensity.HARD, gateId = gate.id)
        }
    }

    /**
     * Hard work on the person's current exercise for [pattern]: work sets at
     * their target, with the last set swapped for a best-effort set while the
     * coach is still finding their level.
     *
     * The pattern of the gate being worked on gets nothing from its ladder. The
     * gate is that pattern's training until it is passed, with or without
     * today's skill work, so the two never compete.
     */
    private fun ladderPlan(pattern: MovementPattern, context: CoachContext): ExercisePlan? =
        context.exerciseFor(pattern)?.takeIf { pattern != context.currentGatePattern }?.let { exercise ->
            val state = context.model.exercises[exercise.id] ?: ExerciseState(target = exercise.range.first)
            val work = PlannedSet(state.target, isBestEffort = false)
            val count = setsFor(pattern, context.model.profile)
            val sets = if (state.isCalibrating) {
                List(count - 1) { work } + PlannedSet(exercise.range.last, isBestEffort = true)
            } else {
                List(count) { work }
            }
            val isHold = exercise.measure == GateMeasure.HOLD_SECONDS
            ExercisePlan(
                exercise = exercise,
                sets = sets,
                restSeconds = if (isHold) SessionPlanner.REST_SECONDS_HOLDS else SessionPlanner.REST_SECONDS_REPS,
                repsInReserve = SessionPlanner.TARGET_REPS_IN_RESERVE,
                intensity = Intensity.HARD,
            )
        }

    /** A focused plan gives patterns that have nothing to do with the goal the fewest sets. */
    private fun setsFor(pattern: MovementPattern, profile: CoachProfile): Int {
        val holdSteady = profile.style == PlanStyle.FOCUSED &&
            profile.goalPath != null &&
            VolumeTargets.roleOf(pattern, profile) == PatternRole.OTHER
        return if (holdSteady) CoachTuning.MIN_SETS_PER_EXERCISE else CoachTuning.SETS_PER_EXERCISE
    }

    /**
     * Takes exercises from the front of [wanted] until [budgetSeconds] runs out,
     * cutting one to fewer sets if that makes it fit. A session always gets its
     * first exercise, even when the person's time is shorter than that.
     */
    private fun fit(wanted: List<ExercisePlan>, budgetSeconds: Int): List<ExercisePlan> {
        val chosen = mutableListOf<ExercisePlan>()
        var left = budgetSeconds
        for (plan in wanted) {
            val short = plan.trimmedTo(CoachTuning.MIN_SETS_PER_EXERCISE)
            val fitted = listOf(plan, short).firstOrNull { it.estimatedSeconds <= left } ?: break
            chosen += fitted
            left -= fitted.estimatedSeconds
        }
        return chosen.ifEmpty { wanted.take(1).map { it.trimmedTo(CoachTuning.MIN_SETS_PER_EXERCISE) } }
    }
}
