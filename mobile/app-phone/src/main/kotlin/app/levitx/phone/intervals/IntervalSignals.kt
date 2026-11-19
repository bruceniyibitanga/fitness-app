package app.levitx.phone.intervals

import app.levitx.data.settings.AppSettings
import app.levitx.phone.live.CueOutput
import app.levitx.phone.live.KeepAwake
import app.levitx.phone.live.LiveIntervals
import app.levitx.phone.live.LiveWorkout
import app.levitx.phone.workout.Cue
import javax.inject.Inject

/**
 * What the interval timer does to the phone itself: beeps and buzzes, the
 * wake lock that keeps them on time with the screen off, and the workout it
 * shows as in progress. Kept apart from [IntervalTimerViewModel] so the
 * timer only deals with time.
 *
 * Showing the workout is what keeps the timer alive off screen: the workout
 * service runs for as long as one is shown, and Android doesn't pause an app
 * with a workout service running. Without it the beeps stop soon after the
 * person opens another app, their music say.
 *
 * Not a singleton: each timer gets its own speaker and lock, so it can't
 * cut off the strength workout's cues or let go of their lock.
 */
class IntervalSignals @Inject internal constructor(
    private val output: CueOutput,
    private val awake: KeepAwake,
    private val live: LiveWorkout,
) {
    /** Shows [intervals] as the workout in progress: on the lock screen, and to the service that keeps it running. */
    fun show(intervals: LiveIntervals) {
        live.publishIntervals(this, intervals)
    }

    /** Stops showing a workout in progress, once it is over. */
    fun hide() {
        live.clearIntervals(this)
    }

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

    /** Lets go of the speaker, the lock and the workout shown, once the timer is gone. */
    fun release() {
        hide()
        awake.release()
        output.release()
    }
}
