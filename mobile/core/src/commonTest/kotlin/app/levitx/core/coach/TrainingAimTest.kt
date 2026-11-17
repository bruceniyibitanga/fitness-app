package app.levitx.core.coach

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** What the ladder work is tuned for: rest, effort, volume and where each exercise moves on. */
class TrainingAimTest {
    private val coach = Coach()
    private val pushUp = exercise("horizontal_push.push_up")

    /** Someone settled on push-ups at [target], training for [aim], with no load kit. */
    private fun onPushUps(aim: TrainingAim, target: Int): AthleteModel {
        val rung = ExerciseCatalogue.ladder(pushUp.pattern).rungs.indexOfFirst { pushUp in it.options }
        return newcomer(pullUpProfile().copy(aim = aim)).copy(
            rungs = mapOf(pushUp.pattern to rung),
            exercises = mapOf(pushUp.id to ExerciseState(target = target, best = target, timesTrained = 5)),
        )
    }

    private fun AthleteModel.afterHitting(target: Int): AthleteModel =
        coach.record(this, finished(0, done(hardPlan(pushUp, target), target)))

    @Test
    fun eachAimMovesOnAtItsOwnPointInTheRange() {
        // Push-ups run 6 to 12.
        assertEquals(12, TrainingAim.BALANCED.top(pushUp))
        assertEquals(12, TrainingAim.MUSCLE.top(pushUp))
        assertEquals(9, TrainingAim.STRENGTH.top(pushUp))
        assertEquals(15, TrainingAim.CONDITIONING.top(pushUp))
    }

    @Test
    fun theTopNeverLeavesTheRangeOrPassesTheCeiling() {
        ExerciseCatalogue.ladders.flatMap { ladder -> ladder.rungs.flatMap { it.options } }.forEach { exercise ->
            TrainingAim.entries.forEach { aim ->
                assertTrue(aim.top(exercise) in exercise.range.first..exercise.ceiling, "${aim.name} ${exercise.id}")
            }
        }
        val negative = exercise("vertical_pull.negative")
        assertEquals(
            negative.range.last,
            TrainingAim.CONDITIONING.top(negative),
            "negatives never climb past their range",
        )
    }

    @Test
    fun trainingForStrengthMovesUpSoonerThanBalanced() {
        val balanced = onPushUps(TrainingAim.BALANCED, 9).afterHitting(9)
        assertEquals(10, balanced.exercises.getValue(pushUp.id).target)
        val strength = onPushUps(TrainingAim.STRENGTH, 9).afterHitting(9)
        assertTrue(strength.rung(pushUp.pattern) > balanced.rung(pushUp.pattern))
    }

    @Test
    fun trainingForConditioningClimbsPastTheRangeBeforeMovingUp() {
        val after = onPushUps(TrainingAim.CONDITIONING, 12).afterHitting(12)
        assertEquals(onPushUps(TrainingAim.CONDITIONING, 12).rung(pushUp.pattern), after.rung(pushUp.pattern))
        assertEquals(13, after.exercises.getValue(pushUp.id).target)
    }

    @Test
    fun restAndEffortFollowTheAim() {
        val plans = TrainingAim.entries.associateWith { aim ->
            checkNotNull(coach.workout(onPushUps(aim, 8), WorkoutFocus.PUSH, atDay(0))).plans.first {
                it.exercise ==
                    pushUp
            }
        }
        assertEquals(120, plans.getValue(TrainingAim.BALANCED).restSeconds)
        assertEquals(180, plans.getValue(TrainingAim.STRENGTH).restSeconds)
        assertEquals(60, plans.getValue(TrainingAim.CONDITIONING).restSeconds)
        assertEquals(1, plans.getValue(TrainingAim.MUSCLE).repsInReserve)
        assertEquals(2, plans.getValue(TrainingAim.BALANCED).repsInReserve)
    }

    @Test
    fun trainingForMuscleAsksForMoreWeeklySetsWithinTheLimit() {
        val roomy = pullUpProfile().copy(sessionsPerWeek = 5, sessionMinutes = 90)
        val balanced = VolumeTargets.weekly(roomy).values.sum()
        val muscle = VolumeTargets.weekly(roomy.copy(aim = TrainingAim.MUSCLE))
        assertTrue(muscle.values.sum() > balanced)
        assertTrue(muscle.values.all { it <= CoachTuning.MAX_WEEKLY_HARD_SETS })
    }

    @Test
    fun shortRestFitsMoreSetsIntoTheSameWeek() {
        val short = pullUpProfile().copy(sessionsPerWeek = 2, sessionMinutes = 20)
        val balanced = VolumeTargets.weekly(short).values.sum()
        val conditioning = VolumeTargets.weekly(short.copy(aim = TrainingAim.CONDITIONING)).values.sum()
        val strength = VolumeTargets.weekly(short.copy(aim = TrainingAim.STRENGTH)).values.sum()
        assertTrue(conditioning > balanced && balanced > strength)
    }

    @Test
    fun switchingAimsKeepsStoredTargetsValid() {
        // A target from a balanced plan past the strength top simply means "move on" next time it's hit.
        val switched = onPushUps(TrainingAim.STRENGTH, 11).afterHitting(11)
        assertTrue(switched.rung(pushUp.pattern) > onPushUps(TrainingAim.STRENGTH, 11).rung(pushUp.pattern))
    }
}
