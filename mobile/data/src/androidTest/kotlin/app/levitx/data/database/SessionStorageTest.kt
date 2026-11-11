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

    @Test
    fun aSessionIdIsOnlyStoredOnce() = runTest {
        val session = TrainingSessionEntity("s1", "muscle_up.dips", 0, 1)
        check(database.sessionDao().insertSessionIfNew(session) != -1L) { "first insert should store the session" }
        assertEquals(-1L, database.sessionDao().insertSessionIfNew(session.copy(finishedAtEpochMillis = 99)))
        assertEquals(1, database.sessionDao().count())
    }

    @Test
    fun queuedSessionsComeBackWithTheirSetsUntilDelivered() = runTest {
        database.sessionDao().insertSession(TrainingSessionEntity("s1", "muscle_up.dips", 0, 1))
        database.sessionDao().insertSets(
            listOf(LoggedSetEntity("s1", 0, 5, 2, false), LoggedSetEntity("s1", 1, 8, 0, true)),
        )
        database.sessionOutboxDao().queue(SessionOutboxEntity("s1", queuedAtEpochMillis = 5))
        // Queuing twice is harmless.
        database.sessionOutboxDao().queue(SessionOutboxEntity("s1", queuedAtEpochMillis = 6))

        val queued = database.sessionOutboxDao().observeQueued().first()
        assertEquals(listOf("s1"), queued.map { it.session.id })
        assertEquals(2, queued.single().sets.size)

        database.sessionOutboxDao().remove("s1")
        assertEquals(emptyList<SessionWithSets>(), database.sessionOutboxDao().observeQueued().first())
        assertEquals(1, database.sessionDao().count())
    }

    @Test
    fun theSessionInProgressIsReplacedAndCleared() = runTest {
        val draft = SessionDraftEntity(
            sessionId = "s1",
            gateId = "muscle_up.dips",
            previousBest = 6,
            startedAtEpochMillis = 0,
            restSeconds = 120,
            repsInReserve = 2,
            restEndsAtElapsedMillis = null,
            addedKg = 0.0,
            assistanceKg = 0.0,
        )
        val sets = listOf(
            SessionDraftSetEntity(0, 4, false, null, null, 0.0, 0.0),
            SessionDraftSetEntity(1, 7, true, null, null, 0.0, 0.0),
        )
        database.sessionDraftDao().replace(SessionDraftRows(draft, sets))
        database.sessionDraftDao().replace(SessionDraftRows(draft, listOf(sets[0].copy(value = 4), sets[1])))

        val loaded = checkNotNull(database.sessionDraftDao().load()) { "the draft should be stored" }
        assertEquals(listOf(4, null), loaded.sets.map { it.value })

        database.sessionDraftDao().clear()
        assertEquals(null, database.sessionDraftDao().load())
    }
}
