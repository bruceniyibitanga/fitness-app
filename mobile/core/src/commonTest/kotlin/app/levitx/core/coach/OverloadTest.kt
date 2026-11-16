package app.levitx.core.coach

import app.levitx.core.load.LoadKit
import app.levitx.core.session.LoggedSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What happens at the top of a ladder, where there is no harder exercise to move to. */
class OverloadTest {
    private val coach = Coach()

    // The hardest push-up on the ladder: feet on a 61 cm box.
    private val top = exercise("horizontal_push.feet_61cm")
    private val topRung = ExerciseCatalogue.ladder(MovementPattern.HORIZONTAL_PUSH).rungs.indexOfFirst {
        top in
            it.options
    }
    private val vest = LoadKit(vestMaxKg = 20.0, vestStepKg = 1.0)

    /** Someone settled at the top push-up with [state], owning [kit]. */
    private fun atTop(state: ExerciseState, kit: LoadKit = LoadKit()): AthleteModel =
        newcomer(pullUpProfile().copy(loadKit = kit)).copy(
            rungs = mapOf(MovementPattern.HORIZONTAL_PUSH to topRung),
            exercises = mapOf(top.id to state),
        )

    private fun settled(target: Int, addedKg: Double = 0.0, slow: Boolean = false) =
        ExerciseState(target = target, best = target, timesTrained = 5, addedKg = addedKg, slowLowering = slow)

    /** [model] after a session of three work sets of [reps], each carrying [usedKg], prescribed [prescribedKg]. */
    private fun AthleteModel.after(
        day: Int,
        reps: Int,
        prescribedKg: Double,
        usedKg: Double = prescribedKg,
    ): AthleteModel {
        val state = exercises.getValue(top.id)
        val plan = hardPlan(top, state.target).copy(addedKg = prescribedKg)
        val sets = plan.sets.map { LoggedSet(reps, 2, false, addedKg = usedKg) }
        return coach.record(this, finished(day, ExerciseResult(plan, sets)))
    }

    private fun AthleteModel.topState(): ExerciseState = exercises.getValue(top.id)

    @Test
    fun withAVestToppingOutAddsTheSmallestStepOfTheRightSize() {
        val after = atTop(settled(top.range.last), vest).after(0, top.range.last, prescribedKg = 0.0)
        val state = after.topState()
        // With no weight entered, steps are sized for a 70 kg person: 1 kg is under 2% of what a rep moves, 2 kg isn't.
        assertEquals(2.0, state.addedKg)
        assertEquals(top.range.first, state.target)
        assertEquals(ProgressNote.WEIGHT_UP, state.note)
    }

    @Test
    fun withWeightTheRepsBuildBackUpBeforeMoreGoesOn() {
        val after = atTop(settled(top.range.first, addedKg = 2.0), vest).after(0, top.range.first, prescribedKg = 2.0)
        assertEquals(2.0, after.topState().addedKg)
        assertEquals(top.range.first + 1, after.topState().target)
    }

    @Test
    fun withNoWeightTheTargetClimbsPastTheRangeToTheCeiling() {
        val after = atTop(settled(top.range.last)).after(0, top.range.last, prescribedKg = 0.0)
        assertEquals(top.range.last + 1, after.topState().target)
        assertEquals(ProgressNote.TARGET_UP, after.topState().note)
        assertEquals(0.0, after.topState().addedKg)
    }

    @Test
    fun atTheCeilingTheLoweringSlowsDown() {
        val after = atTop(settled(top.ceiling)).after(0, top.ceiling, prescribedKg = 0.0)
        val state = after.topState()
        assertTrue(state.slowLowering)
        assertEquals(top.range.last, state.target)
        assertEquals(ProgressNote.SLOWER, state.note)
    }

    @Test
    fun aVestTooHeavyForOneStepIsNotUsed() {
        // A fixed 20 kg vest is far more than 10% of what a push-up moves.
        val after = atTop(settled(top.range.last), LoadKit(vestMaxKg = 20.0)).after(0, top.range.last, 0.0)
        assertEquals(0.0, after.topState().addedKg)
        assertEquals(top.range.last + 1, after.topState().target)
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
        val start = atTop(settled(top.range.first, slow = true))
        val after = start.after(0, top.range.first - 1, 0.0).after(2, top.range.first - 1, 0.0)
        assertFalse(after.topState().slowLowering)
        assertEquals(top.range.last, after.topState().target)
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
    fun belowTheTopRungToppingOutStillMovesUpRatherThanAddingWeight() {
        val pushUp = exercise("horizontal_push.push_up")
        val rung = ExerciseCatalogue.ladder(MovementPattern.HORIZONTAL_PUSH).rungs.indexOfFirst { pushUp in it.options }
        val model = newcomer(pullUpProfile().copy(loadKit = vest)).copy(
            rungs = mapOf(MovementPattern.HORIZONTAL_PUSH to rung),
            exercises = mapOf(pushUp.id to settled(pushUp.range.last)),
        )
        val after = coach.record(model, finished(0, done(hardPlan(pushUp, pushUp.range.last), pushUp.range.last)))
        assertEquals(0.0, after.exercises.getValue(pushUp.id).addedKg)
        assertTrue(after.rung(MovementPattern.HORIZONTAL_PUSH) > rung)
    }

    @Test
    fun theNextSessionPrescribesTheWeightAndTheSlowLowering() {
        val weighted = atTop(settled(top.range.first, addedKg = 3.0), vest)
        val plan = Coach().workout(weighted, WorkoutFocus.PUSH, atDay(0))?.plans?.first { it.exercise == top }
        assertEquals(3.0, plan?.addedKg)
        assertNull(plan?.slowLowerSeconds)

        val slow = atTop(settled(top.range.last, slow = true))
        val slowPlan = Coach().workout(slow, WorkoutFocus.PUSH, atDay(0))?.plans?.first { it.exercise == top }
        assertEquals(CoachTuning.SLOW_LOWER_SECONDS, slowPlan?.slowLowerSeconds)
    }

    @Test
    fun weightTheKitCanNoLongerMakeIsDroppedNotMadeUp() {
        val model = atTop(settled(top.range.first, addedKg = 6.0), LoadKit(vestMaxKg = 5.0, vestStepKg = 2.5))
        val plan = Coach().workout(model, WorkoutFocus.PUSH, atDay(0))?.plans?.first { it.exercise == top }
        assertEquals(5.0, plan?.addedKg)
    }
}
