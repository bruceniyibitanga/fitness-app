package app.levitx.phone.workout

import androidx.lifecycle.viewModelScope
import app.levitx.core.coach.Effort
import app.levitx.core.coach.ExerciseLookup
import app.levitx.core.coach.ExercisePlan
import app.levitx.core.coach.Intensity
import app.levitx.core.coach.PlannedSession
import app.levitx.core.coach.SessionResult
import app.levitx.core.coach.SkillTraining
import app.levitx.core.coach.WorkoutRun
import app.levitx.core.session.PlannedSet
import app.levitx.core.skills.SkillCatalogue
import app.levitx.data.coach.CoachRepository
import app.levitx.data.coach.Recorded
import app.levitx.data.coach.WorkoutDraftRepository
import app.levitx.data.settings.AppSettings
import app.levitx.phone.MainDispatcherRule
import app.levitx.phone.live.LiveWorkout
import app.levitx.testing.FakeCoachRepository
import app.levitx.testing.FakeEquipmentRepository
import app.levitx.testing.FakeSettingsRepository
import app.levitx.testing.FakeTrainingClock
import app.levitx.testing.FakeWorkoutDraftRepository
import app.levitx.testing.coachState
import app.levitx.testing.startedWorkout
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WorkoutViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val clock = FakeTrainingClock()
    private val drafts = FakeWorkoutDraftRepository(startedWorkout(nowEpochMillis = clock.now))
    private val coachRepository = FakeCoachRepository(coachState(), drafts)
    private val live = LiveWorkout()
    private val viewModel by lazy {
        WorkoutViewModel(coachRepository, drafts, FakeEquipmentRepository(), clock, FakeSettingsRepository(), live)
    }

    private val active: WorkoutUiState.Active
        get() = viewModel.uiState.value as WorkoutUiState.Active

    @Test
    fun withNoWorkoutInProgressThereIsNothingToShow() {
        val empty = FakeWorkoutDraftRepository()
        val viewModel =
            WorkoutViewModel(coachRepository, empty, FakeEquipmentRepository(), clock, FakeSettingsRepository(), live)
        assertEquals(WorkoutUiState.NoWorkout, viewModel.uiState.value)
        assertNull(live.session.value)
    }

    @Test
    fun theLiveSessionFollowsTheWorkoutAndEndsWithIt() {
        assertTrue(viewModel.uiState.value is WorkoutUiState.Active)
        val first = checkNotNull(live.session.value)
        assertEquals(1, first.setNumber)
        assertEquals(0, first.setsBehind)
        assertNull(first.restEndsAtElapsedMillis)

        viewModel.logSet(active.nextSet.target, Effort.RIGHT.repsInReserve)
        val resting = checkNotNull(live.session.value)
        assertEquals(active.run.restEndsAtElapsedMillis, resting.restEndsAtElapsedMillis)
        assertEquals(1, resting.setsBehind)
        assertEquals(active.run.nextSetIndex + 1, resting.setNumber)

        viewModel.finish()
        assertNull(live.session.value)
    }

    @Test
    fun aLoggedSetIsSavedAndStartsRest() {
        viewModel.logSet(active.nextSet.target, Effort.RIGHT.repsInReserve)
        val saved = checkNotNull(drafts.current)
        assertEquals(1, saved.logged.sumOf { it.size })
        assertTrue(active.run.isResting)
        assertEquals(active.run, saved)
    }

    @Test
    fun noSetCanBeLoggedDuringRest() {
        viewModel.logSet(active.nextSet.target, Effort.RIGHT.repsInReserve)
        viewModel.logSet(active.nextSet.target, Effort.RIGHT.repsInReserve)
        assertEquals(1, active.run.logged.sumOf { it.size })
        viewModel.rest(RestAction.END)
        assertNull(active.run.restEndsAtElapsedMillis)
    }

    @Test
    fun finishingHandsTheWorkoutToTheCoachAndClearsTheDraft() {
        viewModel.logSet(active.nextSet.target, Effort.RIGHT.repsInReserve)
        viewModel.finish()
        assertEquals(WorkoutUiState.Finished::class, viewModel.uiState.value::class)
        assertEquals(1, (viewModel.uiState.value as WorkoutUiState.Finished).report.sets)
        assertEquals(setOf("workout-1"), coachRepository.recordedWorkouts)
        assertNull(drafts.current)
    }

    @Test
    fun finishingWithNothingDoneThrowsTheWorkoutAway() {
        viewModel.finish()
        assertEquals(WorkoutUiState.NoWorkout, viewModel.uiState.value)
        assertTrue(coachRepository.recordedWorkouts.isEmpty())
        assertNull(drafts.current)
    }

    @Test
    fun skippingEveryExerciseEndsTheWorkoutWithNothingSaved() {
        repeat(active.run.session.plans.size) { viewModel.skipExercise() }
        assertEquals(WorkoutUiState.NoWorkout, viewModel.uiState.value)
        assertTrue(coachRepository.recordedWorkouts.isEmpty())
    }

    @Test
    fun impossibleLoadsAreClampedBeforeTheyReachTheRun() {
        viewModel.logSet(active.nextSet.target, Effort.RIGHT.repsInReserve, addedKg = Double.NaN, assistanceKg = -5.0)
        val set = checkNotNull(drafts.current).logged.flatten().single()
        assertEquals(0.0, set.addedKg, 0.0)
        assertEquals(0.0, set.assistanceKg, 0.0)
    }

    @Test
    fun restCanBeMadeLongerOrShorter() {
        viewModel.logSet(active.nextSet.target, Effort.RIGHT.repsInReserve)
        val endsAt = checkNotNull(active.run.restEndsAtElapsedMillis)
        viewModel.rest(RestAction.LONGER)
        assertEquals(endsAt + 15_000, active.run.restEndsAtElapsedMillis)
        viewModel.rest(RestAction.SHORTER)
        viewModel.rest(RestAction.SHORTER)
        assertEquals(endsAt - 15_000, checkNotNull(drafts.current).restEndsAtElapsedMillis)
    }

    @Test
    fun undoTakesBackTheSetJustLoggedAndSavesThat() {
        viewModel.logSet(active.nextSet.target, Effort.RIGHT.repsInReserve)
        assertTrue(active.canUndo)
        viewModel.rest(RestAction.UNDO)
        assertEquals(0, active.run.logged.sumOf { it.size })
        assertNull(active.run.restEndsAtElapsedMillis)
        assertEquals(0, checkNotNull(drafts.current).logged.sumOf { it.size })
    }

    @Test
    fun aCountedSetHasNoTimer() {
        val counted = timedWorkout(firstId = "squat.bodyweight")
        counted.timer(TimerAction.START)
        assertNull((counted.uiState.value as WorkoutUiState.Active).timer)
    }

    @Test
    fun aStoppedHoldFillsInTheSecondsHeld() {
        val holds = timedWorkout(firstId = "core.plank")
        holds.timer(TimerAction.START)
        val timer = checkNotNull((holds.uiState.value as WorkoutUiState.Active).timer)
        assertEquals(TimerKind.HOLD, timer.kind)
        // Through the count-in, then 23.5 seconds of holding.
        clock.elapsed += DEFAULT_LEAD_IN_SECONDS * 1_000L + 23_500
        holds.timer(TimerAction.STOP)
        val stopped = holds.uiState.value as WorkoutUiState.Active
        assertNull(stopped.timer)
        assertEquals(23, stopped.counted)
    }

    @Test
    fun negativesArePacedAtTheExercisesLoweringTime() {
        val negatives = timedWorkout(firstId = "vertical_pull.negative")
        negatives.timer(TimerAction.START)
        negatives.timer(TimerAction.NOW)
        val timer = checkNotNull((negatives.uiState.value as WorkoutUiState.Active).timer)
        assertEquals(TimerKind.NEGATIVE, timer.kind)
        assertEquals(5_000L, timer.lowerMillis)
        assertEquals(3, timer.reps)
        assertEquals(clock.elapsed, timer.startsAtElapsedMillis)
    }

    @Test
    fun loggingASetEndsTheTimerAndItsCount() {
        val holds = timedWorkout(firstId = "core.plank")
        holds.timer(TimerAction.START)
        clock.elapsed += DEFAULT_LEAD_IN_SECONDS * 1_000L + 20_000
        holds.timer(TimerAction.STOP)
        holds.logSet(20, repsInReserve = null)
        val resting = holds.uiState.value as WorkoutUiState.Active
        assertNull(resting.counted)
        assertNull(resting.timer)
        assertNotNull(resting.lastSet)
    }

    @Test
    fun theChosenLoadIsKeptInTheRun() {
        viewModel.chooseLoad(addedKg = 5.0, assistanceKg = 0.0)
        assertEquals(5.0, checkNotNull(drafts.current).load.addedKg, 0.0)
    }

    /** A view model for a workout that starts with [firstId], three sets of three. */
    private fun timedWorkout(firstId: String): WorkoutViewModel {
        val plan = ExercisePlan(
            exercise = checkNotNull(ExerciseLookup.byId(firstId)),
            sets = List(3) { PlannedSet(3, false) },
            restSeconds = 90,
            repsInReserve = 2,
            intensity = Intensity.HARD,
        )
        val drafts = FakeWorkoutDraftRepository(WorkoutRun("timed", PlannedSession(listOf(plan)), clock.now))
        return WorkoutViewModel(
            FakeCoachRepository(coachState(), drafts),
            drafts,
            FakeEquipmentRepository(),
            clock,
            FakeSettingsRepository(),
            LiveWorkout(),
        )
    }

    @Test
    fun aSetThatWouldPassAGateWaitsForTheStandardFirst() {
        val gates = gateWorkout()
        gates.logSet(10, repsInReserve = 0)
        val waiting = gates.uiState.value as WorkoutUiState.Active
        assertNotNull(waiting.pending)
        assertEquals(0, waiting.run.logged.sumOf { it.size })

        gates.gate(GateAction.CONFIRM)
        val passed = gates.uiState.value as WorkoutUiState.Active
        assertNull(passed.pending)
        assertEquals(1, passed.run.logged.sumOf { it.size })
        assertEquals("first_pull_up.scapular_pulls", passed.passed?.gate?.id)

        gates.gate(GateAction.CARRY_ON)
        assertNull((gates.uiState.value as WorkoutUiState.Active).passed)
    }

    @Test
    fun aSetThatDidntMeetTheStandardGoesBackToBeCorrected() {
        val gates = gateWorkout()
        gates.logSet(10, repsInReserve = 0)
        gates.gate(GateAction.RECOUNT)
        val back = gates.uiState.value as WorkoutUiState.Active
        assertNull(back.pending)
        assertEquals(0, back.run.logged.sumOf { it.size })
    }

    @Test
    fun aBandAssistedOrShortSetIsLoggedWithoutAsking() {
        val gates = gateWorkout()
        gates.logSet(10, repsInReserve = 0, assistanceKg = 10.0)
        assertNull((gates.uiState.value as WorkoutUiState.Active).pending)
        gates.rest(RestAction.END)
        gates.logSet(8, repsInReserve = 0)
        val after = gates.uiState.value as WorkoutUiState.Active
        assertNull(after.pending)
        assertNull(after.passed)
        assertEquals(2, after.run.logged.sumOf { it.size })
    }

    @Test
    fun anAttemptCanBePutOffAndThatIsSaved() {
        val gates = gateWorkout()
        gates.gate(GateAction.DEFER)
        val deferred = gates.uiState.value as WorkoutUiState.Active
        assertTrue(!deferred.plan.isGateAttempt)
        assertTrue(deferred.plan.sets.last().isBestEffort)
    }

    /** A view model for a workout that starts with an attempt at the 10 scapular pull-ups gate. */
    private fun gateWorkout(): WorkoutViewModel {
        val gate = checkNotNull(SkillCatalogue.gateById("first_pull_up.scapular_pulls"))
        val plan = ExercisePlan(
            exercise = checkNotNull(SkillTraining.exerciseFor(gate)),
            sets = listOf(PlannedSet(10, true)) + List(3) { PlannedSet(6, false) },
            restSeconds = 120,
            repsInReserve = 2,
            intensity = Intensity.HARD,
            gateId = gate.id,
            isGateAttempt = true,
        )
        val drafts = FakeWorkoutDraftRepository(WorkoutRun("gate", PlannedSession(listOf(plan)), clock.now))
        return WorkoutViewModel(
            FakeCoachRepository(coachState(), drafts),
            drafts,
            FakeEquipmentRepository(),
            clock,
            FakeSettingsRepository(),
            LiveWorkout(),
        )
    }

    @Test
    fun theTimerCountsInForTheChosenLengthAndSoundsStartAsChosen() {
        val drafts = FakeWorkoutDraftRepository(startedWorkout(nowEpochMillis = clock.now))
        val settings = FakeSettingsRepository(AppSettings(leadInSeconds = 10, sounds = false, buzzes = true))
        val chosen =
            WorkoutViewModel(
                FakeCoachRepository(coachState(), drafts),
                drafts,
                FakeEquipmentRepository(),
                clock,
                settings,
                LiveWorkout(),
            )
        val active = chosen.uiState.value as WorkoutUiState.Active
        assertEquals(false, active.soundsOn)
        assertEquals(10, active.leadInSeconds)
    }

    @Test
    fun aSetLoggedJustBeforeLeavingTheScreenIsStillSaved() {
        val slow = SlowDrafts(drafts)
        val leaving =
            WorkoutViewModel(coachRepository, slow, FakeEquipmentRepository(), clock, FakeSettingsRepository(), live)
        val target = (leaving.uiState.value as WorkoutUiState.Active).nextSet.target

        leaving.logSet(target, Effort.RIGHT.repsInReserve)
        // The screen closes while the write is still on its way to the database.
        leaving.viewModelScope.cancel()
        slow.letThrough()

        assertEquals(1, checkNotNull(drafts.current).logged.sumOf { it.size })
    }

    @Test
    fun aWorkoutFinishedJustBeforeLeavingTheScreenIsStillRecorded() {
        val gate = CompletableDeferred<Unit>()
        val slowCoach = object : CoachRepository by coachRepository {
            override suspend fun recordWorkout(workoutId: String, result: SessionResult): Recorded {
                gate.await()
                return coachRepository.recordWorkout(workoutId, result)
            }
        }
        val leaving =
            WorkoutViewModel(slowCoach, drafts, FakeEquipmentRepository(), clock, FakeSettingsRepository(), live)
        val target = (leaving.uiState.value as WorkoutUiState.Active).nextSet.target
        leaving.logSet(target, Effort.RIGHT.repsInReserve)

        leaving.finish()
        leaving.viewModelScope.cancel()
        gate.complete(Unit)

        assertEquals(1, coachRepository.recordedWorkouts.size)
        assertNull(drafts.current)
    }
}

/** Drafts whose saves wait until [letThrough], to stand in for a write that is slow to reach the database. */
private class SlowDrafts(private val drafts: FakeWorkoutDraftRepository) : WorkoutDraftRepository by drafts {
    private val gate = CompletableDeferred<Unit>()

    fun letThrough() {
        gate.complete(Unit)
    }

    override suspend fun save(run: WorkoutRun) {
        gate.await()
        drafts.save(run)
    }
}
