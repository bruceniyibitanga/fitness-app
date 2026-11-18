package app.levitx.wear.workout

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/*
 * The buzzes the watch gives during a workout, in one place so rest and the
 * interval timer feel like the same app. Every pattern is waveform timings
 * in milliseconds: wait, buzz, wait, buzz.
 */

/** Two firm pulses: clear on the wrist without looking, and unlike a notification. */
internal val TWO_PULSES = longArrayOf(0, 250, 150, 250)

/** One long pulse: go. */
internal val ONE_LONG_PULSE = longArrayOf(0, 500)

/** One short tap, for each of the last three seconds. */
internal val ONE_TAP = longArrayOf(0, 60)

/** Three pulses: the workout is over. */
internal val THREE_PULSES = longArrayOf(0, 200, 120, 200, 120, 200)

private const val NO_REPEAT = -1

/** Buzzes [pattern] once. Does nothing on a watch without a vibrator. */
internal fun Context.buzz(pattern: LongArray) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        getSystemService(Vibrator::class.java)
    }
    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, NO_REPEAT))
}
