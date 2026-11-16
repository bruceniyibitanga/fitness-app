package app.levitx.core.coach

import app.levitx.core.session.SessionPlanner
import kotlin.math.roundToInt

/** What to change for an exercise after a session. */
private sealed interface Change {
    data object Stay : Change

    data class Target(val target: Int, val note: ProgressNote?) : Change

    /**
     * Move [by] rungs, up or down. [target] applies when the ladder has no
     * such rung: the raised target when moving up, the eased one when moving
     * down. [placing] is true when a best-effort set is placing the person,
     * which never adds weight.
     */
    data class Rungs(val by: Int, val target: Int, val placing: Boolean = false) : Change

    /** Take the weight down a step and aim for the top of the range with it. */
    data object Lighter : Change

    /** Go back to the usual pace of lowering and aim for the top of the range. */
    data object UsualPace : Change
}

/**
 * How someone moves along a ladder.
 *
 * Reps go up first. At the top of an exercise's range the person moves up a
 * rung. At the top of the ladder, or of what their kit allows, the target keeps
 * climbing another way: weight first if they have it, then more reps, then a
 * slower lowering. Two sessions in a row short of target ease it back, undoing
 * those in reverse: the weight first, then the slow lowering, then the rung.
 * While the coach is still finding their level, a best-effort set places them
 * directly. Fast weight loss holds targets where they are.
 */
internal object LadderProgression {
    /**
     * The model after one hard ladder exercise. [model] already holds this
     * session's mark for the exercise, so the mark before it is the last session.
     */
    fun learn(model: AthleteModel, result: ExerciseResult, context: CoachContext): AthleteModel {
        val exercise = result.plan.exercise
        val outcome = Outcome.of(result)
        val adopted = model.adoptingHeavierWeight(exercise, result, outcome)
        val state = adopted.exercises.getValue(exercise.id)
        val earlier = state.marks.dropLast(1)
        val wasCalibrating = state.timesTrained <= CoachTuning.CALIBRATION_SESSIONS
        val change = when {
            wasCalibrating && outcome.didBestEffort -> calibrate(exercise, outcome.top)
            context.holdProgress -> Change.Stay
            outcome.hitTarget && outcome.effort != Effort.HARD -> raise(exercise, state.target, outcome, earlier)
            !outcome.hitTarget && earlier.lastOrNull()?.hitTarget == false -> ease(exercise, state)
            else -> Change.Stay
        }
        return when (change) {
            Change.Stay -> adopted

            is Change.Target -> adopted.withState(exercise) { it.copy(target = change.target, note = change.note) }

            is Change.Rungs -> moveRungs(adopted, exercise, change, context)

            Change.Lighter -> adopted.withState(exercise) {
                it.copy(
                    addedKg = LoadProgression.nextDown(exercise, it.addedKg, adopted.profile.loadKit),
                    target = exercise.range.last,
                    note = ProgressNote.EASED_BACK,
                )
            }

            Change.UsualPace -> adopted.withState(exercise) {
                it.copy(slowLowering = false, target = exercise.range.last, note = ProgressNote.EASED_BACK)
            }
        }
    }

    /**
     * Places someone from a best-effort set. Reaching the top of the range
     * moves them up, twice over for double it. Falling short of the bottom
     * moves them down. Otherwise work sets start at the usual share of the best.
     */
    private fun calibrate(exercise: Exercise, top: Int): Change {
        val workTarget = (top * SessionPlanner.WORK_FRACTION).roundToInt().coerceAtLeast(1)
        val jump = (top / exercise.range.last).coerceAtMost(CoachTuning.MAX_CALIBRATION_JUMP)
        return when {
            jump >= 1 -> Change.Rungs(by = jump, target = workTarget, placing = true)
            top < exercise.range.first -> Change.Rungs(by = -1, target = workTarget)
            else -> Change.Target(workTarget.coerceIn(exercise.range), note = null)
        }
    }

    /** One step up, or two when this session and the one before were both easy. */
    private fun raise(exercise: Exercise, target: Int, outcome: Outcome, earlier: List<ExerciseMark>): Change {
        val last = earlier.lastOrNull()
        val easyTwice = outcome.effort == Effort.EASY && last?.effort == Effort.EASY && last.hitTarget
        val raised = target + exercise.step * if (easyTwice) 2 else 1
        return if (raised > exercise.range.last) {
            Change.Rungs(by = 1, target = raised)
        } else {
            Change.Target(raised, ProgressNote.TARGET_UP)
        }
    }

    /** One step back, undoing the last way the exercise got harder. */
    private fun ease(exercise: Exercise, state: ExerciseState): Change {
        val eased = state.target - exercise.step
        return when {
            eased >= exercise.range.first -> Change.Target(eased, ProgressNote.EASED_BACK)
            state.addedKg > 0.0 -> Change.Lighter
            state.slowLowering -> Change.UsualPace
            else -> Change.Rungs(by = -1, target = eased.coerceAtLeast(1))
        }
    }

