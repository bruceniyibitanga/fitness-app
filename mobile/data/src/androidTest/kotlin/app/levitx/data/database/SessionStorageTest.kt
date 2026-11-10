package app.levitx.data.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Checks goals and sessions round-trip through Room. Uses an in-memory database. */
@RunWith(AndroidJUnit4::class)
class SessionStorageTest {
    private lateinit var database: LevitxDatabase

    @Before
    fun open() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, LevitxDatabase::class.java).build()
    }

    @After
    fun close() = database.close()

    @Test
    fun savingAGoalReplacesThePreviousOne() = runTest {
        database.goalDao().replace(ActiveGoalEntity(pathId = "muscle_up", chosenAtEpochMillis = 0))
        database.goalDao().replace(ActiveGoalEntity(pathId = "first_pull_up", chosenAtEpochMillis = 1))
        assertEquals("first_pull_up", database.goalDao().observe().first()?.pathId)
    }

    @Test
    fun sessionAndItsSetsAreSaved() = runTest {
        database.sessionDao().insertSession(TrainingSessionEntity("s1", "muscle_up.dips", 0, 1))
        database.sessionDao().insertSets(
            listOf(
                LoggedSetEntity("s1", 0, 5, 2, false),
                LoggedSetEntity("s1", 1, 8, 0, true),
            ),
        )
        assertEquals(1, database.sessionDao().count())
    }
}
