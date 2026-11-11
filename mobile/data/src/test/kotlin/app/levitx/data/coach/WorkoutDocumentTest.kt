package app.levitx.data.coach

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.SuggestionChoice
import app.levitx.core.coach.WorkoutRun
import app.levitx.core.session.SetLoad
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutDocumentTest {
    private val model = AthleteModel.starting(
        CoachProfile("first_pull_up", equipment = setOf(Equipment.PULL_UP_BAR)),
        PullUpAnswer.NOT_YET,
    )
    private val session = checkNotNull(Coach().recommend(model, 0).session)

    /** A run part way through, with a skip, a rest and a load. */
    private val run = WorkoutRun("w1", session, startedAtEpochMillis = 5, choice = SuggestionChoice.KEEP)
        .logSet(10, repsInReserve = 2, nowElapsedMillis = 0)
        .endRest()
        .skipExercise()
        .withLoad(SetLoad(addedKg = 5.0))
        .logSet(8, repsInReserve = null, nowElapsedMillis = 100)

    private fun roundTrip(run: WorkoutRun): WorkoutRun? =
        CoachCodec.decodeWorkoutOrNull(CoachCodec.encode(workoutDocumentOf(run)))?.toRunOrNull()

    @Test
    fun aWorkoutInProgressSurvivesStorage() {
        assertEquals(run, roundTrip(run))
    }

    @Test
    fun aDraftWithAnExerciseThisVersionDoesNotKnowIsDropped() {
        val json = CoachCodec.encode(workoutDocumentOf(run)).replace(session.plans.last().exercise.id, "gone.exercise")
        assertNull(CoachCodec.decodeWorkoutOrNull(json)?.toRunOrNull())
    }

    @Test
    fun aDraftThatBreaksTheRunsRulesIsDropped() {
        val document = workoutDocumentOf(run).copy(exerciseIndex = session.plans.size + 3)
        assertNull(CoachCodec.decodeWorkoutOrNull(CoachCodec.encode(document))?.toRunOrNull())
    }

    @Test
    fun damagedOrNewerDraftsAreDropped() {
        assertNull(CoachCodec.decodeWorkoutOrNull("{not json"))
        val newer = workoutDocumentOf(run).copy(v = WORKOUT_DOCUMENT_VERSION + 1)
        assertNull(CoachCodec.decodeWorkoutOrNull(CoachCodec.encode(newer)))
    }
}
