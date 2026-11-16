package app.levitx.core.load

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LoadKitTest {
    private val vestOnly = setOf(LoadCarrier.VEST)
    private val hanging = setOf(LoadCarrier.VEST, LoadCarrier.BELT)

    @Test
    fun anAdjustableVestGoesUpInItsStepsToItsFullWeight() {
        val kit = LoadKit(vestMaxKg = 10.0, vestStepKg = 2.5)
        assertEquals(listOf(0.0, 2.5, 5.0, 7.5, 10.0), kit.choices(vestOnly))
    }

    @Test
    fun aStepThatDoesNotDivideTheVestStillEndsAtItsFullWeight() {
        val kit = LoadKit(vestMaxKg = 9.0, vestStepKg = 2.0)
        assertEquals(listOf(0.0, 2.0, 4.0, 6.0, 8.0, 9.0), kit.choices(vestOnly))
    }

    @Test
    fun aFixedVestIsAllOrNothing() {
        assertEquals(listOf(0.0, 20.0), LoadKit(vestMaxKg = 20.0).choices(vestOnly))
    }

    @Test
    fun platesMakeEveryTotalTheyCanAddUpTo() {
        val kit = LoadKit(platesKg = listOf(2.5, 2.5, 5.0), hasBelt = true)
        assertEquals(listOf(0.0, 2.5, 5.0, 7.5, 10.0), kit.choices(setOf(LoadCarrier.BELT)))
    }

    @Test
    fun platesNeedABeltToHangFromButNotToRestOnTheHips() {
        val kit = LoadKit(platesKg = listOf(5.0))
        assertEquals(listOf(0.0), kit.choices(setOf(LoadCarrier.BELT)))
        assertEquals(listOf(0.0, 5.0), kit.choices(setOf(LoadCarrier.PLATES)))
    }

    @Test
    fun aVestAndABeltCanBeWornTogether() {
        val kit = LoadKit(vestMaxKg = 10.0, platesKg = listOf(5.0), hasBelt = true)
        assertEquals(listOf(0.0, 5.0, 10.0, 15.0), kit.choices(hanging))
        // A push-up takes the vest only.
        assertEquals(listOf(0.0, 10.0), kit.choices(vestOnly))
    }

    @Test
    fun poundPlatesAddUpExactly() {
        val pound = 0.45359237
        val kit = LoadKit(platesKg = listOf(25 * pound, 25 * pound), hasBelt = true)
        val choices = kit.choices(setOf(LoadCarrier.BELT))
        assertEquals(3, choices.size)
        assertEquals(50 * pound, choices.last(), 0.001)
    }

    @Test
    fun anExerciseThatTakesNoWeightHasOnlyBodyweight() {
        assertEquals(listOf(0.0), LoadKit(vestMaxKg = 10.0, vestStepKg = 1.0).choices(emptySet()))
    }

    @Test
    fun emptyMeansNothingToAddWeightWith() {
        assertTrue(LoadKit().isEmpty)
        assertTrue(!LoadKit(vestMaxKg = 5.0).isEmpty)
    }

    @Test
    fun impossibleKitIsRefused() {
        assertFailsWith<IllegalArgumentException> { LoadKit(vestMaxKg = -1.0) }
        assertFailsWith<IllegalArgumentException> { LoadKit(vestMaxKg = 5.0, vestStepKg = 6.0) }
        assertFailsWith<IllegalArgumentException> { LoadKit(platesKg = listOf(0.0)) }
        assertFailsWith<IllegalArgumentException> { LoadKit(platesKg = List(25) { 1.0 }) }
    }
}
