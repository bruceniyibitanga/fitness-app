package app.levitx.core.coach

import app.levitx.core.session.LoggedSet
import app.levitx.core.session.PlannedSet
import app.levitx.core.session.SetLoad

private const val MILLIS_PER_SECOND = 1_000L

/**
 * A planned session in progress: which exercise is up, what has been done and
 * whether the person is resting.
 *
 * It is immutable. Each action returns the next state, so the rules for moving
 * through a workout live here once, and the phone, the watch and their tests
 * share them. Exercises are done in order; the person can skip one, or finish
 * the workout at any point.
 *
 * @property id Unique id the finished workout is stored under. Made when it
 *   starts, so saving it twice can never store it twice.
 * @property session The plan. Fixed when the workout starts, so targets don't move mid-session.
 * @property startedAtEpochMillis When it started, in UTC milliseconds.
 * @property choice What the person picked if the coach offered an easier day, else null.
 * @property logged Sets done so far, one list per exercise in [session].
 * @property skipped Indexes of the exercises the person skipped.
 * @property exerciseIndex The exercise up next. Equal to the number of exercises once complete.
 * @property restEndsAtElapsedMillis When the current rest ends on the device's
 *   elapsed (since boot) clock, or null when not resting.
 * @property load The load the next set will be logged with.
 * @throws IllegalArgumentException if [id] is blank or the progress doesn't fit the plan.
 */
