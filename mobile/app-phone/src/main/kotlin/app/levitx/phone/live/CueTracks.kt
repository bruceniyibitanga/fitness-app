package app.levitx.phone.live

import app.levitx.phone.workout.Beat
import app.levitx.phone.workout.REST_COUNTDOWN_SECONDS
import app.levitx.phone.workout.SetTimer
import app.levitx.phone.workout.TimerPhase
import app.levitx.phone.workout.ceilSeconds
import app.levitx.phone.workout.restBeat
import app.levitx.phone.workout.timerBeat

/*
 * When each cue is due. Every timer in a workout changes on whole seconds
 * counted from one moment (the end of rest, or the start of a hold or of
 * negatives), so the player can sleep until the next of those seconds instead
 * of checking the clock many times a second.
 */

private const val MILLIS_PER_SECOND = 1_000L

/**
 * A "go" more than this late isn't played. It would only be heard when the
 * app comes back long after rest ran out, when it means nothing.
 */
internal const val LATE_GO_MILLIS = 1_500L

/**
 * A hold stops being cued this long after it starts. No hold in the app is
 * near this long; it only stops a forgotten timer keeping the phone awake.
 */
internal const val LONGEST_CUED_MILLIS = 10 * 60 * MILLIS_PER_SECOND

/** One run of cues: a rest, or a timer. Equal tracks are the same run of cues. */
internal sealed interface CueTrack {
    /** The beat at [now] on the elapsed clock, or null once there is nothing left to cue. */
    fun beatAt(now: Long): Beat?

    /** When the beat next changes, after [now], or null when it won't. */
    fun nextChangeAfter(now: Long): Long?
}

/** Rest that runs out at [endsAt]: a count in the last seconds, then go. */
internal data class RestTrack(val endsAt: Long) : CueTrack {
    override fun beatAt(now: Long): Beat? {
        val left = endsAt - now
        return when {
            left > 0 -> restBeat(ceilSeconds(left))
            -left < LATE_GO_MILLIS -> restBeat(0)
            else -> null
        }
    }

    override fun nextChangeAfter(now: Long): Long? =
        (REST_COUNTDOWN_SECONDS downTo 0).map { endsAt - it * MILLIS_PER_SECOND }.firstOrNull { it > now }
}

/** A running hold or negatives [timer], with [target] seconds or reps. */
internal data class TimerTrack(val timer: SetTimer, val target: Int) : CueTrack {
    override fun beatAt(now: Long): Beat? = if (now - timer.startsAtElapsedMillis > LONGEST_CUED_MILLIS) {
        null
    } else {
        timerBeat(timer.phaseAt(now), target)
    }

    override fun nextChangeAfter(now: Long): Long? {
        if (timer.phaseAt(now) is TimerPhase.Done) return null
        val since = now - timer.startsAtElapsedMillis
        return timer.startsAtElapsedMillis + (Math.floorDiv(since, MILLIS_PER_SECOND) + 1) * MILLIS_PER_SECOND
    }
}

/** The cues [session] needs now: its timer if one runs, otherwise its rest, otherwise none. */
internal fun cueTrack(session: LiveSession): CueTrack? = when {
    session.timer != null -> TimerTrack(session.timer, session.target)
    session.restEndsAtElapsedMillis != null -> RestTrack(session.restEndsAtElapsedMillis)
    else -> null
}
