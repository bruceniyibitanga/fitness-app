package app.levitx.core.coach

import app.levitx.core.session.LoggedSet
import app.levitx.core.skills.SkillCatalogue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProgressionTest {
    private val coach = Coach()
    private val pushUp = exercise("horizontal_push.push_up")
    private val kneePushUp = exercise("horizontal_push.knees")
    private val pushUpRung = ExerciseCatalogue.ladder(MovementPattern.HORIZONTAL_PUSH).rungs
        .indexOfFirst { pushUp in it.options }

    /** Someone past calibration on the push-up, with work sets at [target]. */
    private fun settled(target: Int): AthleteModel = newcomer().copy(
        rungs = mapOf(MovementPattern.HORIZONTAL_PUSH to pushUpRung),
        exercises = mapOf(pushUp.id to ExerciseState(target = target, best = target, timesTrained = 5)),
    )

    private fun AthleteModel.pushUps(): ExerciseState = exercises.getValue(pushUp.id)

    private fun AthleteModel.after(day: Int, target: Int, reps: Int, effort: Effort? = Effort.RIGHT): AthleteModel =
        coach.record(this, finished(day, done(hardPlan(pushUp, target), reps, effort)))

    @Test
    fun hittingEverySetRaisesTheTargetByOneRep() {
        val after = settled(8).after(day = 0, target = 8, reps = 8)
        assertEquals(9, after.pushUps().target)
        assertEquals(ProgressNote.TARGET_UP, after.pushUps().note)
    }

    @Test
    fun aHardSessionHoldsTheTargetEvenWhenEverySetWasHit() {
        assertEquals(8, settled(8).after(day = 0, target = 8, reps = 8, effort = Effort.HARD).pushUps().target)
    }

    @Test
    fun twoEasySessionsInARowRaiseTheTargetByTwo() {
        val once = settled(8).after(day = 0, target = 8, reps = 8, effort = Effort.EASY)
        assertEquals(9, once.pushUps().target)
        assertEquals(11, once.after(day = 2, target = 9, reps = 9, effort = Effort.EASY).pushUps().target)
    }

    @Test
    fun setsWithNoRatingStillProgressOnRepsAlone() {
        assertEquals(9, settled(8).after(day = 0, target = 8, reps = 8, effort = null).pushUps().target)
    }

    @Test
    fun oneMissedSessionChangesNothing() {
        val after = settled(8).after(day = 0, target = 8, reps = 6)
        assertEquals(8, after.pushUps().target)
        assertNull(after.pushUps().note)
    }

    @Test
    fun twoMissedSessionsInARowEaseTheTargetBack() {
        val after = settled(8).after(day = 0, target = 8, reps = 6).after(day = 2, target = 8, reps = 6)
        assertEquals(7, after.pushUps().target)
        assertEquals(ProgressNote.EASED_BACK, after.pushUps().note)
    }

    @Test
    fun stoppingEarlyCountsAsAMiss() {
        val plan = hardPlan(pushUp, 8)
        val twoOfThree = ExerciseResult(plan, List(2) { LoggedSet(8, repsInReserve = 2, isBestEffort = false) })
        assertEquals(8, coach.record(settled(8), finished(0, twoOfThree)).pushUps().target)
    }

    @Test
    fun toppingOutTheRangeMovesUpARung() {
        val top = pushUp.range.last
        val after = settled(top).after(day = 0, target = top, reps = top)
        assertEquals(pushUpRung + 1, after.rung(MovementPattern.HORIZONTAL_PUSH))
        val harder = exercise("horizontal_push.feet_30cm")
        assertEquals(harder.range.first, after.exercises.getValue(harder.id).target)
        assertEquals(ProgressNote.MOVED_UP, after.exercises.getValue(harder.id).note)
    }

    @Test
    fun fallingBelowTheRangeMovesDownARung() {
        val bottom = pushUp.range.first
        val after = settled(bottom).after(day = 0, target = bottom, reps = 3).after(day = 2, target = bottom, reps = 3)
        assertEquals(pushUpRung - 1, after.rung(MovementPattern.HORIZONTAL_PUSH))
    }

    @Test
    fun aStrongBestEffortSetWhileCalibratingJumpsRungs() {
        val box = exercise("horizontal_push.hands_61cm")
        val plan = hardPlan(box, box.range.first, bestEffort = true)
        val sets = plan.sets.map { LoggedSet(if (it.isBestEffort) 25 else it.target, 2, it.isBestEffort) }
        val after = coach.record(newcomer(), finished(0, ExerciseResult(plan, sets)))
        assertEquals(CoachTuning.MAX_CALIBRATION_JUMP, after.rung(MovementPattern.HORIZONTAL_PUSH))
    }

    @Test
    fun aBestEffortSetWhileCalibratingSetsWorkSetsAtSeventyPercent() {
        val plan = hardPlan(kneePushUp, kneePushUp.range.first, bestEffort = true)
        val sets = plan.sets.map { LoggedSet(if (it.isBestEffort) 10 else it.target, 2, it.isBestEffort) }
        val model = newcomer().copy(rungs = mapOf(MovementPattern.HORIZONTAL_PUSH to 1))
        val after = coach.record(model, finished(0, ExerciseResult(plan, sets)))
        assertEquals(7, after.exercises.getValue(kneePushUp.id).target)
        assertEquals(1, after.rung(MovementPattern.HORIZONTAL_PUSH))
    }

    @Test
    fun lightWorkNeverChangesATarget() {
        val light = done(hardPlan(pushUp, 8).lightened(), 6)
        val after = coach.record(settled(8), finished(0, light))
        assertEquals(settled(8).pushUps(), after.pushUps())
        assertEquals(0.0, after.history.single().hardSets.values.sum())
    }

    @Test
    fun fastWeightLossHoldsTargetsWhereTheyAre() {
        val losing = listOf(0 to 80.0, 7 to 78.8, 14 to 77.6)
            .fold(settled(8)) { model, (day, kg) -> model.withWeighIn(WeighIn(atDay(day), kg)) }
        assertEquals(8, losing.after(day = 15, target = 8, reps = 8).pushUps().target)
    }

    @Test
    fun anExerciseThatIsNotOnThePersonsRungOnlyMovesItsOwnTarget() {
        // Done from the Train tab: the push-up, by someone the coach has on knee push-ups.
        val model = newcomer().copy(
            rungs = mapOf(MovementPattern.HORIZONTAL_PUSH to 1),
            exercises = mapOf(pushUp.id to ExerciseState(target = 12, timesTrained = 5)),
        )
        val after = coach.record(model, finished(0, done(hardPlan(pushUp, 12), 12)))
        assertEquals(1, after.rung(MovementPattern.HORIZONTAL_PUSH))
    }

    @Test
    fun skillWorkRaisesTheGatesBest() {
        val gate = SkillCatalogue.firstPullUp.gates.first()
        val hang = assertNotNull(SkillTraining.exerciseFor(gate))
        val plan = hardPlan(hang, 10).copy(gateId = gate.id)
        val after = coach.record(newcomer(), finished(0, done(plan, 14)))
        assertEquals(14, after.gateBests[gate.id])
    }

    @Test
    fun bandAssistedSetsDoNotCountTowardAGate() {
        val gate = SkillCatalogue.firstPullUp.gates.last()
        val pullUp = assertNotNull(SkillTraining.exerciseFor(gate))
        val plan = hardPlan(pullUp, 1).copy(gateId = gate.id)
        val assisted =
            ExerciseResult(plan, plan.sets.map { LoggedSet(3, 2, isBestEffort = false, assistanceKg = 20.0) })
        assertEquals(0, coach.record(newcomer(), finished(0, assisted)).gateBests[gate.id])
    }

    @Test
    fun passingAGateFlagsTheNextOneToBeExplained() {
        val (first, second) = SkillCatalogue.firstPullUp.gates
        val hang = assertNotNull(SkillTraining.exerciseFor(first))
        val plan = hardPlan(hang, 20).copy(gateId = first.id)
        val after = coach.record(newcomer(), finished(0, done(plan, first.target)))
        assertEquals(ProgressNote.MOVED_UP, after.exercises.getValue(second.id).note)
    }

    @Test
    fun skippingAnExerciseIsRememberedAndChangesNothingElse() {
        val skipped = ExerciseResult(hardPlan(pushUp, 8), emptyList(), skipped = true)
        val after = coach.record(settled(8), finished(0, skipped))
        assertEquals(1, after.skips[pushUp.id])
        assertEquals(settled(8).pushUps(), after.pushUps())
        assertTrue(after.history.single().exerciseIds.isEmpty())
    }

    @Test
    fun theWeekCountsSetsDoneNotSetsPlanned() {
        val plan = hardPlan(pushUp, 8)
        val oneSet = ExerciseResult(plan, listOf(LoggedSet(8, 2, isBestEffort = false)))
        val record = coach.record(settled(8), finished(0, oneSet)).history.single()
        assertEquals(1.0, record.hardSets[MovementPattern.HORIZONTAL_PUSH])
        assertEquals(0.5, record.hardSets[MovementPattern.VERTICAL_PUSH])
        assertEquals(mapOf(MovementPattern.HORIZONTAL_PUSH to 1), record.directSets)
    }

    @Test
    fun onlyTheLastFewResultsAreKept() {
        val model = (0..9).fold(settled(8)) { model, index ->
            model.after(index * 2, target = 8, reps = 6, effort = Effort.HARD)
        }
        assertEquals(CoachTuning.MARKS_KEPT, model.pushUps().marks.size)
    }

    @Test
    fun historyOlderThanTheModelNeedsIsDropped() {
        val old = settled(8).after(day = 0, target = 8, reps = 8)
        val later = old.after(day = CoachTuning.HISTORY_DAYS + 5, target = 9, reps = 9)
        assertEquals(1, later.history.size)
    }
}