public data class WorkoutRun(
    val id: String,
    val session: PlannedSession,
    val startedAtEpochMillis: Long,
    val choice: SuggestionChoice? = null,
    val logged: List<List<LoggedSet>> = session.plans.map { emptyList() },
    val skipped: Set<Int> = emptySet(),
    val exerciseIndex: Int = 0,
    val restEndsAtElapsedMillis: Long? = null,
    val load: SetLoad = SetLoad.None,
) {
    init {
        require(id.isNotBlank()) { "id must not be blank" }
        require(logged.size == session.plans.size) { "logged must have one list per exercise" }
        require(logged.indices.all { logged[it].size <= session.plans[it].sets.size }) { "more sets than planned" }
        require(exerciseIndex in 0..session.plans.size) { "exerciseIndex out of range" }
        require(skipped.all { it in session.plans.indices }) { "skipped exercise out of range" }
    }

    /** True once every exercise is done or skipped. */
    val isComplete: Boolean
        get() = exerciseIndex >= session.plans.size

    /** True while the rest timer is running. */
    val isResting: Boolean
        get() = restEndsAtElapsedMillis != null

    /** The exercise up next, or null once complete. */
    val current: ExercisePlan?
        get() = session.plans.getOrNull(exerciseIndex)

    /** The set to do next, or null once complete. */
    val nextSet: PlannedSet?
        get() = current?.sets?.getOrNull(logged[exerciseIndex].size)

    /** 0-based index of the next set within the current exercise. */
    val nextSetIndex: Int
        get() = logged.getOrNull(exerciseIndex)?.size ?: 0

    /** True when at least one set has been done. */
    val hasStarted: Boolean
        get() = logged.any { it.isNotEmpty() }

    /** Changes the load the next set will be logged with. */
    public fun withLoad(load: SetLoad): WorkoutRun = copy(load = load)

    /**
     * Records the set just done with the current [load]. When it finishes the
     * exercise, the workout moves on to the next one. Rest starts if anything
     * is left. Does nothing during rest or once complete.
     *
     * @param value Reps or seconds done. Negative values count as 0.
     * @param repsInReserve Reps the person felt they had left, or null if they didn't say.
     * @param nowElapsedMillis The elapsed clock now, which rest is timed from.
     */
    public fun logSet(value: Int, repsInReserve: Int?, nowElapsedMillis: Long): WorkoutRun {
        val plan = current
        val planned = nextSet
        if (plan == null || planned == null || isResting) return this
        val set = LoggedSet(
            value = value.coerceAtLeast(0),
            repsInReserve = repsInReserve?.coerceAtLeast(0),
            isBestEffort = planned.isBestEffort,
            addedKg = load.addedKg,
            assistanceKg = load.assistanceKg,
        )
        val done = logged[exerciseIndex] + set
        val finishedExercise = done.size == plan.sets.size
        val nextIndex = if (finishedExercise) exerciseIndex + 1 else exerciseIndex
        val restEndsAt = if (nextIndex < session.plans.size) {
            nowElapsedMillis + plan.restSeconds * MILLIS_PER_SECOND
        } else {
            null
        }
        return copy(
            logged = logged.replacing(exerciseIndex, done),
            exerciseIndex = nextIndex,
            restEndsAtElapsedMillis = restEndsAt,
            // Kit is set up per exercise, so a new exercise starts with none.
            load = if (finishedExercise) SetLoad.None else load,
        )
    }

    /**
     * Moves on from the current exercise. With no sets done it counts as
     * skipped, which the coach remembers. With some done it ends the exercise
     * early and keeps them. Ends any rest.
     */
    public fun skipExercise(): WorkoutRun {
        if (isComplete) return this
        val untouched = logged[exerciseIndex].isEmpty()
        return copy(
            skipped = if (untouched) skipped + exerciseIndex else skipped,
            exerciseIndex = exerciseIndex + 1,
            restEndsAtElapsedMillis = null,
            load = SetLoad.None,
        )
    }

    /** Ends rest, early or because it ran out. */
    public fun endRest(): WorkoutRun = copy(restEndsAtElapsedMillis = null)

    /**
     * Makes the current rest longer or shorter by [byMillis]. Shortening it
     * past now ends it on now, so the screen shows it as done rather than
     * jumping into the next set. Does nothing when not resting.
     *
     * @param nowElapsedMillis The elapsed clock now.
     */
    public fun adjustRest(byMillis: Long, nowElapsedMillis: Long): WorkoutRun {
        val endsAt = restEndsAtElapsedMillis ?: return this
        return copy(restEndsAtElapsedMillis = maxOf(endsAt + byMillis, nowElapsedMillis))
    }

    /**
     * Takes back the set just logged, for a mistap. Only works during the
     * rest that set started, which is the only time the set just logged is
     * unambiguous: the run goes back to before it, with the same load.
     */
    public fun undoLastSet(): WorkoutRun {
        // A set that finished its exercise moved the run on to the next one.
        val index = if (logged.getOrNull(exerciseIndex).isNullOrEmpty()) exerciseIndex - 1 else exerciseIndex
        val sets = logged.getOrNull(index).orEmpty()
        if (!isResting || sets.isEmpty() || index in skipped) return this
        val last = sets.last()
        return copy(
            logged = logged.replacing(index, sets.dropLast(1)),
            exerciseIndex = index,
            restEndsAtElapsedMillis = null,
            load = SetLoad(addedKg = last.addedKg, assistanceKg = last.assistanceKg),
        )
    }

    /**
     * This run with any impossible rest ended. Rest is timed on the clock that
     * counts from boot, which starts again after a restart, so a saved rest
     * could look hours long. No rest is longer than the longest in the plan,
     * so anything longer is ended. Call it on a run read back from storage.
     *
     * @param nowElapsedMillis The elapsed clock now.
     */
    public fun withSaneRest(nowElapsedMillis: Long): WorkoutRun {
        val endsAt = restEndsAtElapsedMillis ?: return this
        val longest = session.plans.maxOf { it.restSeconds } * MILLIS_PER_SECOND
        return if (endsAt > nowElapsedMillis + longest) endRest() else this
    }

    /**
     * The workout as it stands, ready to give to [Coach.record], or null when
     * no set was done. Exercises not reached are left out: stopping early isn't
     * the same as not liking them.
     *
     * @param finishedAtEpochMillis Now, in UTC milliseconds. If the device clock
     *   was moved back mid-workout, the start time is used instead.
     */
    public fun result(finishedAtEpochMillis: Long): SessionResult? {
        if (!hasStarted) return null
        val results = session.plans.indices.mapNotNull { index ->
            val sets = logged[index]
            when {
                index in skipped -> ExerciseResult(session.plans[index], emptyList(), skipped = true)
                sets.isNotEmpty() -> ExerciseResult(session.plans[index], sets)
                else -> null
            }
        }
        return SessionResult(
            results = results,
            startedAtEpochMillis = startedAtEpochMillis,
            finishedAtEpochMillis = maxOf(finishedAtEpochMillis, startedAtEpochMillis),
            choice = choice,
        )
    }
}

private fun <T> List<T>.replacing(index: Int, value: T): List<T> = mapIndexed { i, old ->
    if (i ==
        index
    ) {
        value
    } else {
        old
    }
}
