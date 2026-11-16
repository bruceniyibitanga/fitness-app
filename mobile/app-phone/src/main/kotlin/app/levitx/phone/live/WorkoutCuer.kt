package app.levitx.phone.live

import app.levitx.data.ApplicationScope
import app.levitx.data.time.TrainingClock
import app.levitx.phone.workout.Beat
import app.levitx.phone.workout.Cue
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Extra time on each wake lock hold, so it outlasts the wait it protects. */
private const val AWAKE_MARGIN_MILLIS = 2_000L

/**
 * Beeps and buzzes through a workout, on the device's clock, with the screen
 * on or off.
 *
 * It follows [LiveWorkout] and, for each rest or timer, sleeps until the next
 * second that has a cue, plays it, and goes back to sleep. While a cue is due
 * it keeps the processor awake, so the beep at the end of rest comes on time
 * with the phone locked in a pocket. It holds nothing between workouts.
 *
 * The screen plays nothing itself, so there is one source of sound and
 * nothing is ever played twice.
 */
@Singleton
class WorkoutCuer @Inject internal constructor(
    private val live: LiveWorkout,
    private val clock: TrainingClock,
    private val output: CueOutput,
    private val awake: KeepAwake,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private var job: Job? = null

    /*
     * The last beat played and the track it came from, and the last gate
     * cheered. Only the follow coroutine touches these, one value at a time.
     * They outlive one value of the session so that turning sounds off and on,
     * which restarts the track, doesn't play the same beat again.
     */
    private var lastBeat: Pair<CueTrack, Beat>? = null
    private var lastGate: String? = null

    /** Starts following workouts. Called once, as the app starts; later calls do nothing. */
    fun start() {
        if (job == null) job = scope.launch { follow() }
    }

    /** Plays the cues of every workout published, until cancelled. */
    internal suspend fun follow() {
        live.session
            // Only what the cues use: a new exercise name mustn't restart a rest countdown.
            .map { session -> session?.let { CueInput(cueTrack(it), it.passedGate, it.sounds, it.buzzes) } }
            .distinctUntilChanged()
            // A newer value cancels the wait for the older one's next cue.
            .collectLatest { input ->
                if (input == null) {
                    lastBeat = null
                    lastGate = null
                    output.release()
                } else {
                    play(input)
                }
            }
    }

    private suspend fun play(input: CueInput) {
        input.passedGate?.takeIf { it != lastGate }?.let {
            lastGate = it
            output.play(Cue.DONE, input.sounds, input.buzzes)
        }
        val track = input.track ?: return
        try {
            var next = step(track, input)
            while (next != null) {
                val wait = (next - clock.elapsedRealtimeMillis()).coerceAtLeast(0)
                awake.holdFor(wait + AWAKE_MARGIN_MILLIS)
                delay(wait)
                next = step(track, input)
            }
        } finally {
            // Also runs when a newer value cancels the wait, so the lock is never left held.
            awake.release()
        }
    }

    /** Plays the beat due now, unless it already played, and returns when the next is due, or null when none is. */
    private fun step(track: CueTrack, input: CueInput): Long? {
        val now = clock.elapsedRealtimeMillis()
        val beat = track.beatAt(now) ?: return null
        if (lastBeat != track to beat) {
            lastBeat = track to beat
            beat.cue?.let { output.play(it, input.sounds, input.buzzes) }
        }
        return track.nextChangeAfter(now)
    }

    /**
     * What the cues depend on.
     *
     * @property track The rest or timer being cued, or null for neither.
     * @property passedGate The gate just passed, cheered once.
     * @property sounds True when cues beep.
     * @property buzzes True when cues buzz.
     */
    private data class CueInput(
        val track: CueTrack?,
        val passedGate: String?,
        val sounds: Boolean,
        val buzzes: Boolean,
    )
}
