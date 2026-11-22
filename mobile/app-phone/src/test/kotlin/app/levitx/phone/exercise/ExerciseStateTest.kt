package app.levitx.phone.exercise

import app.levitx.core.coach.ExerciseCatalogue
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
    fun cardioAndMobilityMovesHaveAGuideButNoLadder() {
        listOf("cardio.burpees", "mobility.pigeon").forEach { id ->
            val state = exerciseState(newcomer(), id, nowEpochMillis = 0L) as ExerciseUiState.Ready
            assertNull("$id ladder", state.ladder)
            assertNotNull("$id guide", state.guide)
        }
    }

    @Test
    fun anUnknownIdIsNotFound() {
        assertEquals(ExerciseUiState.NotFound, exerciseState(newcomer(), "nope", nowEpochMillis = 0L))
    }

    /** A newcomer on the split squat rung, where a reverse lunge is the other option with no kit. */
    private val onSplitSquats = newcomer().copy(
        rungs = mapOf(
            MovementPattern.SQUAT to ExerciseCatalogue.ladder(MovementPattern.SQUAT).rungs.indexOfFirst { rung ->
                rung.options.any { it.id == "squat.split" }
            },
        ),
    )

    @Test
    fun theExerciseTheyTrainListsTheOthersAtItsLevel() {
        val state = exerciseState(onSplitSquats, "squat.split", nowEpochMillis = 0L) as ExerciseUiState.Ready
        assertEquals(SwapState.CURRENT, state.swap)
        assertEquals(listOf("squat.reverse_lunge"), state.alternatives.map { it.id })
    }

    @Test
    fun anotherExerciseAtTheirLevelCanBeSwappedIn() {
        val state = exerciseState(onSplitSquats, "squat.reverse_lunge", nowEpochMillis = 0L) as ExerciseUiState.Ready
        assertEquals(SwapState.AVAILABLE, state.swap)
        assertTrue(state.alternatives.isEmpty())
    }

    @Test
    fun exercisesAboveTheirLevelAndGatesCantBeSwapped() {
        listOf("squat.bulgarian", "first_pull_up.dead_hang", "cardio.burpees").forEach { id ->
            val state = exerciseState(onSplitSquats, id, nowEpochMillis = 0L) as ExerciseUiState.Ready
            assertEquals(id, SwapState.NONE, state.swap)
        }
    }
}
