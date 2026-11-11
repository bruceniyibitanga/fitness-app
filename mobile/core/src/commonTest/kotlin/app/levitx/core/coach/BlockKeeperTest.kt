package app.levitx.core.coach

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlockKeeperTest {
    private val coach = Coach()
    private val squat = exercise("squat.bodyweight")
    private val perWeek = 3
    private val blockSessions = perWeek * CoachTuning.BLOCK_WEEKS

    /** Trains squats every other day for [sessions] sessions, starting on [fromDay]. */
    private fun AthleteModel.train(sessions: Int, fromDay: Int = 0): AthleteModel =
        (0 until sessions).fold(this) { model, index ->
            coach.record(model, finished(fromDay + index * 2, done(hardPlan(squat, 12), 10, Effort.HARD)))
        }

    @Test
    fun aBlockIsCountedInSessions() {
        val almost = newcomer().train(blockSessions - 1)
        assertFalse(almost.block.isLighterWeek)
        assertEquals(blockSessions - 1, almost.block.sessionsDone)
    }

    @Test
    fun theLighterWeekFollowsTheLastSessionOfTheBlock() {
        val block = newcomer().train(blockSessions).block
        assertTrue(block.isLighterWeek)
        assertEquals(perWeek, block.lighterSessionsLeft)
    }

    @Test
    fun aNewBlockStartsAfterTheLighterWeek() {
        val block = newcomer().train(blockSessions + perWeek).block
        assertEquals(TrainingBlock(index = 1), block)
    }

    @Test
    fun timeAwayCountsAsTheLighterWeek() {
        val resting = newcomer().train(blockSessions)
        val back = resting.train(1, fromDay = blockSessions * 2 + CoachTuning.COMEBACK_AFTER_DAYS + 1)
        assertEquals(TrainingBlock(index = 1), back.block)
    }

    @Test
    fun accessoryExercisesRotateWithTheBlockAndAreFlagged() {
        val model = newcomer().copy(rungs = mapOf(MovementPattern.SQUAT to 2))
        val goal = setOf(MovementPattern.VERTICAL_PULL)
        val before = model.exerciseFor(MovementPattern.SQUAT, goal)
        val next = model.train(blockSessions + perWeek)
        val after = next.exerciseFor(MovementPattern.SQUAT, goal)
        assertTrue(before != after)
        assertEquals(ProgressNote.NEW_THIS_BLOCK, next.exercises[after?.id]?.note)
    }

    @Test
    fun goalPatternsKeepTheSameExerciseEveryBlock() {
        val model = newcomer().copy(rungs = mapOf(MovementPattern.SQUAT to 2))
        val goal = setOf(MovementPattern.SQUAT)
        assertEquals(
            model.exerciseFor(MovementPattern.SQUAT, goal, 0),
            model.exerciseFor(MovementPattern.SQUAT, goal, 1),
        )
    }
}
