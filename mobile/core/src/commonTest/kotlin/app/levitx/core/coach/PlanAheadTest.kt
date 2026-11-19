package app.levitx.core.coach

import app.levitx.core.pain.PainAdvice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The plan the phone sends the watch: right whether it is done at once or days later. */
class PlanAheadTest {
    private val coach = Coach()
    private val restMillis = CoachTuning.MIN_HOURS_BETWEEN_HARD_SESSIONS * MILLIS_PER_HOUR

    /** [model] after doing the coach's session on [day], every set on target. */
    private fun AthleteModel.trainedOn(day: Int): AthleteModel {
        val session = assertNotNull(coach.recommend(this, atDay(day)).session)
        return coach.record(this, finished(day, *session.plans.map { done(it, it.workTarget) }.toTypedArray()))
    }

    @Test
    fun beforeTheFirstSessionThereIsOnlyOnePlan() {
        val plan = assertNotNull(coach.planAhead(newcomer(), atDay(0)))

        assertEquals(coach.recommend(newcomer(), atDay(0)).session, plan.now)
        assertNull(plan.rested)
        assertNull(plan.restedAtEpochMillis)
    }

    @Test
    fun straightAfterASessionThePlanForNowIsLightAndTheRestedOneIsTheRealNextSession() {
        val model = newcomer().trainedOn(day = 1)
        val finishedAt = atDay(1) + MILLIS_PER_HOUR

        val plan = assertNotNull(coach.planAhead(model, finishedAt))

        // Right now, what was just trained can only be done light.
        assertTrue(plan.now.plans.any { !it.isHard })
        // Once rested, the watch does exactly what the phone would say then.
        assertEquals(finishedAt + restMillis, plan.restedAtEpochMillis)
        assertEquals(coach.recommend(model, finishedAt + restMillis).session, plan.rested)
        assertTrue(assertNotNull(plan.rested).plans.all { it.isHard })
    }

    @Test
    fun theWatchGetsTheSessionForTheMomentItStarts() {
        val model = newcomer().trainedOn(day = 1)
        val finishedAt = atDay(1) + MILLIS_PER_HOUR
        val plan = assertNotNull(coach.planAhead(model, finishedAt))
        val restedAt = assertNotNull(plan.restedAtEpochMillis)

        assertEquals(plan.now, plan.at(finishedAt))
        assertEquals(plan.now, plan.at(restedAt - 1))
        assertEquals(plan.rested, plan.at(restedAt))
        assertEquals(plan.rested, plan.at(atDay(9)))
    }

    @Test
    fun onceTheRestIsOverThePlanForNowIsAlreadyTheRealOne() {
        val model = newcomer().trainedOn(day = 1)

        val plan = assertNotNull(coach.planAhead(model, atDay(4)))

        assertNull(plan.rested)
        assertEquals(coach.recommend(model, atDay(4)).session, plan.now)
    }

    @Test
    fun whenNothingCanBeTrainedSafelyThereIsNoPlan() {
        val stop = PainAdvice.SEE_PROFESSIONAL
        val sore = newcomer().copy(pain = BodyArea.entries.associateWith { stop })

        assertNull(coach.recommend(sore, atDay(0)).session)
        assertNull(coach.planAhead(sore, atDay(0)))
    }

    @Test
    fun aRestedSessionAlwaysComesWithItsTime() {
        val session = assertNotNull(coach.recommend(newcomer(), atDay(0)).session)

        assertFailsWith<IllegalArgumentException> { PlanAhead(session, rested = session) }
        assertFailsWith<IllegalArgumentException> { PlanAhead(session, restedAtEpochMillis = 5) }
    }
}