    /**
     * Moves the person along the ladder when [exercise] is the one they train
     * at their current rung and there is a rung to move to with their kit.
     * At the top, moving up becomes [topOut]. Otherwise only the target changes.
     */
    private fun moveRungs(
        model: AthleteModel,
        exercise: Exercise,
        change: Change.Rungs,
        context: CoachContext,
    ): AthleteModel {
        val current = model.rung(exercise.pattern)
        val destination = destination(model, exercise, change.by)
        val moved = model.copy(rungs = model.rungs + (exercise.pattern to (destination ?: current)))
        val arrival = moved.exerciseFor(exercise.pattern, context.goalPatterns)?.takeIf { destination != null }
        return when {
            arrival != null -> {
                val note = if (change.by > 0) ProgressNote.MOVED_UP else ProgressNote.EASED_BACK
                val state = moved.exercises[arrival.id] ?: ExerciseState(target = arrival.range.first)
                moved.copy(exercises = moved.exercises + (arrival.id to state.copy(note = note)))
            }

            change.by > 0 && model.isAtTop(exercise) -> topOut(model, exercise, change, context)

            // A best-effort set that topped the range, on an exercise they can't move up from here.
            change.placing -> model.withState(exercise) { it.copy(target = exercise.range.last, note = null) }

            else -> model.withState(exercise) {
                it.copy(target = change.target.coerceIn(1, exercise.range.last), note = null)
            }
        }
    }

    /**
     * Makes [exercise] harder when there is nowhere higher to go: weight if the
     * kit allows a step of the right size, else more reps up to the
     * exercise's ceiling, else a slower lowering. A best-effort set only
     * places the target; it never adds weight.
     */
    private fun topOut(
        model: AthleteModel,
        exercise: Exercise,
        change: Change.Rungs,
        context: CoachContext,
    ): AthleteModel {
        val bodyweight = context.weightTrend?.currentKg
        return model.withState(exercise) { state ->
            val heavier = if (change.placing) {
                null
            } else {
                LoadProgression.nextUp(exercise, state.addedKg, model.profile.loadKit, bodyweight)
            }
            when {
                heavier != null ->
                    state.copy(addedKg = heavier, target = exercise.range.first, note = ProgressNote.WEIGHT_UP)

                change.placing -> state.copy(target = change.target.coerceIn(exercise.range.first, exercise.ceiling))

                state.target < exercise.ceiling ->
                    state.copy(target = change.target.coerceAtMost(exercise.ceiling), note = ProgressNote.TARGET_UP)

                exercise.allowsSlowLowering && !state.slowLowering ->
                    state.copy(slowLowering = true, target = exercise.range.last, note = ProgressNote.SLOWER)

                else -> state.copy(target = exercise.ceiling, note = null)
            }
        }
    }

    /** The rung [by] steps from the person's current one, or null when they can't or shouldn't move. */
    private fun destination(model: AthleteModel, exercise: Exercise, by: Int): Int? {
        val ladder = ExerciseCatalogue.ladder(exercise.pattern)
        val owned = model.profile.equipment
        val current = model.rung(exercise.pattern)
        val reached = if (by > 0) {
            (1..by).fold(current) { rung, _ -> ladder.usableRungAbove(rung, owned) ?: rung }
        } else {
            ladder.usableRungAtOrBelow(current - 1, owned)?.takeIf { current > 0 }
        }
        return reached?.takeIf { model.isOnCurrentRung(exercise) && it != current }
    }
}

/** True when [exercise] is on the person's rung of its ladder. */
private fun AthleteModel.isOnCurrentRung(exercise: Exercise): Boolean =
    ExerciseCatalogue.ladder(exercise.pattern).rungs.getOrNull(rung(exercise.pattern))?.options
        ?.contains(exercise) == true

/** True when [exercise] is on the person's rung and there is nothing higher they have the kit for. */
private fun AthleteModel.isAtTop(exercise: Exercise): Boolean = isOnCurrentRung(exercise) &&
    ExerciseCatalogue.ladder(exercise.pattern).usableRungAbove(rung(exercise.pattern), profile.equipment) == null

/**
 * When the person did every work set with more weight than prescribed and
 * still hit the target, the coach takes that weight as theirs: the next
 * step goes from there.
 */
private fun AthleteModel.adoptingHeavierWeight(
    exercise: Exercise,
    result: ExerciseResult,
    outcome: Outcome,
): AthleteModel {
    val used = result.workSets.minOfOrNull { it.addedKg } ?: return this
    val state = exercises.getValue(exercise.id)
    return if (outcome.hitTarget && used > state.addedKg) withState(exercise) { it.copy(addedKg = used) } else this
}

private fun AthleteModel.withState(exercise: Exercise, change: (ExerciseState) -> ExerciseState): AthleteModel {
    val state = exercises.getValue(exercise.id)
    return copy(exercises = exercises + (exercise.id to change(state)))
}
