package app.levitx.core.intervals

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IntervalCueTest {
    private val start = IntervalRun(IntervalTimeline.of(checkNotNull(IntervalCatalogue.byId("tabata"))))

    @Test
    fun theLastThreeSecondsOfAStepCountDown() {
        val early = start.tick(IntervalTimeline.PREPARE_SECONDS - 4)
        assertNull(early.cueFrom(start), "4 s left is quiet")
        assertEquals(IntervalCue.COUNT, early.tick(1).cueFrom(early))
    }

    @Test
    fun workGetsGoAndRestGetsAChange() {
        val ready = start.tick(IntervalTimeline.PREPARE_SECONDS - 1)
        assertEquals(IntervalCue.GO, ready.tick(1).cueFrom(ready))
        val working = ready.tick(1)
        assertEquals(IntervalCue.CHANGE, working.skip().cueFrom(working))
    }

    @Test
    fun aLateTickPlaysOnlyTheNewestCue() {
        // Jumping from 3 s before the end of get-ready to 2 s into work: only GO.
        val ready = start.tick(IntervalTimeline.PREPARE_SECONDS - 3)
        assertEquals(IntervalCue.GO, ready.tick(5).cueFrom(ready))
    }

    @Test
    fun finishingCuesOnce() {
        val finished = start.tick(Int.MAX_VALUE / 2)
        assertEquals(IntervalCue.DONE, finished.cueFrom(start))
        assertNull(finished.tick(1).cueFrom(finished))
    }
}
