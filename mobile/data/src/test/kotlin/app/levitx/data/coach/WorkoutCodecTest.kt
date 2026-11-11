package app.levitx.data.coach

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
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
        assertEquals(session, WorkoutCodec.decodePlanOrNull(WorkoutCodec.encodePlan(session)))
    }

    @Test
    fun aLighterSessionKeepsItsIntensity() {
        val lighter = session.lightened()
        assertEquals(lighter, WorkoutCodec.decodePlanOrNull(WorkoutCodec.encodePlan(lighter)))
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
        val unknown = WorkoutCodec.encodePlan(session).replace(session.plans.first().exercise.id, "gone.exercise")
        assertNull(WorkoutCodec.decodePlanOrNull(unknown))
        assertNull(WorkoutCodec.decodeRunOrNull("{}"))
    }
}
