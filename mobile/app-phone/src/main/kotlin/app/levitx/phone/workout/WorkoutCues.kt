package app.levitx.phone.workout

import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/*
 * What beeps and buzzes when, for the timers and rest. The phone is usually on
 * the floor or a bench during a set, out of reach and often out of sight, so
 * sound carries the timing and the buzz is a bonus for a phone in a pocket.
 *
 * These are only the rules. WorkoutCuer in the live package plays them, on
 * the device's clock, whether the screen is on or not.
 */

private const val SECONDS_PER_BEEP = 5

/** Beeps in the last seconds of rest, before the one at zero. */
internal const val REST_COUNTDOWN_SECONDS = 3

/* Tone lengths, in milliseconds: short for counting, long for "go". */
private const val SHORT_TONE = 120
private const val LONG_TONE = 450
private const val TICK_TONE = 60
private const val PROMPT_TONE = 200
private const val DOUBLE_TONE = 300
private const val DONE_TONE = 400

/** How strong a buzz is. */
internal enum class Buzz {
    /** A light tick, for counting. */
    TICK,

    /** A firm click, for a new phase. */
    CLICK,

    /** A heavy click, for go and for done. */
    HEAVY,
}

/** A sound with the buzz that goes with it. */
internal enum class Cue(val tone: Int, val millis: Int, val buzz: Buzz) {
    /** A count-in second. */
    COUNT(ToneGenerator.TONE_PROP_BEEP, SHORT_TONE, Buzz.CLICK),

    /** Go: the hold starts, or get to the top. */
    GO(ToneGenerator.TONE_PROP_BEEP, LONG_TONE, Buzz.HEAVY),

    /** A second passing while lowering, or every few seconds of a hold. */
    TICK(ToneGenerator.TONE_PROP_BEEP, TICK_TONE, Buzz.TICK),

    /** Start lowering. */
    LOWER(ToneGenerator.TONE_PROP_PROMPT, PROMPT_TONE, Buzz.CLICK),

    /** The hold reached its target. */
    TARGET(ToneGenerator.TONE_PROP_BEEP2, DOUBLE_TONE, Buzz.HEAVY),

    /** Every rep paced, or a gate passed. */
    DONE(ToneGenerator.TONE_PROP_ACK, DONE_TONE, Buzz.HEAVY),
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

private const val MILLIS_PER_SECOND = 1_000L

/** Whole seconds of getting back to the top, as the countdown starts. */
private val TOP_SECONDS = ceilSeconds(TO_TOP_MILLIS)
