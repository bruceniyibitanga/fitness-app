package app.levitx.core.history

import app.levitx.core.coach.MovementPattern
import app.levitx.core.session.LoggedSet
import app.levitx.core.skills.SkillCatalogue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProgressTest {
    private val day = 24L * 60 * 60 * 1000

    private fun set(value: Int, addedKg: Double = 0.0, assistanceKg: Double = 0.0) =
        LoggedSet(value, repsInReserve = 2, isBestEffort = false, addedKg = addedKg, assistanceKg = assistanceKg)

    private fun workout(dayNumber: Int, vararg exercises: ExerciseRecord) = WorkoutRecord(
        id = "w$dayNumber",
        startedAtEpochMillis = dayNumber * day,
        finishedAtEpochMillis = dayNumber * day + 45 * 60_000L,
        exercises = exercises.toList(),
    )

    private fun pushUps(id: String, vararg sets: LoggedSet, bodyweightKg: Double? = 80.0) =
        ExerciseRecord("horizontal_push.$id", sets.toList(), bodyweightKg)

    @Test
    fun aWorkoutReportsItsLengthSetsAndKilograms() {
        val w = workout(1, pushUps("push_up", set(10), set(8)))
        assertEquals(45, w.minutes)
        assertEquals(2, w.sets)
        val perRep = checkNotNull(w.exercises.single().kgPerRep(set(1)))
        assertEquals(perRep * 18, checkNotNull(w.kgMoved), 1e-9)
    }

    @Test
    fun withoutBodyweightThereAreNoKilograms() {
        val w = workout(1, pushUps("push_up", set(10), bodyweightKg = null))
        assertNull(w.kgMoved)
        assertTrue(Progress.loadSeries(listOf(w), MovementPattern.HORIZONTAL_PUSH, 0).isEmpty())
    }

    @Test
    fun theLoadSeriesTakesTheHeaviestRepOfEachWorkoutAndMarksEachClimb() {
        val workouts = listOf(
            workout(3, pushUps("feet_30cm", set(6))),
            workout(1, pushUps("knees", set(12))),
            workout(2, pushUps("push_up", set(10)), pushUps("knees", set(15))),
            workout(4, pushUps("push_up", set(12))),
        )
        val series = Progress.loadSeries(workouts, MovementPattern.HORIZONTAL_PUSH, fromEpochMillis = 0)
        assertEquals(
            listOf("knees", "push_up", "feet_30cm", "push_up"),
            series.map {
                it.exerciseId.substringAfter('.')
            },
        )
        assertEquals(listOf(false, true, true, false), series.map { it.isNewRung })
        assertTrue(series.zipWithNext().take(2).all { (a, b) -> b.kgPerRep > a.kgPerRep })
    }

    @Test
    fun aVestAddsToTheLoadAndABandTakesFromIt() {
        val record = pushUps("push_up", set(8), set(8, addedKg = 10.0), set(8, assistanceKg = 10.0))
        val (plain, vest, band) = record.sets.map { checkNotNull(record.kgPerRep(it)) }
        assertTrue(vest > plain)
        assertTrue(band < plain)
        assertEquals(vest, record.topKgPerRep)
    }

    @Test
    fun visitsAreNewestFirst() {
        val workouts = listOf(workout(1, pushUps("knees", set(12))), workout(5, pushUps("knees", set(14))))
        assertEquals(listOf("w5", "w1"), Progress.visits(workouts, "horizontal_push.knees").map { it.workout.id })
    }

    @Test
    fun workoutsAreCountedInSevenDayWindowsBackFromNow() {
        val workouts = listOf(workout(1), workout(2), workout(9), workout(20))
        // Now is the end of day 20: weeks are days 0-6, 7-13 and 14-20, roughly.
        assertEquals(listOf(2, 1, 1), Progress.workoutsPerWeek(workouts, weeks = 3, nowEpochMillis = 21 * day))
    }

    @Test
    fun aGatePassedIsDatedByTheFirstStrictSetThatReachedIt() {
        val gate = checkNotNull(SkillCatalogue.gateById("first_pull_up.scapular_pulls"))
        val workouts = listOf(
            workout(1, ExerciseRecord(gate.id, listOf(set(10, assistanceKg = 10.0)), 80.0)),
            workout(2, ExerciseRecord(gate.id, listOf(set(10)), 80.0)),
            workout(3, ExerciseRecord(gate.id, listOf(set(11)), 80.0)),
        )
        val hang = SkillCatalogue.firstPullUp.gates.first()
        val passed = Progress.passedGates(workouts, listOf(gate, hang))
        assertEquals(listOf(hang, gate), passed.map { it.gate })
        assertNull(passed.first().atEpochMillis)
        assertEquals(workouts[1].finishedAtEpochMillis, passed.last().atEpochMillis)
    }
}
