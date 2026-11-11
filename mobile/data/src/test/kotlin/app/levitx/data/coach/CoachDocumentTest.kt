package app.levitx.data.coach

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.BodyArea
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Effort
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.ExerciseResult
import app.levitx.core.coach.PlanStyle
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.SessionResult
import app.levitx.core.coach.SuggestionChoice
import app.levitx.core.coach.WeighIn
import app.levitx.core.pain.PainAdvice
import app.levitx.core.pain.PainAssessment
import app.levitx.core.pain.PainReason
import app.levitx.core.session.LoggedSet
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachDocumentTest {
    private val coach = Coach()
    private val days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
    private val profile = CoachProfile(
        goalPathId = "first_pull_up",
        style = PlanStyle.FOCUSED,
        sessionsPerWeek = 3,
        sessionMinutes = 40,
        equipment = setOf(Equipment.PULL_UP_BAR, Equipment.BAND),
    )

    /** A model with something in every field, from two real sessions. */
    private fun trained(): AthleteModel {
        var model = AthleteModel.starting(profile, PullUpAnswer.NOT_YET)
        repeat(2) { index ->
            val session = checkNotNull(coach.recommend(model, at(index * 2)).session)
            val results = session.plans.map { plan ->
                ExerciseResult(plan, plan.sets.map { LoggedSet(it.target, Effort.HARD.repsInReserve, it.isBestEffort) })
            }
            model =
                coach.record(model, SessionResult(results, at(index * 2), at(index * 2) + HOUR, SuggestionChoice.KEEP))
        }
        return model
            .withPain(BodyArea.ELBOW, PainAssessment(PainAdvice.EASE_OFF, PainReason.WORSE_NEXT_MORNING))
            .withWeightNoticeShown()
            .copy(skips = mapOf("squat.bodyweight" to 1), lastEasedAtEpochMillis = at(3))
    }

    private fun roundTrip(model: AthleteModel): CoachDocument =
        CoachCodec.decodeCoach(CoachCodec.encode(coachDocumentOf(model, days)))

    @Test
    fun everythingTheCoachLearnedSurvivesStorage() {
        val model = trained()
        val document = roundTrip(model)
        assertEquals(model, document.toModel())
        assertEquals(days, document.days())
    }

    @Test
    fun bodyweightIsNeverWrittenIntoTheDocument() {
        val model = trained().withWeighIn(WeighIn(at(1), 80.0))
        val json = CoachCodec.encode(coachDocumentOf(model, days))
        assertFalse(json.contains("80.0"))
        assertEquals(emptyList<WeighIn>(), roundTrip(model).toModel().weighIns)
    }

    @Test
    fun valuesThisVersionDoesNotKnowAreDroppedNotFatal() {
        val json = CoachCodec.encode(coachDocumentOf(trained(), days))
            .replace("\"ELBOW\"", "\"TAIL\"")
            .replace("\"BAND\"", "\"JETPACK\"")
        val model = CoachCodec.decodeCoach(json).toModel()
        assertTrue(model.pain.isEmpty())
        assertEquals(setOf(Equipment.PULL_UP_BAR), model.profile.equipment)
    }

    @Test
    fun fieldsAddedByANewerVersionAreIgnored() {
        val json = CoachCodec.encode(coachDocumentOf(trained(), days)).replaceFirst("{", "{\"futureField\":true,")
        assertEquals(trained(), CoachCodec.decodeCoach(json).toModel())
    }

    @Test
    fun aDocumentFromANewerFormatIsRefusedRatherThanMisread() {
        val json = CoachCodec.encode(coachDocumentOf(trained(), days).copy(v = COACH_DOCUMENT_VERSION + 1))
        assertThrows(UnreadableCoachStateException::class.java) { CoachCodec.decodeCoach(json) }
    }

    @Test
    fun aDamagedDocumentFailsWithoutQuotingIt() {
        val json = CoachCodec.encode(coachDocumentOf(trained(), days))
        val error = assertThrows(UnreadableCoachStateException::class.java) { CoachCodec.decodeCoach(json.take(40)) }
        assertFalse(error.message.orEmpty().contains("first_pull_up"))
        assertTrue(error.cause == null)
    }

    private companion object {
        const val HOUR = 3_600_000L

        fun at(day: Int): Long = day * 24 * HOUR + 18 * HOUR
    }
}
