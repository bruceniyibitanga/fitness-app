package app.levitx.phone.intervals

import app.levitx.data.settings.AppSettings
import app.levitx.phone.live.CueOutput
import app.levitx.phone.live.KeepAwake
import app.levitx.phone.workout.Cue
import javax.inject.Inject

/**
 * What the interval timer does to the phone itself: beeps and buzzes, and
 * the wake lock that keeps them on time with the screen off. Kept apart
 * from [IntervalTimerViewModel] so the timer only deals with time.
 *
 * Not a singleton: each timer gets its own speaker and lock, so it can't
 * cut off the strength workout's cues or let go of their lock.
 */
class IntervalSignals @Inject internal constructor(private val output: CueOutput, private val awake: KeepAwake) {
    /** Plays [cue] as [settings] allow: a beep, a buzz, both, or nothing. */
    internal fun play(cue: Cue, settings: AppSettings) {
        output.play(cue, settings.sounds, settings.buzzes)
    }

    /** Keeps the processor on for [millis]. A later call replaces the time limit. */
    fun stayAwakeFor(millis: Long) {
        awake.holdFor(millis)
    }

    /** Lets the processor sleep again. */
    fun letSleep() {
        awake.release()
    }

    /** Lets go of the speaker and the lock, once the timer is gone. */
    fun release() {
        awake.release()
        output.release()
    }
}
