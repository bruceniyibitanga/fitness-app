package app.levitx.wear.workout

import android.content.Context
import android.os.PowerManager
import app.levitx.core.intervals.IntervalCue
import app.levitx.wear.intervals.IntervalSignals
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

private const val WAKE_LOCK_TAG = "levitx:intervals"

/**
 * [IntervalSignals] on the watch: a buzz for every cue (a watch speaker is
 * easy to miss mid-workout, a wrist isn't), and a partial wake lock so the
 * buzzes come on time with the screen off.
 *
 * The wake lock always has a time limit, the workout's remaining length
 * plus a margin, so it is let go even if this code never gets to. ADR 0006
 * explains why a wake lock was chosen over an exact alarm for rest; the same
 * reasons hold here, with a buzz every few seconds.
 */
@Singleton
class BuzzingIntervalSignals @Inject constructor(@param:ApplicationContext private val context: Context) :
    IntervalSignals {
    private val wakeLock: PowerManager.WakeLock by lazy {
        checkNotNull(context.getSystemService(PowerManager::class.java))
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            // Not counted, so each new time limit replaces the last rather than stacking.
            .apply { setReferenceCounted(false) }
    }

    override fun play(cue: IntervalCue) {
        context.buzz(
            when (cue) {
                IntervalCue.GO -> ONE_LONG_PULSE
                IntervalCue.CHANGE -> TWO_PULSES
                IntervalCue.COUNT -> ONE_TAP
                IntervalCue.DONE -> THREE_PULSES
            },
        )
    }

    override fun stayAwakeFor(millis: Long) {
        wakeLock.acquire(millis)
    }

    override fun letSleep() {
        if (wakeLock.isHeld) wakeLock.release()
    }
}

/** Tells Hilt which [IntervalSignals] the app uses. */
@Module
@InstallIn(SingletonComponent::class)
interface IntervalSignalsModule {
    /** Binds the buzzing signals. */
    @Binds
    fun bindIntervalSignals(signals: BuzzingIntervalSignals): IntervalSignals
}
