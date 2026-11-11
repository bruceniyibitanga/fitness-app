package app.levitx.core.coach

import app.levitx.core.pain.PainAdvice
import app.levitx.core.pain.PainAssessment
import app.levitx.core.pain.PainReason
import app.levitx.core.skills.SkillCatalogue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoachTest {
    private val coach = Coach()
    private val easeOff = PainAssessment(PainAdvice.EASE_OFF, PainReason.TOO_HIGH_DURING_EXERCISE)
    private val seeSomeone = PainAssessment(PainAdvice.SEE_PROFESSIONAL, PainReason.RED_FLAG)

    private fun first(model: AthleteModel): PlannedSession = assertNotNull(coach.recommend(model, atDay(0)).session)

    /** Does the coach's session on [day], hitting every target, and returns the model after. */
    private fun AthleteModel.trainOn(day: Int): AthleteModel {
        val session = assertNotNull(coach.recommend(this, atDay(day)).session)
        return coach.record(this, finished(day, *session.plans.map { done(it, it.workTarget) }.toTypedArray()))
    }

    @Test
    fun theFirstSessionStartsWithTheSkillAndFitsTheTime() {
        val recommendation = coach.recommend(newcomer(), atDay(0))
        val session = assertNotNull(recommendation.session)
        assertEquals(SkillCatalogue.firstPullUp.gates.first().id, session.plans.first().gateId)
        assertTrue(session.estimatedMinutes <= newcomer().profile.sessionMinutes)
        assertTrue(session.plans.size >= 4, "a 45-minute all-round session trains more than the skill")
        assertEquals(CoachReason.StillLearning, recommendation.reason)
        assertNull(recommendation.suggestion)
    }

    @Test
    fun theFirstSessionsFindThePersonsLevelWithABestEffortSet() {
        val ladderPlans = first(newcomer()).plans.filter { it.gateId == null && it.sets.size > 2 }
        assertTrue(ladderPlans.isNotEmpty() && ladderPlans.all { it.sets.last().isBestEffort })
    }

    @Test
    fun theSameModelAtTheSameTimeAlwaysGetsTheSameAnswer() {
        val model = newcomer().trainOn(0).trainOn(2)
        assertEquals(coach.recommend(model, atDay(4)), Coach().recommend(model, atDay(4)))
    }

    @Test
    fun theGatesPatternIsTrainedByTheGateAndNothingElse() {
        val pulling = first(newcomer()).plans.filter { it.exercise.pattern == MovementPattern.VERTICAL_PULL }
        assertEquals(1, pulling.size)
        assertNotNull(pulling.single().gateId)
    }

    @Test
    fun sayingYesToAPullUpStartsPullingAtPullUps() {
        val profile = CoachProfile(goalPathId = null, equipment = fullKit)
        val session = first(AthleteModel.starting(profile, PullUpAnswer.YES))
        assertTrue(session.plans.any { it.exercise.id == "vertical_pull.pull_up" })
        val unsure = first(AthleteModel.starting(profile, PullUpAnswer.NOT_SURE))
        assertTrue(unsure.plans.any { it.exercise.id == "vertical_pull.dead_hang" })
    }

    @Test
    fun withoutABarTheCoachTrainsWhatItCan() {
        val session = first(newcomer(pullUpProfile(equipment = emptySet())))
        assertTrue(session.plans.none { it.gateId != null || it.exercise.equipment.isNotEmpty() })
        assertTrue(session.plans.isNotEmpty())
    }

    @Test
    fun generalFitnessWorksWithNoSkillGoal() {
        val model = newcomer(CoachProfile(goalPathId = null, equipment = fullKit))
        val session = first(model)
        assertTrue(session.plans.none { it.gateId != null })
        assertTrue(session.plans.map { it.exercise.pattern }.toSet().size >= 4)
    }

    @Test
    fun theMuscleUpPathStartsAtItsFirstGate() {
        val model = newcomer(CoachProfile("muscle_up", equipment = fullKit))
        assertEquals(SkillCatalogue.muscleUp.gates.first().id, first(model).plans.first().gateId)
    }

    @Test
    fun aFocusedPlanDoesMoreSkillWorkAndLessOfWhatDoesNotHelpIt() {
        val allRound = first(newcomer())
        val focused = first(newcomer(pullUpProfile(style = PlanStyle.FOCUSED)))
        assertTrue(focused.plans.first().sets.size > allRound.plans.first().sets.size)
        val squats = focused.plans.first { it.exercise.pattern == MovementPattern.SQUAT }
        assertEquals(CoachTuning.MIN_SETS_PER_EXERCISE, squats.sets.size)
    }

    @Test
    fun aTenMinuteSessionStillGetsSomethingWorthDoing() {
        val session = first(newcomer(pullUpProfile(sessionMinutes = CoachProfile.MIN_SESSION_MINUTES)))
        assertNotNull(session.plans.first().gateId)
        assertTrue(session.estimatedMinutes <= CoachProfile.MIN_SESSION_MINUTES + 1)
    }

    @Test
    fun theDayAfterASessionNothingTrainedYesterdayIsHard() {
        val model = newcomer().trainOn(0)
        val trainedHard = model.history.single().directSets.filterValues {
            it >= CoachTuning.HARD_SESSION_MIN_SETS
        }.keys
        val session = assertNotNull(coach.recommend(model, atDay(1)).session)
        assertTrue(session.plans.filter { it.isHard }.none { it.exercise.pattern in trainedHard })
    }

    @Test
    fun aWorkoutFromTheTrainTabCountsLikeAnyOther() {
        val pushUp = exercise("horizontal_push.hands_61cm")
        val offPlan = coach.record(newcomer(), finished(0, done(hardPlan(pushUp, 6), 6)))
        val nextDay = assertNotNull(coach.recommend(offPlan, atDay(1)).session)
        assertTrue(nextDay.plans.filter { it.isHard }.none { it.exercise.pattern == MovementPattern.HORIZONTAL_PUSH })
    }

    @Test
    fun aSoreElbowMakesUpperBodyWorkLightAndTheCoachSaysWhy() {
        val recommendation = coach.recommend(newcomer().withPain(BodyArea.ELBOW, easeOff), atDay(0))
        val session = assertNotNull(recommendation.session)
        val loadsElbow = session.plans.filter { BodyArea.ELBOW in it.exercise.pattern.loads }
        assertTrue(loadsElbow.isNotEmpty() && loadsElbow.none { it.isHard })
        assertTrue(session.plans.any { it.isHard }, "legs and core carry on")
        assertEquals(CoachReason.WorkingAround(BodyArea.ELBOW), recommendation.reason)
    }

    @Test
    fun anAreaThatNeedsAProfessionalIsNotTrainedAtAll() {
        val session = first(newcomer().withPain(BodyArea.KNEE, seeSomeone))
        assertTrue(session.plans.none { BodyArea.KNEE in it.exercise.pattern.loads })
    }

    @Test
    fun whenNothingIsSafeTheCoachSaysRest() {
        val everywhere = BodyArea.entries.fold(newcomer()) { model, area -> model.withPain(area, seeSomeone) }
        val recommendation = coach.recommend(everywhere, atDay(0))
        assertNull(recommendation.session)
        assertEquals(CoachReason.RestDay, recommendation.reason)
    }

    @Test
    fun theLighterOptionIsAlwaysSafeToDo() {
        val model = newcomer().trainOn(0).copy(block = TrainingBlock(lighterSessionsLeft = 1))
        val context = contextOn(2, model)
        val lighter = assertNotNull(coach.recommend(model, atDay(2)).suggestion).lighter
        assertNull(HardRules.firstBlock(lighter, context))
        assertTrue(lighter.isLight)
    }

    @Test
    fun fastWeightLossIsMentionedOnceAndHoldsProgress() {
        val losing = listOf(0 to 80.0, 7 to 78.8, 14 to 77.6)
            .fold(newcomer().trainOn(0).trainOn(2).trainOn(14)) { model, (day, kg) ->
                model.withWeighIn(WeighIn(atDay(day), kg))
            }
            .copy(exercises = emptyMap())
        val told = coach.recommend(losing, atDay(16))
        assertEquals(setOf(CoachNotice.FAST_WEIGHT_LOSS), told.notices)
        assertEquals(CoachReason.HoldingSteady, told.reason)
        assertTrue(coach.recommend(losing.withWeightNoticeShown(), atDay(16)).notices.isEmpty())
    }

    @Test
    fun aChangeSinceLastTimeIsExplained() {
        val model = newcomer().trainOn(0).trainOn(2).trainOn(4).trainOn(7)
        val reason = coach.recommend(model, atDay(9)).reason
        assertIs<CoachReason.Progress>(reason)
    }

    @Test
    fun aComebackIsOfferedLighter() {
        val model = newcomer().trainOn(0).trainOn(2)
        val suggestion = assertNotNull(coach.recommend(model, atDay(30)).suggestion)
        assertIs<ReadinessSignal.LongGap>(suggestion.signals.single())
        assertEquals(SuggestionChoice.LIGHTER, suggestion.recommended)
    }

    @Test
    fun theCurrentExerciseFollowsTheRungAndTheKit() {
        val model = newcomer().copy(rungs = mapOf(MovementPattern.HORIZONTAL_PUSH to 3))
        assertEquals("horizontal_push.push_up", model.currentExercise(MovementPattern.HORIZONTAL_PUSH)?.id)
        assertNull(newcomer(pullUpProfile(equipment = emptySet())).currentExercise(MovementPattern.HORIZONTAL_PULL))
    }

    @Test
    fun theCurrentGateMovesOnAsGatesArePassed() {
        val model = newcomer()
        val path = checkNotNull(model.profile.goalPath)
        assertEquals(path.gates.first(), model.currentGate())
        assertEquals(MovementPattern.VERTICAL_PULL, model.currentGatePattern())
        val passedAll = model.copy(gateBests = path.gates.associate { it.id to it.target })
        assertNull(passedAll.currentGate())
        assertNull(passedAll.currentGatePattern())
    }
}
