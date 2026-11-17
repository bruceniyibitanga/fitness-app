package app.levitx.phone.intervals

import app.levitx.core.intervals.IntervalCatalogue
import app.levitx.core.intervals.IntervalRun
import app.levitx.core.intervals.IntervalTimeline
import app.levitx.phone.workout.Cue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IntervalCueTest {
    private val start = IntervalRun(IntervalTimeline.of(checkNotNull(IntervalCatalogue.byId("tabata"))))

    @Test
    fun theLastThreeSecondsOfAStepCountDown() {
        val early = start.tick(IntervalTimeline.PREPARE_SECONDS - 4)
        assertNull("4 s left is quiet", intervalCue(start, early))
        assertEquals(Cue.COUNT, intervalCue(early, early.tick(1)))
    }

    @Test
    fun workGetsGoAndRestGetsTheDoubleBeep() {
        val ready = start.tick(IntervalTimeline.PREPARE_SECONDS - 1)
        assertEquals(Cue.GO, intervalCue(ready, ready.tick(1)))
        val working = ready.tick(1)
        assertEquals(Cue.TARGET, intervalCue(working, working.skip()))
    }

    @Test
    fun aLateTickPlaysOnlyTheNewestCue() {
        // Jumping from 3 s before the end of get-ready to 2 s into work: only GO.
        val ready = start.tick(IntervalTimeline.PREPARE_SECONDS - 3)
        assertEquals(Cue.GO, intervalCue(ready, ready.tick(5)))
    }

    @Test
    fun finishingChimesOnce() {
        val finished = start.tick(Int.MAX_VALUE / 2)
        assertEquals(Cue.DONE, intervalCue(start, finished))
        assertNull(intervalCue(finished, finished.tick(1)))
    }
}
