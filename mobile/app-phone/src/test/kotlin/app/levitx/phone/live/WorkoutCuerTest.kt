package app.levitx.phone.live

import app.levitx.data.time.TrainingClock
import app.levitx.phone.workout.Cue
import app.levitx.phone.workout.SetTimer
import app.levitx.phone.workout.TimerKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutCuerTest {
    private val live = LiveWorkout()
    private val owner = Any()

    /** Cues played, as (milliseconds since the test started, cue). */
    private val played = mutableListOf<Pair<Long, Cue>>()
    private var released = 0
    private var awake = false

    private val session = LiveSession(
        exerciseName = "Pull-up",
        exerciseNumber = 1,
        exerciseCount = 2,
        setNumber = 2,
        setCount = 3,
        setsPlanned = listOf(3, 2),
        setsBehind = 1,
    )

    /** Starts a cue player on the test's clock, so its waits take no real time. */
    private fun TestScope.startCuer() {
        val clock = object : TrainingClock {
            override fun elapsedRealtimeMillis(): Long = START + testScheduler.currentTime

            override fun currentTimeMillis(): Long = testScheduler.currentTime
        }
        val output = object : CueOutput {
            override fun play(cue: Cue, sound: Boolean, buzz: Boolean) {
                played += testScheduler.currentTime to cue
            }

            override fun release() {
                released++
            }
        }
        val keepAwake = object : KeepAwake {
            override fun holdFor(millis: Long) {
                awake = true
            }

            override fun release() {
                awake = false
            }
        }
        val cuer = WorkoutCuer(live, clock, output, keepAwake, backgroundScope)
        backgroundScope.launch { cuer.follow() }
        runCurrent()
    }

    @Test
    fun restCountsDownTheLastThreeSecondsAndSaysGoAtZero() = runTest {
        startCuer()
        live.publish(owner, session.copy(restEndsAtElapsedMillis = START + 10_000))
        advanceTimeBy(10_001)
        assertEquals(
            listOf(7_000L to Cue.COUNT, 8_000L to Cue.COUNT, 9_000L to Cue.COUNT, 10_000L to Cue.GO),
            played,
        )
        assertFalse(awake)
    }

    @Test
    fun aHoldCountsInGoesAndMarksEveryFiveSecondsAndTheTarget() = runTest {
        startCuer()
        val hold = SetTimer(TimerKind.HOLD, startsAtElapsedMillis = START + 3_000)
        live.publish(owner, session.copy(timer = hold, target = 10))
        advanceTimeBy(13_001)
        assertEquals(
            listOf(
                0L to Cue.COUNT,
                1_000L to Cue.COUNT,
                2_000L to Cue.COUNT,
                3_000L to Cue.GO,
                8_000L to Cue.TICK,
                13_000L to Cue.TARGET,
            ),
            played,
        )
        live.publish(owner, session)
        runCurrent()
        assertFalse(awake)
    }

    @Test
    fun turningSoundsOffDoesNotPlayTheSameBeatAgain() = runTest {
        startCuer()
        val resting = session.copy(restEndsAtElapsedMillis = START + 2_500)
        live.publish(owner, resting)
        runCurrent()
        live.publish(owner, resting.copy(sounds = false))
        runCurrent()
        assertEquals(listOf(0L to Cue.COUNT), played)
    }

    @Test
    fun twoRestsInARowEachSayGo() = runTest {
        startCuer()
        live.publish(owner, session.copy(restEndsAtElapsedMillis = START))
        runCurrent()
        live.publish(owner, session)
        advanceTimeBy(5_000)
        live.publish(owner, session.copy(restEndsAtElapsedMillis = START + 5_000))
        runCurrent()
        assertEquals(listOf(0L to Cue.GO, 5_000L to Cue.GO), played)
    }

    @Test
    fun aRestThatRanOutLongAgoSaysNothing() = runTest {
        startCuer()
        advanceTimeBy(60_000)
        live.publish(owner, session.copy(restEndsAtElapsedMillis = START))
        runCurrent()
        assertEquals(emptyList<Pair<Long, Cue>>(), played)
    }

    @Test
    fun aGatePassIsCheeredOnce() = runTest {
        startCuer()
        val passed = session.copy(passedGate = "first_pull_up.scapular_pulls")
        live.publish(owner, passed)
        runCurrent()
        live.publish(owner, passed.copy(buzzes = false))
        runCurrent()
        assertEquals(listOf(0L to Cue.DONE), played)
    }

    @Test
    fun theSpeakerIsLetGoWhenTheWorkoutEnds() = runTest {
        startCuer()
        live.publish(owner, session)
        runCurrent()
        val before = released
        live.clear(owner)
        runCurrent()
        assertEquals(before + 1, released)
    }

    private companion object {
        /** The elapsed clock when each test starts. Any time after boot. */
        const val START = 50_000L
    }
}
