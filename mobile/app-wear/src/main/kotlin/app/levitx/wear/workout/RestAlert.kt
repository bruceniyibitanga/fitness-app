package app.levitx.wear.workout

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import app.levitx.data.time.TrainingClock
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

private const val WAKE_LOCK_TAG = "levitx:rest"

/** Extra time on the wake lock's own limit, so it outlasts the delay it protects. */
private const val WAKE_LOCK_MARGIN_MILLIS = 5_000L

/** Two firm pulses: clear on the wrist without looking, and unlike a notification. */
private val REST_OVER_PATTERN = longArrayOf(0, 250, 150, 250)
private const val NO_REPEAT = -1

/**
 * Waits out a rest and buzzes when it ends, even with the screen off.
 *
 * With the screen off a watch puts its processor to sleep, and an ordinary
 * timer then fires late. A partial wake lock keeps the processor awake for
 * the length of the rest only (two minutes at most, a few times a session).
 * It always has a time limit, so it is released even if this code never gets
 * to. ADR 0006 explains why this was chosen over an exact alarm.
 */
@Singleton
class RestAlert @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clock: TrainingClock,
) {
    /**
     * Suspends until [restEndsAtElapsedMillis] on the elapsed clock, then
     * buzzes. Cancelling it (the person skipped the rest) releases the wake
     * lock and doesn't buzz. Returns at once, without a buzz, if rest is
     * already over.
     */
    suspend fun awaitEndOfRest(restEndsAtElapsedMillis: Long) {
        val remainingMillis = restEndsAtElapsedMillis - clock.elapsedRealtimeMillis()
        if (remainingMillis <= 0) return
        val wakeLock = checkNotNull(context.getSystemService(PowerManager::class.java))
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
        wakeLock.acquire(remainingMillis + WAKE_LOCK_MARGIN_MILLIS)
        try {
            delay(remainingMillis)
            buzz()
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    private fun buzz() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(REST_OVER_PATTERN, NO_REPEAT))
    }
}
