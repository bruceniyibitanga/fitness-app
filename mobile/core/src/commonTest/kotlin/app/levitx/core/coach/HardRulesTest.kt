package app.levitx.core.coach

import app.levitx.core.pain.PainAdvice
import app.levitx.core.pain.PainAssessment
import app.levitx.core.pain.PainReason
import app.levitx.core.skills.SkillCatalogue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HardRulesTest {
    private val pushUp = exercise("horizontal_push.push_up")
    private val squat = exercise("squat.bodyweight")
    private val row = exercise("horizontal_pull.row")
    private val session = sessionOf(hardPlan(pushUp, 8), hardPlan(squat, 12))

    private fun sore(area: BodyArea, advice: PainAdvice): AthleteModel =
        newcomer().withPain(area, PainAssessment(advice, PainReason.TOO_HIGH_DURING_EXERCISE))

    private fun trained(day: Int, plan: ExercisePlan, model: AthleteModel = newcomer()): AthleteModel =
        Coach().record(model, finished(day, done(plan, plan.workTarget)))

    @Test
    fun aHealthyRestedPersonCanDoTheSession() {
        assertNull(HardRules.firstBlock(session, contextOn(0, newcomer())))
    }

    @Test
    fun aSoreAreaAllowsOnlyLightWorkOnWhatLoadsIt() {
        val context = contextOn(0, sore(BodyArea.ELBOW, PainAdvice.EASE_OFF))
        assertEquals(Blocked.Pain(BodyArea.ELBOW, stop = false), PainRule.blocks(session, context))

        val repaired = assertNotNull(HardRules.repair(session, context)).session
        assertEquals(Intensity.LIGHT, repaired.plans.first { it.exercise == pushUp }.intensity)
        assertEquals(Intensity.HARD, repaired.plans.first { it.exercise == squat }.intensity)
    }

    @Test
    fun anAreaThatNeedsAProfessionalIsLeftOutAltogether() {
        val context = contextOn(0, sore(BodyArea.SHOULDER, PainAdvice.SEE_PROFESSIONAL))
        val repaired = assertNotNull(HardRules.repair(session, context)).session
        assertEquals(listOf(squat), repaired.plans.map { it.exercise })
        assertNull(PainRule.blocks(session.lightened().let { PlannedSession(it.plans.drop(1)) }, context))
    }

    @Test
    fun nothingIsLeftWhenEveryExerciseLoadsAnAreaThatNeedsAProfessional() {
        val context = contextOn(0, sore(BodyArea.SHOULDER, PainAdvice.SEE_PROFESSIONAL))
        assertNull(HardRules.repair(sessionOf(hardPlan(pushUp, 8)), context))
    }

    @Test
    fun adviceToCarryOnClearsTheArea() {
        val recovered = sore(BodyArea.ELBOW, PainAdvice.EASE_OFF)
            .withPain(BodyArea.ELBOW, PainAssessment(PainAdvice.CONTINUE_AS_PLANNED, PainReason.WITHIN_GUIDE))
        assertNull(HardRules.firstBlock(session, contextOn(0, recovered)))
    }

    @Test
    fun noHardWorkForAPatternTheDayAfterHardWork() {
        val model = trained(0, hardPlan(pushUp, 8))
        assertEquals(
            Blocked.Recovering(MovementPattern.HORIZONTAL_PUSH),
            RecoveryRule.blocks(session, contextOn(1, model)),
        )
        assertNull(RecoveryRule.blocks(session, contextOn(2, model)))
    }

    @Test
    fun aMorningSessionTwoDaysAfterAnEveningOneIsAllowed() {
        val model = trained(0, hardPlan(pushUp, 8))
        val wednesdayMorning = atDay(2) - 10 * MILLIS_PER_HOUR
        assertNull(RecoveryRule.blocks(session, CoachContext(model, wednesdayMorning)))
    }

    @Test
    fun lightWorkIsAllowedWhileRecovering() {
        val model = trained(0, hardPlan(pushUp, 8))
        assertNull(RecoveryRule.blocks(session.lightened(), contextOn(1, model)))
    }

    @Test
    fun indirectSetsAloneDoNotStartTheRecoveryClock() {
        // Three sets of rows give pulling up 1.5 sets, under the two that count as a hard day.
        val model = trained(0, hardPlan(row, 8))
        val hang = hardPlan(exercise("vertical_pull.dead_hang"), 20)
        assertNull(RecoveryRule.blocks(sessionOf(hang), contextOn(1, model)))
    }

    @Test
    fun weeklyHardSetsStopAtTheStartingAllowanceForSomeoneNew() {
        val plan = hardPlan(squat, 12)
        val model = (0..3).fold(newcomer()) { model, day -> trained(day, plan, model) }
        // 12 sets done this week. Three more just reach the allowance of 15; six more pass it.
        val context = contextOn(5, model)
        assertNull(VolumeCapRule.blocks(sessionOf(plan), context))
        assertEquals(Blocked.VolumeCapped(MovementPattern.SQUAT), VolumeCapRule.blocks(sessionOf(plan, plan), context))
    }

    @Test
    fun onlyAPatternsOwnSetsCountTowardItsLimit() {
        // Rows also train pulling up, but that doesn't use up the pulling-up allowance.
        val model = (0..4).fold(newcomer()) { model, day -> trained(day, hardPlan(row, 8), model) }
        val context = contextOn(5, model)
        assertEquals(0, context.directThisWeek(MovementPattern.VERTICAL_PULL))
        assertEquals(7.5, context.weekSoFar(MovementPattern.VERTICAL_PULL))
        assertNull(VolumeCapRule.blocks(sessionOf(hardPlan(exercise("vertical_pull.dead_hang"), 20)), context))
    }

    @Test
    fun theAllowanceGrowsFromTheBusiestRecentWeekButNeverPastTheCeiling() {
        val plan = hardPlan(squat, 12)
        val busyWeek = (0..4).fold(newcomer()) { model, day -> trained(day, plan, model) }
        val context = contextOn(11, busyWeek)
        assertEquals(15, context.recentPeak(MovementPattern.SQUAT))
        assertEquals(18.0, VolumeCapRule.allowed(MovementPattern.SQUAT, context))

        val hugeWeek = (0..6).fold(newcomer()) { model, day -> trained(day, plan, model) }
        assertEquals(
            CoachTuning.MAX_WEEKLY_HARD_SETS,
            VolumeCapRule.allowed(MovementPattern.SQUAT, contextOn(13, hugeWeek)),
        )
    }

    @Test
    fun aWeekOffDoesNotResetTheAllowance() {
        val plan = hardPlan(squat, 12)
        val busyWeek = (0..4).fold(newcomer()) { model, day -> trained(day, plan, model) }
        assertEquals(18.0, VolumeCapRule.allowed(MovementPattern.SQUAT, contextOn(25, busyWeek)))
        assertEquals(
            CoachTuning.STARTING_WEEKLY_HARD_SETS,
            VolumeCapRule.allowed(MovementPattern.SQUAT, contextOn(40, busyWeek)),
        )
    }

    @Test
    fun lastWeeksSessionOnTheSameWeekdayIsNotThisWeeks() {
        val model = trained(0, hardPlan(squat, 12))
        assertEquals(0, contextOn(7, model).directThisWeek(MovementPattern.SQUAT))
        assertEquals(3, contextOn(6, model).directThisWeek(MovementPattern.SQUAT))
    }

    @Test
    fun onlyTheGateThePersonIsOnCanBeTrained() {
        val gates = SkillCatalogue.firstPullUp.gates
        val later = assertNotNull(SkillTraining.exerciseFor(gates[2]))
        val outOfOrder = sessionOf(hardPlan(later, 5).copy(gateId = later.id), hardPlan(squat, 12))
        val context = contextOn(0, newcomer())
        assertEquals(Blocked.GateOutOfOrder(later.id), GateOrderRule.blocks(outOfOrder, context))
        assertEquals(
            listOf(squat),
            assertNotNull(HardRules.repair(outOfOrder, context)).session.plans.map {
                it.exercise
            },
        )
    }

    @Test
    fun repairRecordsWhatItHadToRelax() {
        val context = contextOn(0, sore(BodyArea.KNEE, PainAdvice.EASE_OFF))
        val repaired = assertNotNull(HardRules.repair(session, context))
        assertEquals(listOf<Blocked>(Blocked.Pain(BodyArea.KNEE, stop = false)), repaired.relaxed)
        assertTrue(HardRules.repair(session, contextOn(0, newcomer()))?.relaxed.orEmpty().isEmpty())
    }
}
