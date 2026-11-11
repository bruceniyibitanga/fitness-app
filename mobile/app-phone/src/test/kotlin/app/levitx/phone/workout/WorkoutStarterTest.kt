package app.levitx.phone.workout

import app.levitx.core.coach.Coach
import app.levitx.core.coach.SuggestionChoice
import app.levitx.testing.FakeTrainingClock
import app.levitx.testing.FakeWorkoutDraftRepository
import app.levitx.testing.newcomer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class WorkoutStarterTest {
    private val drafts = FakeWorkoutDraftRepository()
    private val clock = FakeTrainingClock()
    private val starter = WorkoutStarter(drafts, clock)
    private val session = checkNotNull(Coach().recommend(newcomer(), clock.now).session)

    @Test
    fun savesANewWorkoutWithTheChoice() = runTest {
        assertEquals(WorkoutStarter.Started.NEW, starter.start(session, SuggestionChoice.LIGHTER))
        val run = checkNotNull(drafts.current)
        assertEquals(session, run.session)
        assertEquals(clock.now, run.startedAtEpochMillis)
        assertEquals(SuggestionChoice.LIGHTER, run.choice)
    }

    @Test
    fun neverReplacesAWorkoutInProgress() = runTest {
        starter.start(session)
        val first = checkNotNull(drafts.current)
        val other = session.lightened()
        assertNotEquals(session, other)
        assertEquals(WorkoutStarter.Started.RESUMING, starter.start(other))
        assertEquals(first, drafts.current)
    }
}
