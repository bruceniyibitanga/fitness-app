package app.levitx.wear.session

import app.levitx.core.session.SessionPlanner
import app.levitx.core.session.SessionRun
import app.levitx.core.session.SetLoad
import app.levitx.core.skills.SkillCatalogue
import app.levitx.data.DataUnavailableException
import app.levitx.data.session.SessionDraftRepository
import app.levitx.testing.FakeGoalRepository
import app.levitx.testing.FakeProgressRepository
import app.levitx.testing.FakeSessionDraftRepository
import app.levitx.testing.FakeSessionRepository
import app.levitx.testing.FakeTrainingClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutControllerTest {
    private val path = SkillCatalogue.muscleUp
    private val dips = path.gates[1]
    private val deadHang = path.gates[0]
    private val clock = FakeTrainingClock()
    private val progress = FakeProgressRepository(mapOf(deadHang.id to 30, dips.id to 6))
    private val sessions = FakeSessionRepository(progress)
    private val drafts = FakeSessionDraftRepository()

    // Unconfined, so each action has finished by the time the call returns.
    private val scope = CoroutineScope(UnconfinedTestDispatcher())

    private fun controller(
        goal: FakeGoalRepository = FakeGoalRepository(path),
        draftRepository: SessionDraftRepository = drafts,
    ) = WorkoutController(goal, progress, draftRepository, sessions, clock, scope)

    private fun WorkoutController.run(): SessionRun = (state.value as WorkoutState.Running).run

    @Test
    fun startsIdleWhenNothingWasInProgress() {
        assertEquals(WorkoutState.Idle(), controller().state.value)
    }

    @Test
    fun startingPlansTodayFromTheCurrentBestAndSavesADraft() {
        val controller = controller()
        controller.start()

        assertEquals(SessionPlanner.planFor(dips, best = 6), controller.run().plan)
        assertEquals(6, controller.run().previousBest)
        assertEquals(controller.run(), drafts.current)
    }

    @Test
    fun startingWithNoGoalStaysIdle() {
        val controller = controller(goal = FakeGoalRepository())
        controller.start()
        assertEquals(WorkoutState.Idle(), controller.state.value)
    }

    @Test
    fun startingAgainCarriesOnWithTheSameSession() {
        val controller = controller()
        controller.start()
        controller.logSet(4)
        val before = controller.run()

        controller.start()

        assertEquals(before, controller.run())
    }

    @Test
    fun loggingASetStartsRestAndSavesTheDraft() {
        val controller = controller()
        controller.start()
        controller.logSet(4)

        val run = controller.run()
        assertEquals(clock.elapsed + SessionPlanner.REST_SECONDS_REPS * 1_000L, run.restEndsAtElapsedMillis)
        assertEquals(1, run.logged.size)
        assertNull(run.logged.single().repsInReserve)
        assertEquals(run, drafts.current)
    }

    @Test
    fun theLastSetSavesAndQueuesTheSessionAndClearsTheDraft() {
        val controller = controller()
        controller.start()
        val sessionId = controller.run().id
        repeat(SessionPlanner.WORK_SETS) {
            controller.logSet(4)
            controller.endRest()
        }
        controller.logSet(8)

        val finished = controller.state.value as WorkoutState.Finished
        assertEquals(6, finished.previousBest)
        assertEquals(8, finished.sessionBest)
        assertTrue(finished.isNewBest)
        assertEquals(listOf(sessionId), sessions.queued.map { it.id })
        assertEquals(8, progress.current[dips.id])
        assertNull(drafts.current)
    }

    @Test
    fun endingEarlySavesWhatWasDone() {
        val controller = controller()
        controller.start()
        controller.logSet(5)
        controller.finishEarly()

        assertTrue(controller.state.value is WorkoutState.Finished)
        assertEquals(1, sessions.sessions.single().sets.size)
    }

    @Test
    fun endingBeforeAnySetSavesNothing() {
        val controller = controller()
        controller.start()
        controller.finishEarly()

        assertEquals(WorkoutState.Idle(), controller.state.value)
        assertTrue(sessions.sessions.isEmpty())
        assertNull(drafts.current)
    }

    @Test
    fun theChosenLoadIsStampedOnEverySetAndKeptForNextTime() {
        val controller = controller()
        controller.setLoad(SetLoad(assistanceKg = 20.0))
        controller.start()
        controller.logSet(5)
        controller.finishEarly()

        val finished = controller.state.value as WorkoutState.Finished
        assertTrue(finished.usedBand)
        // A band-assisted set doesn't count toward the gate.
        assertEquals(0, finished.sessionBest)
        assertEquals(20.0, sessions.sessions.single().sets.single().assistanceKg, 0.0)

        controller.dismissSummary()
        assertEquals(WorkoutState.Idle(SetLoad(assistanceKg = 20.0)), controller.state.value)
    }

    @Test
    fun holdsIgnoreTheChosenLoad() {
        val newProgress = FakeProgressRepository()
        val controller = WorkoutController(
            FakeGoalRepository(path),
            newProgress,
            drafts,
            FakeSessionRepository(newProgress),
            clock,
            scope,
        )
        controller.setLoad(SetLoad(addedKg = 10.0))
        controller.start()

        assertEquals(deadHang, controller.run().plan.gate)
        assertEquals(SetLoad.None, controller.run().load)
    }

    @Test
    fun theLoadCantChangeMidSession() {
        val controller = controller()
        controller.start()
        controller.setLoad(SetLoad(addedKg = 10.0))
        assertEquals(SetLoad.None, controller.run().load)
    }

    @Test
    fun aSessionInProgressSurvivesTheAppBeingKilled() {
        val first = controller()
        first.start()
        first.logSet(4)
        first.endRest()
        val before = first.run()

        // A new controller is what a restarted process gets.
        val second = controller()

        assertEquals(before, second.run())
    }

    @Test
    fun aSessionKilledJustBeforeSavingIsSavedOnceOnRestart() {
        val first = controller()
        first.start()
        val plan = first.run().plan
        // Every set logged, but the process died before the session was saved.
        var run = first.run()
        repeat(plan.sets.size) { run = run.endRest().logSet(5, null, clock.elapsed) }
        val interrupted = FakeSessionDraftRepository(run)

        val second = controller(draftRepository = interrupted)
        controller(draftRepository = FakeSessionDraftRepository(run))

        assertTrue(second.state.value is WorkoutState.Finished)
        // Restoring the same draft twice still stores one session.
        assertEquals(listOf(run.id), sessions.stored.map { it.id })
        assertNull(interrupted.current)
    }

    @Test
    fun aRestLeftOverFromBeforeARebootIsEnded() {
        val first = controller()
        first.start()
        first.logSet(4)
        // The elapsed clock starts again after a reboot, so the saved end time is far in the future.
        clock.elapsed = 0

        val second = controller()

        assertNull(second.run().restEndsAtElapsedMillis)
    }

    @Test
    fun lockedDataBecomesAStateNotACrash() {
        val locked = object : SessionDraftRepository {
            override suspend fun load(): SessionRun? = throw DataUnavailableException("locked")

            override suspend fun save(run: SessionRun) = Unit

            override suspend fun clear() = Unit
        }
        assertEquals(WorkoutState.Unavailable, controller(draftRepository = locked).state.value)
    }
}
