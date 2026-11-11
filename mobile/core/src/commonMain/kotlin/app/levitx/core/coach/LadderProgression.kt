package app.levitx.core.coach

import app.levitx.core.session.SessionPlanner
import kotlin.math.roundToInt

/** What to change for an exercise after a session. */
private sealed interface Change {
    data object Stay : Change

    data class Target(val target: Int, val note: ProgressNote?) : Change

    /** Move [by] rungs, up or down. [fallbackTarget] applies when the ladder has no such rung. */
    data class Rungs(val by: Int, val fallbackTarget: Int) : Change
}

/**
 * How someone moves along a ladder.
 *
 * Reps go up first. At the top of an exercise's range the person moves up a
 * rung. Two sessions in a row short of target ease it back. While the coach is
 * still finding their level, a best-effort set places them directly. Fast
 * weight loss holds targets where they are.
 */
internal object LadderProgression {
    /**
     * The model after one hard ladder exercise. [model] already holds this
     * session's mark for the exercise, so the mark before it is the last session.
     */
    fun learn(model: AthleteModel, result: ExerciseResult, context: CoachContext): AthleteModel {
        val exercise = result.plan.exercise
        val state = model.exercises.getValue(exercise.id)
        val earlier = state.marks.dropLast(1)
        val wasCalibrating = state.timesTrained <= CoachTuning.CALIBRATION_SESSIONS
        val outcome = Outcome.of(result)
        val change = when {
            wasCalibrating && outcome.didBestEffort -> calibrate(exercise, outcome.top)
            context.holdProgress -> Change.Stay
            outcome.hitTarget && outcome.effort != Effort.HARD -> raise(exercise, state.target, outcome, earlier)
            !outcome.hitTarget && earlier.lastOrNull()?.hitTarget == false -> ease(exercise, state.target)
            else -> Change.Stay
        }
        return when (change) {
            Change.Stay -> model
            is Change.Target -> model.withTarget(exercise, change.target, change.note)
            is Change.Rungs -> moveRungs(model, exercise, change, context)
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
            jump >= 1 -> Change.Rungs(by = jump, fallbackTarget = exercise.range.last)
            top < exercise.range.first -> Change.Rungs(by = -1, fallbackTarget = workTarget)
            else -> Change.Target(workTarget.coerceIn(exercise.range), note = null)
        }
    }

    /** One step up, or two when this session and the one before were both easy. */
    private fun raise(exercise: Exercise, target: Int, outcome: Outcome, earlier: List<ExerciseMark>): Change {
        val last = earlier.lastOrNull()
        val easyTwice = outcome.effort == Effort.EASY && last?.effort == Effort.EASY && last.hitTarget
        val raised = target + exercise.step * if (easyTwice) 2 else 1
        return if (raised > exercise.range.last) {
            Change.Rungs(by = 1, fallbackTarget = exercise.range.last)
        } else {
            Change.Target(raised, ProgressNote.TARGET_UP)
        }
    }

    private fun ease(exercise: Exercise, target: Int): Change {
        val eased = target - exercise.step
        return if (eased < exercise.range.first) {
            Change.Rungs(by = -1, fallbackTarget = eased.coerceAtLeast(1))
        } else {
            Change.Target(eased, ProgressNote.EASED_BACK)
        }
    }

    /**
     * Moves the person along the ladder when [exercise] is the one they train
     * at their current rung and there is a rung to move to with their kit.
     * Otherwise only the target changes.
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
        return if (arrival == null) {
            model.withTarget(exercise, change.fallbackTarget, note = null)
        } else {
            val note = if (change.by > 0) ProgressNote.MOVED_UP else ProgressNote.EASED_BACK
            val state = moved.exercises[arrival.id] ?: ExerciseState(target = arrival.range.first)
            moved.copy(exercises = moved.exercises + (arrival.id to state.copy(note = note)))
        }
    }

    /** The rung [by] steps from the person's current one, or null when they can't or shouldn't move. */
    private fun destination(model: AthleteModel, exercise: Exercise, by: Int): Int? {
        val ladder = ExerciseCatalogue.ladder(exercise.pattern)
        val owned = model.profile.equipment
        val current = model.rung(exercise.pattern)
        val onCurrentRung = ladder.rungs.getOrNull(current)?.options?.contains(exercise) == true
        val reached = if (by > 0) {
            (1..by).fold(current) { rung, _ -> ladder.usableRungAbove(rung, owned) ?: rung }
        } else {
            ladder.usableRungAtOrBelow(current - 1, owned)?.takeIf { current > 0 }
        }
        return reached?.takeIf { onCurrentRung && it != current }
    }

    private fun AthleteModel.withTarget(exercise: Exercise, target: Int, note: ProgressNote?): AthleteModel {
        val state = exercises.getValue(exercise.id)
        return copy(exercises = exercises + (exercise.id to state.copy(target = target, note = note)))
    }
}
