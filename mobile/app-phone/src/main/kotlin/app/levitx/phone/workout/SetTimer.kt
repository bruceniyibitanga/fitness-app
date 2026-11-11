package app.levitx.phone.workout

/*
 * The timer for a hold or a set of slow negatives, worked out from the
 * device's elapsed clock alone, so it stays right however often the screen
 * redraws and whatever the screen was doing in between.
 */

/** The 3-2-1 before the timer starts, so the person can get on the bar first. */
internal const val LEAD_IN_MILLIS = 3_000L

/** Time to get back to the top before each negative: a jump, a step or a kick up. */
internal const val TO_TOP_MILLIS = 4_000L

private const val MILLIS_PER_SECOND = 1_000L

/** What a timer is timing. */
enum class TimerKind {
    /** A hold, counted up in seconds until the person stops it. */
    HOLD,

    /** Slow negatives, paced: back to the top, then a timed lowering, rep after rep. */
    NEGATIVE,
}

/**
 * A running timer for the set in progress.
 *
 * Not saved with the workout: if the app is closed mid-hold, the hold is lost
 * and the set is still there to do, which is the safe way round.
 *
 * @property kind What it is timing.
 * @property startsAtElapsedMillis When the lead-in ends and timing starts, on the elapsed clock.
 * @property lowerMillis How long each lowering lasts, for negatives.
 * @property reps Reps to pace, for negatives. The timer stops itself after the last.
 */
data class SetTimer(
    val kind: TimerKind,
    val startsAtElapsedMillis: Long,
    val lowerMillis: Long = 0,
    val reps: Int = 0,
) {
    private val repMillis: Long
        get() = TO_TOP_MILLIS + lowerMillis

    /** Where the timer is at [nowElapsedMillis]. */
    fun phaseAt(nowElapsedMillis: Long): TimerPhase {
        val since = nowElapsedMillis - startsAtElapsedMillis
        if (since < 0) return TimerPhase.LeadIn(secondsLeft = ceilSeconds(-since))
        return when (kind) {
            TimerKind.HOLD -> TimerPhase.Holding(seconds = (since / MILLIS_PER_SECOND).toInt())
            TimerKind.NEGATIVE -> negativePhase(since)
        }
    }

    private fun negativePhase(since: Long): TimerPhase {
        val done = (since / repMillis).toInt()
        val intoRep = since % repMillis
        return when {
            done >= reps -> TimerPhase.Done(reps)
            intoRep < TO_TOP_MILLIS -> TimerPhase.ToTop(rep = done + 1, millisLeft = TO_TOP_MILLIS - intoRep)
            else -> TimerPhase.Lowering(rep = done + 1, millisIn = intoRep - TO_TOP_MILLIS, millisTotal = lowerMillis)
        }
    }

    /**
     * What the timer counted by [nowElapsedMillis]: whole seconds held, or
     * negatives fully lowered. The person can correct it before logging.
     */
    fun countAt(nowElapsedMillis: Long): Int = when (val phase = phaseAt(nowElapsedMillis)) {
        is TimerPhase.LeadIn -> 0
        is TimerPhase.Holding -> phase.seconds
        is TimerPhase.ToTop -> phase.rep - 1
        is TimerPhase.Lowering -> phase.rep - 1
        is TimerPhase.Done -> phase.reps
    }
}

/** Where a [SetTimer] is. */
sealed interface TimerPhase {
    /** Counting down to the start, [secondsLeft] to go. */
    data class LeadIn(val secondsLeft: Int) : TimerPhase

    /** Holding, [seconds] so far. */
    data class Holding(val seconds: Int) : TimerPhase

    /** Getting back to the top for negative [rep], [millisLeft] to go. */
    data class ToTop(val rep: Int, val millisLeft: Long) : TimerPhase {
        /** Whole seconds to go, rounded up. */
        val secondsLeft: Int
            get() = ceilSeconds(millisLeft)
    }

    /** Lowering negative [rep], [millisIn] of [millisTotal]. */
    data class Lowering(val rep: Int, val millisIn: Long, val millisTotal: Long) : TimerPhase

    /** All [reps] negatives have been paced. */
    data class Done(val reps: Int) : TimerPhase
}

/** Whole seconds, rounded up, so a countdown reads 3, 2, 1 and never 0 while running. */
internal fun ceilSeconds(millis: Long): Int = ((millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
