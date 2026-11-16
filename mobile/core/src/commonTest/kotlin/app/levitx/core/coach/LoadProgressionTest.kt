package app.levitx.core.coach

import app.levitx.core.load.LoadKit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoadProgressionTest {
    private val pullUp = exercise("vertical_pull.pull_up")
    private val pistol = exercise("squat.pistol")
    private val plates = LoadKit(platesKg = listOf(1.25, 1.25, 2.5, 2.5, 5.0, 5.0, 10.0, 10.0), hasBelt = true)

    @Test
    fun aStepIsAtLeastTwoPercentOfTheLoadMoved() {
        // An 80 kg pull-up moves 76 kg: 1.25 kg is 1.6%, 2.5 kg is 3.3%.
        assertEquals(2.5, LoadProgression.nextUp(pullUp, 0.0, plates, bodyweightKg = 80.0))
    }

    @Test
    fun aStepOverTenPercentIsNotTaken() {
        val bigPlates = LoadKit(platesKg = listOf(20.0), hasBelt = true)
        assertNull(LoadProgression.nextUp(pullUp, 0.0, bigPlates, bodyweightKg = 80.0))
    }

    @Test
    fun weightStopsAtHalfOfBodyweight() {
        assertEquals(37.5, LoadProgression.nextUp(pullUp, 35.0, plates, bodyweightKg = 80.0))
        assertNull(LoadProgression.nextUp(pullUp, 40.0, plates, bodyweightKg = 80.0))
    }

    @Test
    fun exercisesThatNeverTakeWeightNeverGetAny() {
        assertNull(LoadProgression.nextUp(pistol, 0.0, LoadKit(vestMaxKg = 10.0, vestStepKg = 1.0), 80.0))
    }

    @Test
    fun goingDownTakesTheNextLighterWeight() {
        assertEquals(8.75, LoadProgression.nextDown(pullUp, 10.0, plates))
        assertEquals(0.0, LoadProgression.nextDown(pullUp, 1.25, plates))
    }
}
