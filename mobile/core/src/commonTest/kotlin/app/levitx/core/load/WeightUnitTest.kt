package app.levitx.core.load

import kotlin.test.Test
import kotlin.test.assertEquals

class WeightUnitTest {
    @Test
    fun weightsAreWrittenAsPeopleWriteThem() {
        assertEquals("1.25", formatWeight(1.25, WeightUnit.KG))
        assertEquals("2.5", formatWeight(2.5, WeightUnit.KG))
        assertEquals("20", formatWeight(20.0, WeightUnit.KG))
        assertEquals("0.05", formatWeight(0.05, WeightUnit.KG))
    }

    @Test
    fun poundsComeBackExactlyAsEntered() {
        val plate = WeightUnit.LB.toKg(45.0)
        assertEquals(20.41165665, plate, 1e-9)
        assertEquals("45", formatWeight(plate, WeightUnit.LB))
        assertEquals("2.5", formatWeight(WeightUnit.LB.toKg(2.5), WeightUnit.LB))
    }

    @Test
    fun kilogramsShowInPounds() {
        assertEquals("22.05", formatWeight(10.0, WeightUnit.LB))
    }
}
