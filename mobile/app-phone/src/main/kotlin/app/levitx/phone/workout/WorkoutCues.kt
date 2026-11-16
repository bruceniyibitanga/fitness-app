package app.levitx.phone.workout

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/*
 * Beeps and buzzes for the timers and rest. The phone is usually on the floor
 * or a bench during a set, out of reach and often out of sight, so sound
 * carries the timing and the buzz is a bonus for a phone in a pocket.
 */

/** Beep volume, as a percentage of the media volume, so it follows the person's own setting. */
private const val TONE_VOLUME = 80

private const val SECONDS_PER_BEEP = 5

/** Beeps in the last seconds of rest, before the one at zero. */
private const val REST_COUNTDOWN_SECONDS = 3

/* Tone lengths, in milliseconds: short for counting, long for "go". */
private const val SHORT_TONE = 120
private const val LONG_TONE = 450
private const val TICK_TONE = 60
private const val PROMPT_TONE = 200
private const val DOUBLE_TONE = 300
private const val DONE_TONE = 400

/** A sound with the buzz that goes with it. */
internal enum class Cue(val tone: Int, val millis: Int, val haptic: HapticFeedbackType) {
    /** A count-in second. */
    COUNT(ToneGenerator.TONE_PROP_BEEP, SHORT_TONE, HapticFeedbackType.SegmentTick),

    /** Go: the hold starts, or get to the top. */
    GO(ToneGenerator.TONE_PROP_BEEP, LONG_TONE, HapticFeedbackType.Confirm),

    /** A second passing while lowering, or every few seconds of a hold. */
    TICK(ToneGenerator.TONE_PROP_BEEP, TICK_TONE, HapticFeedbackType.SegmentFrequentTick),

    /** Start lowering. */
    LOWER(ToneGenerator.TONE_PROP_PROMPT, PROMPT_TONE, HapticFeedbackType.SegmentTick),

    /** The hold reached its target. */
    TARGET(ToneGenerator.TONE_PROP_BEEP2, DOUBLE_TONE, HapticFeedbackType.Confirm),

    /** Every rep paced. */
    DONE(ToneGenerator.TONE_PROP_ACK, DONE_TONE, HapticFeedbackType.Confirm),
}

/**
 * One moment worth a cue. A new moment (a different second, rep or phase)
 * plays its [cue] once; the same moment again, on a redraw, doesn't.
 */
internal data class Beat(val phase: String, val rep: Int, val second: Int, val cue: Cue?)

/** The beat for a timer at [phase], with [target] seconds to hold. */
internal fun timerBeat(phase: TimerPhase, target: Int): Beat = when (phase) {
    is TimerPhase.LeadIn -> Beat("lead", 0, phase.secondsLeft, Cue.COUNT)

    is TimerPhase.Holding -> Beat(
        "hold",
        0,
        phase.seconds,
        when {
            phase.seconds == 0 -> Cue.GO
            phase.seconds == target -> Cue.TARGET
            phase.seconds % SECONDS_PER_BEEP == 0 -> Cue.TICK
            else -> null
        },
    )

    // One beep as each rep begins: back to the top.
    is TimerPhase.ToTop -> Beat("top", phase.rep, phase.secondsLeft, Cue.GO.takeIf { phase.secondsLeft == TOP_SECONDS })

    is TimerPhase.Lowering -> {
        val second = (phase.millisIn / MILLIS_PER_SECOND).toInt()
        Beat("lower", phase.rep, second, if (second == 0) Cue.LOWER else Cue.TICK)
    }

    is TimerPhase.Done -> Beat("done", phase.reps, 0, Cue.DONE)
}

/** The beat for rest with [secondsLeft]: a count in the last three seconds, and go at zero. */
internal fun restBeat(secondsLeft: Int): Beat = Beat(
    "rest",
    0,
    secondsLeft.coerceAtLeast(0),
    when (secondsLeft) {
        in 1..REST_COUNTDOWN_SECONDS -> Cue.COUNT
        0 -> Cue.GO
        else -> null
    },
)

/** Plays [beat]'s cue, once, when [enabled]. */
@Composable
internal fun PlayCue(beat: Beat, enabled: Boolean) {
    val haptics = LocalHapticFeedback.current
    val tones by rememberTones(enabled)
    LaunchedEffect(beat) {
        val cue = beat.cue
        if (enabled && cue != null) {
            haptics.performHapticFeedback(cue.haptic)
            // Starts the tone and returns: the sound plays without holding up the screen.
            tones?.startTone(cue.tone, cue.millis)
        }
    }
}

/**
 * The device's elapsed clock, read every [tickMillis] while on screen. Timers
 * work from this, never by adding up ticks, so they stay right if a frame is
 * late or the app was in the background.
 */
@Composable
internal fun rememberElapsedNow(now: () -> Long, tickMillis: Long): Long {
    var millis by remember { mutableLongStateOf(now()) }
    LaunchedEffect(now, tickMillis) {
        while (true) {
            millis = now()
            delay(tickMillis)
        }
    }
    return millis
}

/**
 * A tone generator while [enabled], or null. Made off the main thread, since
 * it opens an audio track, and released when it leaves the screen.
 */
@Composable
private fun rememberTones(enabled: Boolean): State<ToneGenerator?> = produceState<ToneGenerator?>(null, enabled) {
    val tones = if (enabled) withContext(Dispatchers.Default) { newTones() } else null
    value = tones
    awaitDispose { tones?.release() }
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

private const val MILLIS_PER_SECOND = 1_000L

/** Whole seconds of getting back to the top, as the countdown starts. */
private val TOP_SECONDS = ceilSeconds(TO_TOP_MILLIS)
