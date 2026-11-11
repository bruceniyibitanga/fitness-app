package app.levitx.core.coach

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BodyweightTrendTest {
    private fun readings(vararg dayToKg: Pair<Int, Double>) = dayToKg.map { WeighIn(atDay(it.first), it.second) }

    @Test
    fun noReadingsMeansNoTrend() {
        assertNull(bodyweightTrend(emptyList(), atDay(10)))
    }

    @Test
    fun currentWeightIsTheAverageOfTheLastWeek() {
        val trend = assertNotNull(bodyweightTrend(readings(1 to 80.0, 18 to 74.0, 20 to 75.0), atDay(20)))
        assertEquals(74.5, trend.currentKg)
    }

    @Test
    fun anOldReadingIsStillBetterThanNone() {
        val trend = assertNotNull(bodyweightTrend(readings(1 to 80.0), atDay(40)))
        assertEquals(80.0, trend.currentKg)
        assertNull(trend.weeklyChange)
    }

    @Test
    fun twoReadingsAreNotATrend() {
        val trend = assertNotNull(bodyweightTrend(readings(0 to 80.0, 14 to 77.0), atDay(14)))
        assertNull(trend.weeklyChange)
        assertFalse(trend.isFastLoss)
    }

    @Test
    fun readingsOverAFewDaysAreNotATrend() {
        val trend = assertNotNull(bodyweightTrend(readings(10 to 80.0, 12 to 79.0, 14 to 78.0), atDay(14)))
        assertNull(trend.weeklyChange)
    }

    @Test
    fun losingMoreThanOnePercentAWeekForTwoWeeksIsFast() {
        // 80 kg falling 1.2 kg a week is 1.5% a week.
        val trend = assertNotNull(bodyweightTrend(readings(0 to 80.0, 7 to 78.8, 14 to 77.6), atDay(14)))
        assertTrue(trend.isFastLoss)
        assertEquals(-0.015, assertNotNull(trend.weeklyChange), absoluteTolerance = 0.001)
    }

    @Test
    fun steadyLossInsideTheGuidanceIsNotFast() {
        // 0.4 kg a week from 80 kg is 0.5% a week.
        val trend = assertNotNull(bodyweightTrend(readings(0 to 80.0, 7 to 79.6, 14 to 79.2), atDay(14)))
        assertFalse(trend.isFastLoss)
    }

    @Test
    fun aFastDropOverLessThanTwoWeeksIsNotFlaggedYet() {
        val trend = assertNotNull(bodyweightTrend(readings(0 to 80.0, 5 to 79.0, 11 to 78.0), atDay(11)))
        assertNotNull(trend.weeklyChange)
        assertFalse(trend.isFastLoss)
    }

    @Test
    fun oneLowReadingDoesNotMakeATrend() {
        val trend = assertNotNull(
            bodyweightTrend(readings(0 to 80.0, 4 to 80.2, 8 to 79.9, 12 to 80.1, 14 to 78.6), atDay(14)),
        )
        assertFalse(trend.isFastLoss)
    }

    @Test
    fun readingsFromTheFutureAreIgnored() {
        val trend = assertNotNull(bodyweightTrend(readings(0 to 80.0, 30 to 60.0), atDay(1)))
        assertEquals(80.0, trend.currentKg)
    }

    @Test
    fun impossibleWeightsAreRefused() {
        assertFailsWith<IllegalArgumentException> { WeighIn(atDay(0), 7.0) }
    }
}
