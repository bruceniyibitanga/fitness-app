package app.levitx.wear.intervals

import app.levitx.core.intervals.IntervalCue
import app.levitx.core.intervals.IntervalPhase
import app.levitx.core.intervals.IntervalTimeline
import app.levitx.wear.WatchFakes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IntervalControllerTest {
    private val fakes = WatchFakes()
    private val clock = fakes.clock
    private val controller = fakes.intervals()

    private fun running(): IntervalState.Running = controller.state.value as IntervalState.Running

    /** Lets [seconds] pass on the elapsed clock, then ticks. */
    private fun pass(seconds: Int) {
        clock.elapsed += seconds * 1_000L
        controller.tick()
    }

    @Test
    fun startsWithTimeToGetReadyAndKeepsTheWatchAwake() {
        controller.start("tabata")

        assertEquals("Tabata", running().workout.name)
        assertEquals(IntervalPhase.PREPARE, running().run.current?.phase)
        assertEquals(IntervalTimeline.PREPARE_SECONDS, running().run.remaining)
        assertTrue(fakes.signals.isAwake)
    }

    @Test
    fun countsDownWithTheClockAndBuzzesTheLastSecondsThenGo() {
        controller.start("tabata")

        pass(IntervalTimeline.PREPARE_SECONDS - 1)
        assertEquals(1, running().run.remaining)
        assertEquals(listOf(IntervalCue.COUNT), fakes.signals.played)

        pass(1)
        assertEquals(IntervalPhase.WORK, running().run.current?.phase)
        assertEquals(IntervalCue.GO, fakes.signals.played.last())
    }

    @Test
    fun partSecondsAreKeptForTheNextTick() {
        controller.start("tabata")

        clock.elapsed += 1_600
        controller.tick()
        clock.elapsed += 600
        controller.tick()

        assertEquals(IntervalTimeline.PREPARE_SECONDS - 2, running().run.remaining)
    }

    @Test
    fun timePausedNeverCountsAndTheWatchMaySleep() {
        controller.start("tabata")
        controller.togglePause()

        pass(30)
        assertEquals(IntervalTimeline.PREPARE_SECONDS, running().run.remaining)
        assertFalse(fakes.signals.isAwake)

        controller.togglePause()
        pass(2)
        assertEquals(IntervalTimeline.PREPARE_SECONDS - 2, running().run.remaining)
        assertTrue(fakes.signals.isAwake)
    }

    @Test
    fun endingKeepsTheWorkDoneAndQueuesItForThePhone() {
        controller.start("tabata")
        controller.skip()
        pass(15)
        controller.togglePause()
        controller.end()

        val done = controller.state.value as IntervalState.Done
        assertEquals(15, done.workedSeconds)
        assertEquals(IntervalSave.SAVED, done.save)
        val queued = fakes.intervalOutbox.current.single()
        assertEquals("cardio.jumping_jacks", queued.sessions.single().gateId)
        assertEquals(listOf(15), queued.sessions.single().sets.map { it.value })
        assertFalse(fakes.signals.isAwake)
    }

    @Test
    fun endingBeforeAnyWorkSavesNothing() {
        controller.start("tabata")
        controller.end()

        assertEquals(IntervalSave.NOTHING, (controller.state.value as IntervalState.Done).save)
        assertTrue(fakes.intervalOutbox.current.isEmpty())
    }

    @Test
    fun theWholeWorkoutEndsWithTheDoneBuzz() {
        controller.start("shoulders_and_wrists")
        while (controller.state.value is IntervalState.Running) {
            pass(running().run.remaining)
        }

        assertEquals(IntervalCue.DONE, fakes.signals.played.last())
        assertEquals(IntervalSave.SAVED, (controller.state.value as IntervalState.Done).save)
    }

    @Test
    fun aSecondStartWhileOneIsRunningIsIgnored() {
        controller.start("tabata")
        controller.start("quiet_cardio")

        assertEquals("Tabata", running().workout.name)
    }

    @Test
    fun anUnknownWorkoutDoesNothingAndDoneClearsBackToIdle() {
        controller.start("no_such_workout")
        assertEquals(IntervalState.Idle, controller.state.value)

        controller.start("tabata")
        controller.end()
        controller.dismiss()
        assertEquals(IntervalState.Idle, controller.state.value)
    }
}
