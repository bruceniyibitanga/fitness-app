package app.levitx.phone.workout

import org.junit.Assert.assertEquals
import org.junit.Test

class SetTimerTest {
    private val hold = SetTimer(TimerKind.HOLD, startsAtElapsedMillis = 10_000)
    private val negatives = SetTimer(TimerKind.NEGATIVE, startsAtElapsedMillis = 10_000, lowerMillis = 5_000, reps = 3)

    @Test
    fun theCountInReadsThreeTwoOneAndNeverZero() {
        assertEquals(TimerPhase.LeadIn(3), hold.phaseAt(7_000))
        assertEquals(TimerPhase.LeadIn(3), hold.phaseAt(7_001))
        assertEquals(TimerPhase.LeadIn(1), hold.phaseAt(9_999))
        assertEquals(0, hold.countAt(9_999))
    }

    @Test
    fun aHoldCountsWholeSecondsFromTheEndOfTheCountIn() {
        assertEquals(TimerPhase.Holding(0), hold.phaseAt(10_000))
        assertEquals(TimerPhase.Holding(18), hold.phaseAt(28_999))
        assertEquals(18, hold.countAt(28_999))
    }

    @Test
    fun eachNegativeIsTheWayUpThenTheLowering() {
        assertEquals(TimerPhase.ToTop(rep = 1, millisLeft = 4_000), negatives.phaseAt(10_000))
        assertEquals(4, (negatives.phaseAt(10_000) as TimerPhase.ToTop).secondsLeft)
        assertEquals(TimerPhase.Lowering(rep = 1, millisIn = 0, millisTotal = 5_000), negatives.phaseAt(14_000))
        // Rep 2, three seconds into its lowering.
        assertEquals(TimerPhase.Lowering(rep = 2, millisIn = 3_000, millisTotal = 5_000), negatives.phaseAt(30_000))
    }

    @Test
    fun onlyFullyLoweredRepsCount() {
        assertEquals(0, negatives.countAt(18_999))
        assertEquals(1, negatives.countAt(19_000))
        assertEquals(1, negatives.countAt(30_000))
    }

    @Test
    fun negativesFinishAfterTheLastRep() {
        assertEquals(TimerPhase.Done(3), negatives.phaseAt(10_000 + 3 * 9_000))
        assertEquals(3, negatives.countAt(100_000))
    }
}
