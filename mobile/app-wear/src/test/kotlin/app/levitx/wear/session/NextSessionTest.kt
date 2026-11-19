package app.levitx.wear.session

import app.levitx.core.coach.PlanAhead
import app.levitx.core.skills.SkillCatalogue
import app.levitx.testing.FakeNextSessionRepository
import app.levitx.testing.FakeTrainingClock
import app.levitx.testing.skillSession
import app.levitx.wear.WatchFakes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The watch picks the session by its own clock, so a plan sent days ago is still the right one. */
class NextSessionTest {
    private val clock = FakeTrainingClock()
    private val repository = FakeNextSessionRepository()
    private val nextSession = NextSession(repository, clock)

    private val real = skillSession(SkillCatalogue.muscleUp.gates[1])
    private val light = real.lightened()

    /** A plan whose rested session takes over [inMillis] from now. */
    private fun planRestedIn(inMillis: Long) =
        PlanAhead(now = light, rested = real, restedAtEpochMillis = clock.now + inMillis)

    @Test
    fun withNoPlanThereIsNothingToStart() = runTest {
        assertNull(nextSession.current())
        assertNull(nextSession.observe().first())
    }

    @Test
    fun beforeTheRestIsOverItIsTheSessionForNow() = runTest {
        repository.replace(planRestedIn(36 * HOUR))

        assertEquals(light, nextSession.current())
        assertEquals(light, nextSession.observe().first())
    }

    @Test
    fun onceTheRestIsOverItIsTheRealSessionEvenIfThePhoneNeverSentAnythingNew() = runTest {
        repository.replace(planRestedIn(36 * HOUR))

        clock.now += 2 * 24 * HOUR

        assertEquals(real, nextSession.current())
        assertEquals(real, nextSession.observe().first())
    }

    @Test
    fun aScreenLeftOpenSwitchesToTheRealSessionWhenItsTimeComes() = runTest {
        val wait = 40L
        repository.replace(planRestedIn(wait))

        val shown = nextSession.observe()
            // The wall clock moves on while the screen waits.
            .onEachFirst { clock.now += wait }
            .take(2)
            .toList()

        assertEquals(listOf(light, real), shown)
    }

    @Test
    fun theWorkoutStartedIsTheOneForTheMomentItStarts() = runTest {
        val fakes = WatchFakes()
        val controller = fakes.controller()
        fakes.next.replace(PlanAhead(light, real, restedAtEpochMillis = fakes.clock.now + 36 * HOUR))

        fakes.clock.now += 37 * HOUR
        controller.start()

        assertEquals(real, (controller.state.value as WorkoutState.Running).run.session)
    }

    private companion object {
        const val HOUR = 3_600_000L
    }
}

/** Runs [action] when the first value arrives, and passes every value on. */
private fun <T> Flow<T>.onEachFirst(action: () -> Unit): Flow<T> {
    var first = true
    return flow {
        collect { value ->
            if (first) {
                first = false
                action()
            }
            emit(value)
        }
    }
}
