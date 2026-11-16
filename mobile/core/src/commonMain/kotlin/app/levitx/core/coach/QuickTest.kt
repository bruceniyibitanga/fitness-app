package app.levitx.core.coach

import app.levitx.core.session.SessionPlanner
import kotlin.math.roundToInt

/**
 * One movement of the optional quick test at setup: one fresh set of as many
 * clean reps as the person can do. Each result places its ladder before the
 * first session, the same way a calibration set does in training.
 *
 * The test is optional. Skipped, every ladder starts at its easiest rung and
 * the first sessions find the level, so nothing here is needed to train.
 *
 * @property exerciseId The ladder exercise tested, as an [Exercise.id].
 * @property needs Kit the test needs, or null for none.
 * @property maxReps The highest count accepted. Far past anything the ladders
 *   use, so a real result is never cut off and a slip of the thumb is.
 */
public enum class QuickTest(public val exerciseId: String, public val needs: Equipment?, public val maxReps: Int) {
    /** Push-ups from the floor. */
    PUSH_UPS("horizontal_push.push_up", null, MAX_UPPER_REPS),

    /** Strict pull-ups from a dead hang. */
    PULL_UPS("vertical_pull.pull_up", Equipment.PULL_UP_BAR, MAX_UPPER_REPS),

    /** Dips on parallel bars. */
    DIPS("vertical_push.dip", Equipment.DIP_BARS, MAX_UPPER_REPS),

    /** Pike push-ups, the overhead press for anyone without dip bars. */
    PIKE_PUSH_UPS("vertical_push.pike", null, MAX_UPPER_REPS),

    /** Bodyweight squats. */
    SQUATS("squat.bodyweight", null, MAX_SQUAT_REPS),
    ;

    /** The exercise tested. */
    public val exercise: Exercise
        get() = checkNotNull(ExerciseCatalogue.byId(exerciseId)) { "$exerciseId is not in the catalogue" }

    /** Which tests to offer, in order. */
    public companion object {
        /**
         * The tests someone with [owned] kit can do: push-ups, pull-ups with a
         * bar, dips with dip bars or pike push-ups without, then squats.
         */
        public fun forKit(owned: Set<Equipment>): List<QuickTest> = listOfNotNull(
            PUSH_UPS,
            PULL_UPS.takeIf { Equipment.PULL_UP_BAR in owned },
            if (Equipment.DIP_BARS in owned) DIPS else PIKE_PUSH_UPS,
            SQUATS,
        )
    }
}

/** Places ladders from quick test results. */
internal object QuickTestPlacement {
    /**
     * [model] with the ladder [test] trains placed from [reps].
     *
     * - None: the ladder starts at the bottom, like a "not yet" at setup.
     * - Short of the bottom of the test exercise's range: one rung easier.
     * - At or past the top: one rung harder. Calibration can move two at once,
     *   but this is a self-counted set nobody checked, so the test is kept to
     *   one and the first sessions do the rest.
     * - In between: on the test exercise, with work sets at the usual share of
     *   the result and the result kept as the best so far.
     *
     * Rungs the person has no kit for are stepped over.
     *
     * @throws IllegalArgumentException if [reps] is negative or over [QuickTest.maxReps].
     */
    fun place(model: AthleteModel, test: QuickTest, reps: Int): AthleteModel {
        require(reps in 0..test.maxReps) { "a ${test.name} result must be 0 to ${test.maxReps}, was $reps" }
        val exercise = test.exercise
        val ladder = ExerciseCatalogue.ladder(exercise.pattern)
        if (reps == 0) return model.copy(rungs = model.rungs - ladder.pattern)
        val owned = model.profile.equipment
        val tested = ladder.rungs.indexOfFirst { rung -> rung.options.any { it.id == exercise.id } }
        val rung = when {
            reps < exercise.range.first -> ladder.usableRungAtOrBelow(tested - 1, owned)
            reps >= exercise.range.last -> ladder.usableRungAbove(tested, owned) ?: tested
            else -> tested
        } ?: 0
        val placed = model.copy(rungs = model.rungs + (ladder.pattern to rung))
        return if (rung == tested) placed.withTestResult(exercise, reps) else placed
    }

    /** Starts [exercise] at the usual share of [reps], with [reps] as the best so far. */
    private fun AthleteModel.withTestResult(exercise: Exercise, reps: Int): AthleteModel {
        val workTarget = (reps * SessionPlanner.WORK_FRACTION).roundToInt()
        val state = ExerciseState(target = workTarget.coerceIn(exercise.range.first, exercise.ceiling), best = reps)
        return copy(exercises = exercises + (exercise.id to state))
    }
}

/** The most push-ups, pull-ups, dips or pike push-ups a test result can be. */
private const val MAX_UPPER_REPS = 100

/** The most squats a test result can be. */
private const val MAX_SQUAT_REPS = 200
