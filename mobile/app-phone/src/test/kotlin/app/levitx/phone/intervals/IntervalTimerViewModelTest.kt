package app.levitx.phone.intervals

import app.levitx.core.intervals.IntervalCatalogue
import app.levitx.core.intervals.IntervalPhase
import app.levitx.core.intervals.IntervalTimeline
import app.levitx.data.settings.AppSettings
import app.levitx.data.time.TrainingClock
import app.levitx.phone.MainDispatcherRule
import app.levitx.phone.live.CueOutput
import app.levitx.phone.live.KeepAwake
import app.levitx.phone.live.LiveWorkout
import app.levitx.phone.workout.Cue
import app.levitx.testing.FakeProgressRepository
import app.levitx.testing.FakeSessionRepository
import app.levitx.testing.FakeSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IntervalTimerViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(dispatcher)

    private val sessions = FakeSessionRepository(FakeProgressRepository())
    private val settings = FakeSettingsRepository(AppSettings(sounds = true, buzzes = false))
    private val played = mutableListOf<Cue>()
    private var awake = false
    private val live = LiveWorkout()

    private val output = object : CueOutput {
        override fun play(cue: Cue, sound: Boolean, buzz: Boolean) {
            check(sound && !buzz) { "cues follow the settings" }
            played += cue
        }

        override fun release() = Unit
    }

    private val keepAwake = object : KeepAwake {
        override fun holdFor(millis: Long) {
            awake = true
        }

        override fun release() {
            awake = false
        }
    }

    /** A timer for [workoutId] on the test's clock, so its waits take no real time. */
    private fun TestScope.timer(workoutId: String = "tabata"): IntervalTimerViewModel {
        val clock = object : TrainingClock {
            override fun elapsedRealtimeMillis(): Long = testScheduler.currentTime

            override fun currentTimeMillis(): Long = WALL_CLOCK + testScheduler.currentTime
        }
        val viewModel = IntervalTimerViewModel(
            workoutId,
            sessions,
            settings,
            clock,
            IntervalSignals(output, keepAwake, live),
            backgroundScope,
        )
        runCurrent()
        return viewModel
    }

    /** Moves the test clock on by [seconds] and runs whatever is due. */
    private fun TestScope.wait(seconds: Int) {
        advanceTimeBy(seconds * MILLIS_PER_SECOND)
        runCurrent()
    }

    private val IntervalTimerViewModel.running: IntervalTimerUiState.Running
        get() = uiState.value as IntervalTimerUiState.Running

    @Test
    fun anUnknownWorkoutIsNotFound() = runTest(dispatcher) {
        assertEquals(IntervalTimerUiState.NotFound, timer("nope").uiState.value)
    }

    @Test
    fun theClockCountsTheGetReadyStepThenTheFirstWork() = runTest(dispatcher) {
        val viewModel = timer()
        assertTrue("the processor is held while the timer runs", awake)
        wait(IntervalTimeline.PREPARE_SECONDS + 5)
        val run = viewModel.running.run
        assertEquals(IntervalPhase.WORK, run.current?.phase)
        assertEquals("40 s of jumping jacks, 5 done", 35, run.remaining)
        assertEquals("a countdown into the work, then GO", listOf(Cue.COUNT, Cue.COUNT, Cue.COUNT, Cue.GO), played)
        viewModel.end()
    }

    @Test
    fun theWorkoutIsShownAsInProgressStepByStepAndNoLongerOnceItEnds() = runTest(dispatcher) {
        val viewModel = timer()
        // Shown from the start: this is what keeps the timer going with another app in front.
        val ready = checkNotNull(live.intervals.value)
        assertEquals("Tabata", ready.workoutName)
        assertEquals(IntervalPhase.PREPARE, ready.phase)

        wait(IntervalTimeline.PREPARE_SECONDS)
        assertEquals(IntervalPhase.WORK, live.intervals.value?.phase)
        viewModel.togglePause()
        assertEquals(true, live.intervals.value?.paused)

        viewModel.end()
        assertNull(live.intervals.value)
    }

    @Test
    fun pausedTimeNeverCounts() = runTest(dispatcher) {
        val viewModel = timer()
        wait(2)
        viewModel.togglePause()
        assertFalse("a paused timer lets the processor sleep", awake)
        wait(60)
        assertEquals(IntervalTimeline.PREPARE_SECONDS - 2, viewModel.running.run.remaining)
        viewModel.togglePause()
        wait(1)
        assertEquals(IntervalTimeline.PREPARE_SECONDS - 3, viewModel.running.run.remaining)
        viewModel.end()
    }

    @Test
    fun endingEarlyKeepsOnlyTheWorkDone() = runTest(dispatcher) {
        val viewModel = timer()
        wait(IntervalTimeline.PREPARE_SECONDS + 5)
        viewModel.end()
        runCurrent()

        val done = viewModel.uiState.value as IntervalTimerUiState.Done
        assertEquals(5, done.workedSeconds)
        assertEquals(SaveStatus.SAVED, done.save)
        assertFalse(awake)
        val stored = sessions.stored.single()
        assertEquals("cardio.jumping_jacks", stored.session.gateId)
        assertEquals(listOf(5), stored.session.sets.map { it.value })
        assertTrue("one workout in the history, under one id", stored.id.endsWith("/0"))
    }

    @Test
    fun endingBeforeAnyWorkSavesNothing() = runTest(dispatcher) {
        val viewModel = timer()
        wait(3)
        viewModel.end()
        runCurrent()
        assertEquals(SaveStatus.NOTHING, (viewModel.uiState.value as IntervalTimerUiState.Done).save)
        assertTrue(sessions.stored.isEmpty())
    }

    @Test
    fun aWholeWorkoutRunsToTheEndAndIsSavedAsOneWorkout() = runTest(dispatcher) {
        val workout = checkNotNull(IntervalCatalogue.byId("tabata"))
        val viewModel = timer()
        wait(IntervalTimeline.of(workout).sumOf { it.seconds })

        val done = viewModel.uiState.value as IntervalTimerUiState.Done
        assertEquals(workout.blocks.sumOf { it.workSeconds * it.rounds }, done.workedSeconds)
        assertEquals(SaveStatus.SAVED, done.save)
        assertEquals(Cue.DONE, played.last())
        val ids = sessions.stored.map { it.id.substringBefore('/') }.toSet()
        assertEquals("every move under the same workout id", 1, ids.size)
        assertEquals(workout.blocks.map { it.move.id }, sessions.stored.map { it.session.gateId })
    }

    @Test
    fun skipEndsTheStepUnderWay() = runTest(dispatcher) {
        val viewModel = timer()
        viewModel.skip()
        assertEquals(IntervalPhase.WORK, viewModel.running.run.current?.phase)
        assertEquals(Cue.GO, played.last())
        viewModel.end()
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
        const val WALL_CLOCK = 1_700_000_000_000L
    }
}
