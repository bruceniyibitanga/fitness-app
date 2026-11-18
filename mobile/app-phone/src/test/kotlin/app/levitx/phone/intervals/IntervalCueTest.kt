package app.levitx.phone.intervals

import app.levitx.core.intervals.IntervalCatalogue
import app.levitx.core.intervals.IntervalRun
import app.levitx.core.intervals.IntervalTimeline
import app.levitx.phone.workout.Cue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The phone's sound for each cue. When cues happen is tested in core. */
class IntervalCueTest {
    private val start = IntervalRun(IntervalTimeline.of(checkNotNull(IntervalCatalogue.byId("tabata"))))

    @Test
    fun eachCueHasItsSound() {
        val ready = start.tick(IntervalTimeline.PREPARE_SECONDS - 1)
        val working = ready.tick(1)
        val early = start.tick(IntervalTimeline.PREPARE_SECONDS - 4)
        assertEquals(Cue.COUNT, intervalCue(early, early.tick(1)))
        assertEquals(Cue.GO, intervalCue(ready, working))
        assertEquals(Cue.TARGET, intervalCue(working, working.skip()))
        assertEquals(Cue.DONE, intervalCue(start, start.tick(Int.MAX_VALUE / 2)))
        assertNull(intervalCue(start, start))
    }
}
