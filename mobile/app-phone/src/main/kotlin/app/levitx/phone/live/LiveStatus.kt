package app.levitx.phone.live

import app.levitx.phone.workout.TimerKind

/** What the workout notification says the person is doing. */
internal sealed interface LiveStatus {
    /** Resting until [endsAt] on the elapsed clock. */
    data class Resting(val endsAt: Long) : LiveStatus

    /** Rest has run out; the next set is waiting for them. */
    data object RestOver : LiveStatus

    /** Counting in to a hold or negatives that start at [startsAt]. */
    data class CountingIn(val startsAt: Long) : LiveStatus

    /** Holding, or doing negatives, since [startedAt]. */
    data class Timing(val kind: TimerKind, val startedAt: Long) : LiveStatus

    /** A set is up next, counted by hand. */
    data object Ready : LiveStatus
}

/** What [session] is doing at [now] on the elapsed clock. */
internal fun liveStatus(session: LiveSession, now: Long): LiveStatus {
    val timer = session.timer
    val restEnds = session.restEndsAtElapsedMillis
    return when {
        timer != null && now < timer.startsAtElapsedMillis -> LiveStatus.CountingIn(timer.startsAtElapsedMillis)
        timer != null -> LiveStatus.Timing(timer.kind, timer.startsAtElapsedMillis)
        restEnds != null && now < restEnds -> LiveStatus.Resting(restEnds)
        restEnds != null -> LiveStatus.RestOver
        else -> LiveStatus.Ready
    }
}

/** When this status turns into the next one on its own, on the elapsed clock, or null if it doesn't. */
internal fun LiveStatus.changesAt(): Long? = when (this) {
    is LiveStatus.Resting -> endsAt
    is LiveStatus.CountingIn -> startsAt
    else -> null
}

/** [this] without what only the cues use, so a sound being turned off doesn't post the notification again. */
internal fun LiveSession.forNotice(): LiveSession = copy(passedGate = null, sounds = true, buzzes = true)
