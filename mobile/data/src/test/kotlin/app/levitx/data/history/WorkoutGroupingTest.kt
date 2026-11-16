package app.levitx.data.history

import app.levitx.data.database.LoggedSetEntity
import app.levitx.data.database.SessionWithSets
import app.levitx.data.database.TrainingSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutGroupingTest {
    private fun row(id: String, exerciseId: String, finishedAt: Long, workoutId: String?, vararg values: Int) =
        SessionWithSets(
            session = TrainingSessionEntity(
                id = id,
                gateId = exerciseId,
                startedAtEpochMillis = finishedAt - 1_000,
                finishedAtEpochMillis = finishedAt,
                bodyweightKg = 80.0,
                workoutId = workoutId,
            ),
            // Room hands sets back in no particular order.
            sets = values.mapIndexed { position, value ->
                LoggedSetEntity(id, position, value, repsInReserve = 2, isBestEffort = false)
            }.reversed(),
        )

    @Test
    fun rowsOfOneWorkoutBecomeOneWorkoutInExerciseOrder() {
        val workouts = toWorkouts(
            listOf(
                row("w1/10", "core.plank", 5_000, "w1", 30),
                row("w1/2", "horizontal_push.push_up", 5_000, "w1", 10, 9),
                row("w1/0", "vertical_pull.pull_up", 5_000, "w1", 6),
            ),
        )
        val workout = workouts.single()
        assertEquals("w1", workout.id)
        assertEquals(
            listOf("vertical_pull.pull_up", "horizontal_push.push_up", "core.plank"),
            workout.exercises.map {
                it.exerciseId
            },
        )
        assertEquals(listOf(10, 9), workout.exercises[1].sets.map { it.value })
    }

    @Test
    fun aSessionWithoutAWorkoutIsAWorkoutOfItsOwnAndNewestComesFirst() {
        val workouts = toWorkouts(
            listOf(
                row("old", "first_pull_up.dead_hang", 1_000, null, 20),
                row("w2/0", "vertical_pull.pull_up", 9_000, "w2", 5),
            ),
        )
        assertEquals(listOf("w2", "old"), workouts.map { it.id })
    }
}
