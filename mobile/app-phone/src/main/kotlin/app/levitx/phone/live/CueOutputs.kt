package app.levitx.phone.live

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import app.levitx.phone.workout.Buzz
import app.levitx.phone.workout.Cue
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/*
 * The two things the cue player needs from the device, behind interfaces so
 * the player's timing can be tested without a speaker, a motor or a clock.
 */

/** Plays cues. */
internal interface CueOutput {
    /** Plays [cue]: its sound when [sound], its buzz when [buzz]. Returns without waiting for either. */
    fun play(cue: Cue, sound: Boolean, buzz: Boolean)

    /** Frees the speaker once the workout is over. Playing again opens it again. */
    fun release()
}

/** Keeps the processor awake while a cue is due. */
internal interface KeepAwake {
    /** Keeps it awake for [millis] from now, replacing any earlier time. */
    fun holdFor(millis: Long)

    /** Lets it sleep again. Safe to call when nothing is held. */
    fun release()
}

/** Beep volume, as a percentage of the media volume, so it follows the person's own setting. */
private const val TONE_VOLUME = 80

/** Buzz length on phones too old for the system's own tick and click (Android 9). */
private const val OLD_BUZZ_MILLIS = 30L

/**
 * Beeps through the media stream and buzzes with the phone's motor.
 *
 * Only ever called from the cue player's single coroutine, one cue at a time,
 * so the tone generator needs no locking.
 */
internal class DeviceCueOutput @Inject constructor(@param:ApplicationContext private val context: Context) :
    CueOutput {
    private var tones: ToneGenerator? = null

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
    }

    override fun play(cue: Cue, sound: Boolean, buzz: Boolean) {
        // Starts the tone and returns: the sound plays while the player waits for the next cue.
        if (sound) (tones ?: newTones().also { tones = it })?.startTone(cue.tone, cue.millis)
        if (buzz) vibrator?.takeIf { it.hasVibrator() }?.vibrate(effectFor(cue.buzz))
    }

    override fun release() {
        tones?.release()
        tones = null
    }
}

/** The system's own tick and clicks, which feel the same as the rest of the phone. */
private fun effectFor(buzz: Buzz): VibrationEffect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    VibrationEffect.createPredefined(
        when (buzz) {
            Buzz.TICK -> VibrationEffect.EFFECT_TICK
            Buzz.CLICK -> VibrationEffect.EFFECT_CLICK
            Buzz.HEAVY -> VibrationEffect.EFFECT_HEAVY_CLICK
        },
    )
} else {
    VibrationEffect.createOneShot(OLD_BUZZ_MILLIS, VibrationEffect.DEFAULT_AMPLITUDE)
}

/**
 * A tone generator on the media stream, or null if the device can't make one.
 * Sound is a nicety here: the timers still show and buzz without it.
 */
// ToneGenerator's constructor throws a bare RuntimeException when the audio
// system can't start it, and the only sensible response is to go quiet.
@Suppress("TooGenericExceptionCaught", "SwallowedException")
private fun newTones(): ToneGenerator? = try {
    ToneGenerator(AudioManager.STREAM_MUSIC, TONE_VOLUME)
} catch (e: RuntimeException) {
    null
}

/** Names the wake lock in battery stats and Play's vitals, so its cost can be found. */
private const val WAKE_LOCK_TAG = "levitx:workout_cues"

/**
 * A partial wake lock: the processor stays on, the screen can go off.
 *
 * With the screen off a phone puts its processor to sleep, and a timer then
 * fires late, so a rest beep could come long after rest ran out. Every hold
 * has a time limit, so the lock is let go even if the player never gets to.
 * ADR 0008 explains why this was chosen over an exact alarm.
 */
internal class WakeLockKeepAwake @Inject constructor(@param:ApplicationContext private val context: Context) :
    KeepAwake {
    private val lock: PowerManager.WakeLock? by lazy {
        context.getSystemService(PowerManager::class.java)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            // One lock whose time limit each hold replaces, rather than a count of holds.
            ?.apply { setReferenceCounted(false) }
    }

    override fun holdFor(millis: Long) {
        lock?.acquire(millis)
    }

    override fun release() {
        lock?.takeIf { it.isHeld }?.release()
    }
}
