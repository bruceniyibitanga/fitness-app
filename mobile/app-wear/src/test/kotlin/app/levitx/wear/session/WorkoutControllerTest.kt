package app.levitx.wear.session

import app.levitx.core.coach.WorkoutRun
import app.levitx.core.session.LoggedSet
import app.levitx.core.session.PlannedSet
import app.levitx.core.session.SessionPlan
import app.levitx.core.session.SessionRun
import app.levitx.core.session.SetLoad
import app.levitx.core.skills.GateMeasure
import app.levitx.core.skills.SkillCatalogue
import app.levitx.data.DataUnavailableException
import app.levitx.data.coach.WorkoutDraftRepository
import app.levitx.testing.FakeWorkoutDraftRepository
import app.levitx.wear.WatchFakes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutControllerTest {
    private val dips = SkillCatalogue.muscleUp.gates[1]
    private val fakes = WatchFakes()
    private val clock = fakes.clock

    private fun WorkoutController.run(): WorkoutRun = (state.value as WorkoutState.Running).run

    /** Logs every set of the current exercise at its target, ending each rest. */
    private fun WorkoutController.finishExercise(value: Int? = null) {
        val plan = checkNotNull(run().current)
        repeat(plan.sets.size - run().nextSetIndex) {
            logSet(value ?: checkNotNull(run().nextSet).target)
            (state.value as? WorkoutState.Running)?.let { endRest() }
        }
    }

    @Test
    fun startsIdleWhenNothingWasInProgress() {
        assertEquals(WorkoutState.Idle(), fakes.controller().state.value)
    }

    @Test
    fun startingRunsTheCoachsSessionAndSavesADraft() {
        val controller = fakes.controller()
        controller.start()

        assertEquals(fakes.next.current, controller.run().session)
        assertEquals(clock.now, controller.run().startedAtEpochMillis)
        assertEquals(controller.run(), fakes.drafts.current)
    }

    @Test
    fun withNoSessionFromThePhoneItStaysIdle() {
        val controller = WatchFakes(session = null).controller()
        controller.start()
        assertEquals(WorkoutState.Idle(), controller.state.value)
    }

    @Test
    fun startingAgainCarriesOnWithTheSameWorkout() {
        val controller = fakes.controller()
        controller.start()
        controller.logSet(4)
        val before = controller.run()

        controller.start()

        assertEquals(before, controller.run())
    }

    @Test
    fun loggingASetStartsTheExercisesRestAndSavesTheDraft() {
        val controller = fakes.controller()
        controller.start()
        val plan = checkNotNull(controller.run().current)
        controller.logSet(4)

        val run = controller.run()
        assertEquals(clock.elapsed + plan.restSeconds * 1_000L, run.restEndsAtElapsedMillis)
        assertNull(run.logged.first().single().repsInReserve)
        assertEquals(run, fakes.drafts.current)
    }

    @Test
    fun aSetCantBeLoggedDuringRest() {
        val controller = fakes.controller()
        controller.start()
        controller.logSet(4)
        val resting = controller.run()

        controller.logSet(4)

        assertEquals(resting, controller.run())
    }

    @Test
    fun theWorkoutMovesThroughEveryExerciseThenIsQueuedForThePhone() {
        val controller = fakes.controller()
        controller.start()
        val id = controller.run().id
        controller.finishExercise(value = 8)
        assertEquals(1, controller.run().exerciseIndex)
        controller.finishExercise()

        val finished = controller.state.value as WorkoutState.Finished
        assertEquals(5, finished.sets)
        assertEquals(GateBest(dips.title, GateMeasure.REPS, 8, passedGate = false), finished.newBest)
        assertEquals(listOf(id), fakes.outbox.current.map { it.run.id })
        assertEquals(8, fakes.progress.current[dips.id])
        assertNull(fakes.drafts.current)
        // The plan is used up until the phone has the workout and plans the next one.
        assertEquals(id, fakes.next.awaitingId)
        assertNull(fakes.next.current)
    }

    @Test
    fun noNewBestWhenTheSkillWorkDidntBeatIt() {
        val controller = fakes.controller()
        controller.start()
        controller.finishExercise(value = 5)
        controller.finishEarly()

        assertNull((controller.state.value as WorkoutState.Finished).newBest)
    }

    @Test
    fun skippingMovesOnAndTheCoachHearsAboutIt() {
        val controller = fakes.controller()
        controller.start()
        controller.skipExercise()
        assertEquals(1, controller.run().exerciseIndex)

        controller.finishExercise()

        assertEquals(setOf(0), fakes.outbox.current.single().run.skipped)
    }

    @Test
    fun endingEarlySavesWhatWasDone() {
        val controller = fakes.controller()
        controller.start()
        controller.logSet(5)
        controller.finishEarly()

        assertEquals(1, (controller.state.value as WorkoutState.Finished).sets)
        assertEquals(1, fakes.outbox.current.size)
    }

    @Test
    fun endingBeforeAnySetSavesNothingAndKeepsThePlan() {
        val controller = fakes.controller()
        controller.start()
        controller.finishEarly()

        assertEquals(WorkoutState.Idle(), controller.state.value)
        assertTrue(fakes.outbox.current.isEmpty())
        assertNull(fakes.drafts.current)
        assertTrue(fakes.next.current != null)
    }

    @Test
    fun theChosenLoadGoesOnTheSkillWorkOnlyAndIsKeptForNextTime() {
        val controller = fakes.controller()
        controller.setLoad(SetLoad(assistanceKg = 20.0))
        controller.start()
        controller.finishExercise(value = 8)
        controller.logSet(12)
        controller.finishEarly()

        val finished = controller.state.value as WorkoutState.Finished
        assertTrue(finished.usedBand)
        // Band-assisted sets don't count toward the gate.
        assertNull(finished.newBest)
        val logged = fakes.outbox.current.single().run.logged
        assertTrue(logged[0].all { it.assistanceKg == 20.0 })
        assertEquals(0.0, logged[1].single().assistanceKg, 0.0)

        controller.dismissSummary()
        assertEquals(WorkoutState.Idle(SetLoad(assistanceKg = 20.0)), controller.state.value)
    }

    @Test
    fun theLoadCantChangeMidWorkout() {
        val controller = fakes.controller()
        controller.start()
        controller.setLoad(SetLoad(addedKg = 10.0))
        controller.logSet(4)

        assertEquals(0.0, controller.run().logged[0].single().addedKg, 0.0)
    }

    @Test
    fun aWorkoutInProgressSurvivesTheAppBeingKilled() {
        val first = fakes.controller()
        first.start()
        first.logSet(4)
        first.endRest()
        val before = first.run()

        // A new controller is what a restarted process gets.
        val second = fakes.controller()

        assertEquals(before, second.run())
    }

    @Test
    fun aWorkoutKilledJustBeforeSavingIsSavedOnceOnRestart() {
        val first = fakes.controller()
        first.start()
        // Every set logged, but the process died before the workout was saved.
        var run = first.run()
        while (!run.isComplete) run = run.endRest().logSet(5, null, clock.elapsed)

        val second = fakes.controller(draftRepository = FakeWorkoutDraftRepository(run))
        fakes.controller(draftRepository = FakeWorkoutDraftRepository(run))

        assertTrue(second.state.value is WorkoutState.Finished)
        // Restoring the same draft twice still queues one workout.
        assertEquals(listOf(run.id), fakes.outbox.current.map { it.run.id })
    }

    @Test
    fun aRestLeftOverFromBeforeARebootIsEnded() {
        val first = fakes.controller()
        first.start()
        first.logSet(4)
        // The elapsed clock starts again after a reboot, so the saved end time is far in the future.
        clock.elapsed = 0

        val second = fakes.controller()

        assertNull(second.run().restEndsAtElapsedMillis)
    }

    @Test
    fun aGateSessionLeftByAnOlderVersionIsSavedTheOldWay() {
        val plan = SessionPlan(dips, listOf(PlannedSet(4, false), PlannedSet(4, true)), 90, repsInReserve = 2)
        val leftover = SessionRun("00000000-0000-4000-8000-000000000001", plan, 6, startedAtEpochMillis = 1)
            .logSet(5, null, nowElapsedMillis = 0)
        val fakes = WatchFakes(legacyDraft = leftover)

        fakes.controller()

        assertEquals(listOf(leftover.id), fakes.legacySessions.queued.map { it.id })
        assertNull(fakes.legacyDrafts.current)
        assertEquals(listOf(LoggedSet(5, null, false)), fakes.legacySessions.sessions.single().sets)
    }

    @Test
    fun lockedDataBecomesAStateNotACrash() {
        val locked = object : WorkoutDraftRepository {
            override fun observeDraft(): Flow<WorkoutRun?> = flow { throw DataUnavailableException("locked") }

            override suspend fun save(run: WorkoutRun) = Unit

            override suspend fun discard() = Unit
        }
        val controller = fakes.controller(draftRepository = locked)
        assertEquals(WorkoutState.Unavailable, controller.state.value)
        assertFalse(controller.state.value is WorkoutState.Idle)
    }
}
