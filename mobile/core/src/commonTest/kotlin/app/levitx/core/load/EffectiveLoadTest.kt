package app.levitx.core.load

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EffectiveLoadTest {
    @Test
    fun regularPushUpMoves64PercentOfBodyweight() {
        val load = calculateEffectiveLoad(bodyweightKg = 74.0, profile = LoadCatalogue.pushUp)
        assertEquals(47.36, load.totalKg, absoluteTolerance = 0.001)
        assertFalse(load.isEstimate)
    }

    @Test
    fun addedWeightCountsAtTheSameShareAsBodyweight() {
        val load = calculateEffectiveLoad(
            bodyweightKg = 74.0,
            profile = LoadCatalogue.pushUp,
            modifiers = listOf(AddedWeight(10.0)),
        )
        assertEquals(6.4, load.fromAddedWeightKg, absoluteTolerance = 0.001)
        assertEquals(53.76, load.totalKg, absoluteTolerance = 0.001)
    }

    @Test
    fun weightedPullUpAddsTheFullBeltWeight() {
        // 95% of 74 kg is 70.3 kg of body, plus the whole belt.
        val load = calculateEffectiveLoad(
            bodyweightKg = 74.0,
            profile = LoadCatalogue.pullUp,
            modifiers = listOf(AddedWeight(5.0)),
        )
        assertEquals(5.0, load.fromAddedWeightKg, absoluteTolerance = 0.001)
        assertEquals(75.3, load.totalKg, absoluteTolerance = 0.001)
        assertTrue(load.isEstimate)
    }

    @Test
    fun bandAssistanceIsSubtractedInFull() {
        val load = calculateEffectiveLoad(
            bodyweightKg = 80.0,
            profile = LoadCatalogue.pullUp,
            modifiers = listOf(BandAssistance(20.0)),
        )
        // 95% of 80 kg is 76 kg, less the band's 20.
        assertEquals(56.0, load.totalKg, absoluteTolerance = 0.001)
    }

    @Test
    fun totalNeverGoesBelowZero() {
        val load = calculateEffectiveLoad(
            bodyweightKg = 40.0,
            profile = LoadCatalogue.pushUpHandsOn61cmBox,
            modifiers = listOf(BandAssistance(100.0)),
        )
        assertEquals(0.0, load.totalKg)
    }

    @Test
    fun pushUpVariationsGetHeavierInCatalogueOrder() {
        val totals = LoadCatalogue.pushUps.map { calculateEffectiveLoad(80.0, it).totalKg }
        totals.zipWithNext().forEach { (easier, harder) -> assertTrue(harder > easier) }
    }

    @Test
    fun rejectsBodyweightOutsideTheLimits() {
        assertFailsWith<IllegalArgumentException> { calculateEffectiveLoad(7.0, LoadCatalogue.pushUp) }
        assertFailsWith<IllegalArgumentException> { calculateEffectiveLoad(Double.NaN, LoadCatalogue.pushUp) }
    }

    @Test
    fun rejectsNegativeModifier() {
        assertFailsWith<IllegalArgumentException> {
            calculateEffectiveLoad(74.0, LoadCatalogue.pushUp, listOf(AddedWeight(-5.0)))
        }
    }

    @Test
    fun errorMessageDoesNotContainTheValue() {
        val error = assertFailsWith<IllegalArgumentException> { calculateEffectiveLoad(7.25, LoadCatalogue.pushUp) }
        assertFalse(error.message.orEmpty().contains("7.25"))
    }
}
