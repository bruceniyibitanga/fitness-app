package app.levitx.wear.home

import app.levitx.data.coach.WorkoutOutbox
import app.levitx.data.intervals.IntervalOutbox
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Workouts of every kind finished on the watch that the phone hasn't confirmed yet. */
class PendingWorkouts @Inject constructor(private val workouts: WorkoutOutbox, private val intervals: IntervalOutbox) {
    /** What is waiting. Emits straight away and whenever either queue changes. */
    fun observe(): Flow<Pending> = combine(workouts.observeQueued(), intervals.observeQueued()) { strength, timed ->
        Pending(strength.size, timed.size)
    }
}

/**
 * How many finished workouts are waiting for the phone.
 *
 * @property strength Strength sessions. While any wait, the watch has no plan:
 *   the phone plans the next session once it has them.
 * @property timed Cardio and mobility workouts. They never affect the plan.
 */
data class Pending(val strength: Int, val timed: Int) {
    /** Every kind together. */
    val total: Int
        get() = strength + timed
}
