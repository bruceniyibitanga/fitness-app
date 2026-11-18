package app.levitx.wear.session

import app.levitx.core.coach.ExerciseCatalogue
import app.levitx.core.coach.ExercisePlan
import app.levitx.core.coach.Intensity
import app.levitx.core.session.PlannedSet
import app.levitx.core.session.SetLoad
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Which load a set is saved with on the watch: what the person chose for
 * gate work, otherwise the weight the coach prescribed.
 */
class LoadWithTest {
    private val dip = checkNotNull(ExerciseCatalogue.byId("vertical_push.dip"))
    private val chosen = SetLoad(assistanceKg = 20.0)

    private fun plan(gateId: String? = null, addedKg: Double = 0.0) = ExercisePlan(
        exercise = dip,
        sets = listOf(PlannedSet(target = 8, isBestEffort = false)),
        restSeconds = 120,
        repsInReserve = 2,
        intensity = Intensity.HARD,
        gateId = gateId,
        addedKg = addedKg,
    )

    @Test
    fun gateWorkKeepsTheLoadThePersonChose() {
        assertEquals(chosen, plan(gateId = "dips", addedKg = 5.0).loadWith(chosen))
    }

    @Test
    fun accessoryWorkUsesThePrescribedWeight() {
        // Before this, a weighted accessory set was saved as bodyweight.
        assertEquals(SetLoad(addedKg = 5.0), plan(addedKg = 5.0).loadWith(chosen))
    }

    @Test
    fun bodyweightWorkIsSavedAsBodyweight() {
        assertEquals(SetLoad.None, plan().loadWith(chosen))
    }
}
