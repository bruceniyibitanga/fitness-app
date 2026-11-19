package app.levitx.data.coach

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PlanAhead
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.WorkoutRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutCodecTest {
    private val model = AthleteModel.starting(
        CoachProfile("first_pull_up", equipment = setOf(Equipment.PULL_UP_BAR)),
        PullUpAnswer.NOT_YET,
    )
    private val session = checkNotNull(Coach().recommend(model, 0).session)

    @Test
    fun aPlannedSessionTravelsWhole() {
        val plan = PlanAhead(session)
        assertEquals(plan, WorkoutCodec.decodePlanOrNull(WorkoutCodec.encodePlan(plan)))
    }

    @Test
    fun aLighterSessionKeepsItsIntensity() {
        val lighter = PlanAhead(session.lightened())
        assertEquals(lighter, WorkoutCodec.decodePlanOrNull(WorkoutCodec.encodePlan(lighter)))
    }

    @Test
    fun theSessionForOnceRestedTravelsWithItsTime() {
        val plan = PlanAhead(now = session.lightened(), rested = session, restedAtEpochMillis = 9_000)
        assertEquals(plan, WorkoutCodec.decodePlanOrNull(WorkoutCodec.encodePlan(plan)))
    }

    @Test
    fun aLaterSessionWithoutATimeIsDropped() {
        val plan = PlanAhead(now = session.lightened(), rested = session, restedAtEpochMillis = 9_000)
        val noTime = WorkoutCodec.encodePlan(plan).replace(""","laterFrom":9000""", "")
        assertEquals(PlanAhead(plan.now), WorkoutCodec.decodePlanOrNull(noTime))
    }

    @Test
    fun aFinishedWorkoutTravelsWhole() {
        val run = WorkoutRun("w1", session, startedAtEpochMillis = 5).logSet(20, null, 0).endRest()
        assertEquals(run, WorkoutCodec.decodeRunOrNull(WorkoutCodec.encodeRun(run)))
    }

    @Test
    fun anythingElseReadsAsNothing() {
        assertNull(WorkoutCodec.decodePlanOrNull("not json"))
        assertNull(WorkoutCodec.decodePlanOrNull("{\"v\":1,\"plans\":[]}"))
        assertNull(WorkoutCodec.decodePlanOrNull("{\"v\":99,\"plans\":[]}"))
        val unknown = WorkoutCodec.encodePlan(
            PlanAhead(session),
        ).replace(session.plans.first().exercise.id, "gone.exercise")
        assertNull(WorkoutCodec.decodePlanOrNull(unknown))
        assertNull(WorkoutCodec.decodeRunOrNull("{}"))
    }

    @Test
    fun aPlanHoldingACardioMoveReadsAsNothing() {
        // The coach has no ladder for cardio, so a plan with one in it must never reach the coach.
        val first = session.plans.first().exercise.id
        val plan = WorkoutCodec.encodePlan(PlanAhead(session)).replace(first, "cardio.burpees")
        val run = WorkoutCodec.encodeRun(WorkoutRun("w1", session, startedAtEpochMillis = 5))
            .replace(first, "cardio.burpees")

        assertNull(WorkoutCodec.decodePlanOrNull(plan))
        assertNull(WorkoutCodec.decodeRunOrNull(run))
    }
}
