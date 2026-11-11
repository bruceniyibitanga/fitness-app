package app.levitx.core.coach

import app.levitx.core.skills.SkillCatalogue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WorkoutFocusTest {
    private val coach = Coach()

    @Test
    fun everyFocusGivesAWorkoutForSomeoneNew() {
        WorkoutFocus.entries.forEach { focus ->
            val session = assertNotNull(coach.workout(newcomer(), focus, atDay(0)), "no $focus workout")
            assertTrue(session.plans.all { it.exercise.pattern in focus.patterns }, "$focus trained something else")
        }
    }

    @Test
    fun aPullWorkoutStartsWithTheGateBeingWorkedOn() {
        val session = assertNotNull(coach.workout(newcomer(), WorkoutFocus.PULL, atDay(0)))
        assertEquals(newcomer().currentGate()?.id, session.plans.first().gateId)
    }

    @Test
    fun aLegsWorkoutLeavesTheGateAlone() {
        val session = assertNotNull(coach.workout(newcomer(), WorkoutFocus.LEGS, atDay(0)))
        assertTrue(session.plans.none { it.gateId != null })
    }

    @Test
    fun coreOnlyTrainsEveryExerciseOnTheCoreRung() {
        val session = assertNotNull(coach.workout(newcomer(), WorkoutFocus.CORE_ONLY, atDay(0)))
        val rung = ExerciseCatalogue.ladder(MovementPattern.CORE).rungs.first().options
        assertEquals(rung.map { it.id }, session.plans.map { it.exercise.id })
    }

    @Test
    fun aMovementTrainedYesterdayComesBackLight() {
        val squat = exercise("squat.box")
        val model = coach.record(newcomer(), finished(day = 0, done(hardPlan(squat, 10), 10)))
        val session = assertNotNull(coach.workout(model, WorkoutFocus.LEGS, atDay(1)))
        val squatPlan = session.plans.first { it.exercise.pattern == MovementPattern.SQUAT }
        assertFalse(squatPlan.isHard)
        assertTrue(session.plans.first { it.exercise.pattern == MovementPattern.HINGE }.isHard)
    }

    @Test
    fun everyExerciseTheCoachCanPlanHasAGuide() {
        val gateExercises = SkillCatalogue.all.flatMap { it.gates }.mapNotNull(SkillTraining::exerciseFor)
        (ExerciseCatalogue.all + gateExercises).forEach { exercise ->
            val guide = assertNotNull(ExerciseGuides.forExercise(exercise.id), "no guide for ${exercise.id}")
            assertTrue(guide.cues.size in 2..3, "${exercise.id} should have two or three cues")
        }
    }
}
