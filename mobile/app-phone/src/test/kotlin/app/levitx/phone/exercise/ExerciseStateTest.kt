package app.levitx.phone.exercise

import app.levitx.core.coach.MovementPattern
import app.levitx.testing.newcomer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseStateTest {
    @Test
    fun aLadderExerciseShowsItsLadderWithThePersonsPlace() {
        // Floor push-ups are the fifth rung: wall, 61 cm box, knees, 30 cm box, floor.
        val model = newcomer().copy(rungs = mapOf(MovementPattern.HORIZONTAL_PUSH to 4))
        val state = exerciseState(model, "horizontal_push.push_up", nowEpochMillis = 0L) as ExerciseUiState.Ready
        val ladder = checkNotNull(state.ladder)
        assertEquals(RungStatus.CURRENT, ladder[4].status)
        assertTrue(ladder[4].holdsShown)
        assertEquals(RungStatus.DONE, ladder[0].status)
        assertEquals(RungStatus.LATER, ladder.last().status)
        assertNotNull(state.guide)
    }

    @Test
    fun aGateExerciseHasNoLadder() {
        val state = exerciseState(newcomer(), "first_pull_up.dead_hang", nowEpochMillis = 0L) as ExerciseUiState.Ready
        assertNull(state.ladder)
        assertNull(state.target)
    }

    @Test
    fun anUnknownIdIsNotFound() {
        assertEquals(ExerciseUiState.NotFound, exerciseState(newcomer(), "nope", nowEpochMillis = 0L))
    }
}
