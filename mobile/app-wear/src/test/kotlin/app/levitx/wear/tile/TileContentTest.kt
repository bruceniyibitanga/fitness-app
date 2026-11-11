package app.levitx.wear.tile

import app.levitx.core.session.SessionPlanner
import app.levitx.core.session.trainingDay
import app.levitx.core.skills.GateMeasure
import app.levitx.core.skills.SkillCatalogue
import org.junit.Assert.assertEquals
import org.junit.Test

class TileContentTest {
    private val path = SkillCatalogue.muscleUp
    private val deadHang = path.gates[0]
    private val dips = path.gates[1]

    @Test
    fun noGoalAsksForOne() {
        assertEquals(TileContent.NoGoal, tileContentFor(null))
    }

    @Test
    fun showsTheCurrentGateAndTodaysWorkSets() {
        val day = path.trainingDay(mapOf(deadHang.id to deadHang.target, dips.id to 6))
        val plan = SessionPlanner.planFor(dips, best = 6)

        assertEquals(
            TileContent.NextSession(
                gateNumber = 2,
                gateCount = path.gates.size,
                gateTitle = dips.title,
                workSets = SessionPlanner.WORK_SETS,
                workTarget = plan.sets.first().target,
                measure = GateMeasure.REPS,
            ),
            tileContentFor(day),
        )
    }

    @Test
    fun aFinishedPathSaysSo() {
        val day = path.trainingDay(path.gates.associate { it.id to it.target })
        assertEquals(TileContent.PathComplete(path.name), tileContentFor(day))
    }
}
