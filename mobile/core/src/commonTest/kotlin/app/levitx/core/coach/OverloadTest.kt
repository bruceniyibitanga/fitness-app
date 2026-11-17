package app.levitx.core.coach

import app.levitx.core.load.LoadKit
import app.levitx.core.session.LoggedSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * What happens when someone beats the top of an exercise's range: more weight
 * if the exercise takes it and the kit has a step, else the next rung, and at
 * the top of the ladder more reps and then a slower lowering.
 */
class OverloadTest {
    private val coach = Coach()

    // A push-up that takes a vest: feet on a 61 cm box.
    private val top = exercise("horizontal_push.feet_61cm")

    // The hardest push-up on the ladder, which takes no weight.
    private val summit = exercise("horizontal_push.one_arm")
    private val vest = LoadKit(vestMaxKg = 20.0, vestStepKg = 1.0)

    /** Someone settled on [exercise]'s rung with [state], owning [kit]. */
    private fun on(exercise: Exercise, state: ExerciseState, kit: LoadKit = LoadKit()): AthleteModel {
        val rung = ExerciseCatalogue.ladder(exercise.pattern).rungs.indexOfFirst { exercise in it.options }
        return newcomer(pullUpProfile().copy(loadKit = kit)).copy(
            rungs = mapOf(exercise.pattern to rung),
            exercises = mapOf(exercise.id to state),
        )
    }

    /** Someone settled on the box push-up with [state], owning [kit]. */
    private fun atTop(state: ExerciseState, kit: LoadKit = LoadKit()): AthleteModel = on(top, state, kit)

    private fun settled(target: Int, addedKg: Double = 0.0, slow: Boolean = false) =
        ExerciseState(target = target, best = target, timesTrained = 5, addedKg = addedKg, slowLowering = slow)

    /** [model] after a session of three work sets of [reps] on [exercise], each carrying [usedKg]. */
    private fun AthleteModel.after(
        day: Int,
        reps: Int,
        prescribedKg: Double,
        usedKg: Double = prescribedKg,
        exercise: Exercise = top,
    ): AthleteModel {
        val state = exercises.getValue(exercise.id)
        val plan = hardPlan(exercise, state.target).copy(addedKg = prescribedKg)
        val sets = plan.sets.map { LoggedSet(reps, 2, false, addedKg = usedKg) }
        return coach.record(this, finished(day, ExerciseResult(plan, sets)))
    }

    private fun AthleteModel.topState(): ExerciseState = exercises.getValue(top.id)

    private fun AthleteModel.summitState(): ExerciseState = exercises.getValue(summit.id)

    @Test
    fun withAVestToppingOutAddsTheSmallestStepOfTheRightSize() {
        val after = atTop(settled(top.range.last), vest).after(0, top.range.last, prescribedKg = 0.0)
        val state = after.topState()
        // With no weight entered, steps are sized for a 70 kg person: 1 kg is under 2% of what a rep moves, 2 kg isn't.
        assertEquals(2.0, state.addedKg)
        assertEquals(top.range.first, state.target)
        assertEquals(ProgressNote.WEIGHT_UP, state.note)
        assertEquals(
            atTop(settled(0)).rung(MovementPattern.HORIZONTAL_PUSH),
            after.rung(MovementPattern.HORIZONTAL_PUSH),
        )
    }

    @Test
    fun withWeightTheRepsBuildBackUpBeforeMoreGoesOn() {
        val after = atTop(settled(top.range.first, addedKg = 2.0), vest).after(0, top.range.first, prescribedKg = 2.0)
        assertEquals(2.0, after.topState().addedKg)
        assertEquals(top.range.first + 1, after.topState().target)
    }

    @Test
    fun atTheTopOfTheLadderTheTargetClimbsPastTheRangeToTheCeiling() {
        val after = on(summit, settled(summit.range.last), vest)
            .after(0, summit.range.last, prescribedKg = 0.0, exercise = summit)
        assertEquals(summit.range.last + 1, after.summitState().target)
        assertEquals(ProgressNote.TARGET_UP, after.summitState().note)
        assertEquals(0.0, after.summitState().addedKg, "one-arm push-ups never take weight")
    }

    @Test
    fun atTheCeilingTheLoweringSlowsDown() {
        val after = on(summit, settled(summit.ceiling)).after(0, summit.ceiling, prescribedKg = 0.0, exercise = summit)
        val state = after.summitState()
        assertTrue(state.slowLowering)
        assertEquals(summit.range.last, state.target)
        assertEquals(ProgressNote.SLOWER, state.note)
    }

    @Test
    fun aVestTooHeavyForOneStepIsNotUsedAndTheNextRungIs() {
        // A fixed 20 kg vest is far more than 10% of what a push-up moves.
        val start = atTop(settled(top.range.last), LoadKit(vestMaxKg = 20.0))
        val after = start.after(0, top.range.last, 0.0)
        assertEquals(0.0, after.topState().addedKg)
        assertTrue(after.rung(MovementPattern.HORIZONTAL_PUSH) > start.rung(MovementPattern.HORIZONTAL_PUSH))
    }

