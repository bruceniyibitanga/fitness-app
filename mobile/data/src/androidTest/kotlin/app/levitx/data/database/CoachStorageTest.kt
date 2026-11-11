package app.levitx.data.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Checks the coach's tables and the queries it relies on. Uses an in-memory database. */
@RunWith(AndroidJUnit4::class)
class CoachStorageTest {
    private lateinit var database: LevitxDatabase

    @Before
    fun open() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, LevitxDatabase::class.java).build()
    }

    @After
    fun close() = database.close()

    @Test
    fun theCoachStateIsOneRowThatIsReplaced() = runTest {
        assertNull(database.coachDao().state())
        database.coachDao().replaceState(CoachStateEntity(json = "{\"v\":1}", updatedAtEpochMillis = 1))
        database.coachDao().replaceState(CoachStateEntity(json = "{\"v\":2}", updatedAtEpochMillis = 2))
        assertEquals("{\"v\":2}", database.coachDao().observeState().first()?.json)
    }

    @Test
    fun theWorkoutInProgressIsReplacedAndCleared() = runTest {
        database.coachDao().replaceDraft(WorkoutDraftEntity(json = "a"))
        database.coachDao().replaceDraft(WorkoutDraftEntity(json = "b"))
        assertEquals("b", database.coachDao().observeDraft().first()?.json)
        database.coachDao().clearDraft()
        assertNull(database.coachDao().observeDraft().first())
    }

    @Test
    fun aWorkoutIsFoundByItsId() = runTest {
        assertFalse(database.sessionDao().hasWorkout("w1"))
        database.sessionDao().insertSession(TrainingSessionEntity("w1/0", "squat.bodyweight", 0, 1, workoutId = "w1"))
        assertTrue(database.sessionDao().hasWorkout("w1"))
        assertFalse(database.sessionDao().hasWorkout("w2"))
    }

    @Test
    fun theGoalCanBeClearedForGeneralFitness() = runTest {
        database.goalDao().replace(ActiveGoalEntity(pathId = "first_pull_up", chosenAtEpochMillis = 0))
        database.goalDao().clear()
        assertNull(database.goalDao().observe().first())
    }

    @Test
    fun bodyweightReadingsSinceATimeComeOldestFirst() = runTest {
        database.equipmentDao().insertBodyweight(BodyweightEntity(kg = 81.0, recordedAtEpochMillis = 5))
        database.equipmentDao().insertBodyweight(BodyweightEntity(kg = 80.0, recordedAtEpochMillis = 30))
        database.equipmentDao().insertBodyweight(BodyweightEntity(kg = 80.5, recordedAtEpochMillis = 20))
        assertEquals(listOf(80.5, 80.0), database.equipmentDao().bodyweightSince(10).map { it.kg })
        assertEquals(listOf(80.5, 80.0), database.equipmentDao().observeBodyweightSince(10).first().map { it.kg })
    }
}
