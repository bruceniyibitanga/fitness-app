package app.levitx.data.coach

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.WeighIn
import app.levitx.data.database.BodyweightEntity
import app.levitx.data.database.GateBestEntity
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Test

class CoachStateAssemblyTest {
    private val model = AthleteModel.starting(CoachProfile("first_pull_up"), PullUpAnswer.NOT_YET)
        .copy(gateBests = mapOf("first_pull_up.dead_hang" to 20, "first_pull_up.scapular_pulls" to 4))
    private val document = coachDocumentOf(model, setOf(DayOfWeek.TUESDAY))

    @Test
    fun theHigherBestWinsWhereverItWasRecorded() {
        val fromTheWatch = listOf(
            GateBestEntity("first_pull_up.dead_hang", 25, 0),
            GateBestEntity("first_pull_up.scapular_pulls", 2, 0),
            GateBestEntity("muscle_up.dips", 6, 0),
        )
        val bests = assembleCoachState(document, fromTheWatch, emptyList()).model.gateBests
        assertEquals(
            mapOf("first_pull_up.dead_hang" to 25, "first_pull_up.scapular_pulls" to 4, "muscle_up.dips" to 6),
            bests,
        )
    }

    @Test
    fun bodyweightComesFromItsOwnTable() {
        val readings =
            listOf(BodyweightEntity(1, 80.0, 10), BodyweightEntity(2, 79.5, 20), BodyweightEntity(3, 5.0, 30))
        val state = assembleCoachState(document, emptyList(), readings)
        assertEquals(listOf(WeighIn(10, 80.0), WeighIn(20, 79.5)), state.model.weighIns)
        assertEquals(setOf(DayOfWeek.TUESDAY), state.trainingDays)
    }
}