    @Test
    fun onceTheVestIsFullTheNextRungStartsAtBodyweight() {
        val start = atTop(settled(top.range.last, addedKg = 20.0), vest)
        val after = start.after(0, top.range.last, 20.0)
        assertTrue(after.rung(MovementPattern.HORIZONTAL_PUSH) > start.rung(MovementPattern.HORIZONTAL_PUSH))
        val next = checkNotNull(after.exerciseFor(MovementPattern.HORIZONTAL_PUSH, emptySet()))
        assertEquals(0.0, after.exercises.getValue(next.id).addedKg)
    }

    @Test
    fun easingBackTakesWeightOffFirst() {
        val start = atTop(settled(top.range.first, addedKg = 4.0), vest)
        val after = start.after(0, top.range.first - 1, 4.0).after(2, top.range.first - 1, 4.0)
        assertEquals(3.0, after.topState().addedKg)
        assertEquals(top.range.last, after.topState().target)
        assertEquals(ProgressNote.EASED_BACK, after.topState().note)
    }

    @Test
    fun easingBackFromASlowLoweringGoesBackToTheUsualPace() {
        val start = on(summit, settled(summit.range.first, slow = true))
        val after = start.after(0, summit.range.first - 1, 0.0, exercise = summit)
            .after(2, summit.range.first - 1, 0.0, exercise = summit)
        assertFalse(after.summitState().slowLowering)
        assertEquals(summit.range.last, after.summitState().target)
    }

    @Test
    fun setsDoneLighterThanPrescribedDoNotHitTheTarget() {
        val start = atTop(settled(8, addedKg = 4.0), vest)
        val after = start.after(0, reps = 8, prescribedKg = 4.0, usedKg = 0.0)
        assertEquals(8, after.topState().target)
        assertEquals(4.0, after.topState().addedKg)
    }

    @Test
    fun moreWeightThanPrescribedIsTakenAsTheirs() {
        val start = atTop(settled(8, addedKg = 4.0), vest)
        val after = start.after(0, reps = 8, prescribedKg = 4.0, usedKg = 6.0)
        assertEquals(6.0, after.topState().addedKg)
        assertEquals(9, after.topState().target)
    }

    @Test
    fun withNoWeightKitToppingOutMovesUpARung() {
        val pushUp = exercise("horizontal_push.push_up")
        val model = on(pushUp, settled(pushUp.range.last))
        val after = coach.record(model, finished(0, done(hardPlan(pushUp, pushUp.range.last), pushUp.range.last)))
        assertEquals(0.0, after.exercises.getValue(pushUp.id).addedKg)
        assertTrue(after.rung(MovementPattern.HORIZONTAL_PUSH) > model.rung(MovementPattern.HORIZONTAL_PUSH))
    }

    @Test
    fun aBestEffortSetNeverAddsWeight() {
        val pushUp = exercise("horizontal_push.push_up")
        val model = on(pushUp, ExerciseState(target = pushUp.range.first), vest)
        val effort = hardPlan(pushUp, pushUp.range.first, bestEffort = true)
        val sets = effort.sets.dropLast(1).map { LoggedSet(pushUp.range.first, 2, false) } +
            LoggedSet(pushUp.range.last * 2, 1, false)
        val after = coach.record(model, finished(0, ExerciseResult(effort, sets)))
        assertTrue(after.exercises.values.all { it.addedKg == 0.0 })
    }

    @Test
    fun theNextSessionPrescribesTheWeightAndTheSlowLowering() {
        val weighted = atTop(settled(top.range.first, addedKg = 3.0), vest)
        val plan = Coach().workout(weighted, WorkoutFocus.PUSH, atDay(0))?.plans?.first { it.exercise == top }
        assertEquals(3.0, plan?.addedKg)
        assertNull(plan?.slowLowerSeconds)

        val slow = on(summit, settled(summit.range.last, slow = true))
        val slowPlan = Coach().workout(slow, WorkoutFocus.PUSH, atDay(0))?.plans?.first { it.exercise == summit }
        assertEquals(CoachTuning.SLOW_LOWER_SECONDS, slowPlan?.slowLowerSeconds)
    }

    @Test
    fun weightTheKitCanNoLongerMakeIsDroppedNotMadeUp() {
        val model = atTop(settled(top.range.first, addedKg = 6.0), LoadKit(vestMaxKg = 5.0, vestStepKg = 2.5))
        val plan = Coach().workout(model, WorkoutFocus.PUSH, atDay(0))?.plans?.first { it.exercise == top }
        assertEquals(5.0, plan?.addedKg)
    }
}
