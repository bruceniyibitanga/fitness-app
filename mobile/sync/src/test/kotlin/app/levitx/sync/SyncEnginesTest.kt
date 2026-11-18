package app.levitx.sync

import app.levitx.core.coach.Coach
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.WorkoutRun
import app.levitx.core.intervals.IntervalCatalogue
import app.levitx.core.load.WeightUnit
import app.levitx.core.session.CompletedSession
import app.levitx.core.session.LoggedSet
import app.levitx.core.skills.SkillCatalogue
import app.levitx.core.skills.SkillPath
import app.levitx.data.DataUnavailableException
import app.levitx.data.coach.CoachRepository
import app.levitx.data.equipment.Band
import app.levitx.data.goal.GoalRepository
import app.levitx.data.intervals.QueuedIntervalWorkout
import app.levitx.data.session.StoredSession
import app.levitx.data.settings.AppSettings
import app.levitx.testing.FakeCoachRepository
import app.levitx.testing.FakeEquipmentRepository
import app.levitx.testing.FakeGoalRepository
import app.levitx.testing.FakeIntervalOutbox
import app.levitx.testing.FakeNextSessionRepository
import app.levitx.testing.FakePrivacyRepository
import app.levitx.testing.FakeProgressRepository
import app.levitx.testing.FakeResetMarker
import app.levitx.testing.FakeSessionRepository
import app.levitx.testing.FakeSettingsRepository
import app.levitx.testing.FakeTrainingClock
import app.levitx.testing.FakeWorkoutOutbox
import app.levitx.testing.coachState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Runs a pretend phone and a pretend watch against one in-memory Data Layer. */
class SyncEnginesTest {
    private val path = SkillCatalogue.muscleUp
    private val dips = path.gates[1]
    private val network = FakeNetwork()

    private val phoneGoal = FakeGoalRepository(path)
    private val phoneProgress = FakeProgressRepository(mapOf(path.gates[0].id to 30, dips.id to 6))
    private val phoneEquipment = FakeEquipmentRepository(bodyweightKg = 74.0, bands = listOf(Band("b1", "Red", 20.0)))
    private val phoneSessions = FakeSessionRepository(phoneProgress)
    private val phoneCoach = FakeCoachRepository(coachState())
    private val clock = FakeTrainingClock()
    private val phoneSettings = FakeSettingsRepository(AppSettings(units = WeightUnit.LB))
    private val phoneReset = FakeResetMarker()
    private val phone = phoneEngine()
    private val inbox = phoneInbox()

    private val watchGoal = FakeGoalRepository()
    private val watchProgress = FakeProgressRepository()
    private val watchEquipment = FakeEquipmentRepository()
    private val watchSessions = FakeSessionRepository(watchProgress)
    private val watchNext = FakeNextSessionRepository()
    private val watchSettings = FakeSettingsRepository(AppSettings(units = WeightUnit.KG))
    private val watchWorkouts = FakeWorkoutOutbox(watchProgress, next = watchNext)
    private val watchIntervals = FakeIntervalOutbox()

    // Deleting everything on the watch empties its fakes, as the real database is emptied.
    private val watchPrivacy = FakePrivacyRepository {
        watchGoal.clearActiveGoal()
        watchNext.replace(null)
        watchProgress.clear()
        watchEquipment.replaceBands(emptyList())
        watchWorkouts.current.forEach { watchWorkouts.markDelivered(it.run.id) }
        watchIntervals.current.forEach { watchIntervals.markDelivered(it.id) }
    }
    private val watchReset = FakeResetMarker()
    private val watch = watchEngine()

    private fun phoneEngine(
        goal: GoalRepository = phoneGoal,
        coach: CoachRepository = phoneCoach,
        marker: FakeResetMarker = phoneReset,
    ) = PhoneSyncEngine(
        network.onDevice("phone"),
        PhoneStateSource(goal, phoneProgress, phoneEquipment, coach, Coach(), clock),
        phoneSettings,
        marker,
    )

    /** Does what the app does from its start for as long as it runs: announces the last delete, then keeps publishing. */
    private fun CoroutineScope.running(engine: PhoneSyncEngine) = launch {
        engine.announceLastReset()
        engine.publishing().collect {}
    }

    private fun phoneInbox(coach: CoachRepository = phoneCoach) =
        PhoneInbox(network.onDevice("phone"), phoneEquipment, phoneSessions, coach)

    private fun watchEngine(goal: GoalRepository = watchGoal) = WatchSyncEngine(
        network.onDevice("watch"),
        PhoneStateApplier(goal, watchProgress, watchEquipment, watchNext, watchSettings),
        watchSessions,
        watchWorkouts,
        watchIntervals,
        WatchWipe(watchPrivacy, watchReset),
    )

    /** What the phone's coach would plan right now. */
    private fun coachsPlan() = Coach().recommend(checkNotNull(phoneCoach.current).model, clock.now).session

    /** Runs the watch's next session, logs one set of it, and finishes. */
    private suspend fun finishOnTheWatch(id: String) {
        val run = WorkoutRun(id, checkNotNull(watchNext.current), startedAtEpochMillis = clock.now)
        val planned = checkNotNull(run.nextSet)
        watchWorkouts.finishAndQueue(run.logSet(planned.target, 2, clock.elapsed), clock.now + 60_000)
    }

    private fun session(id: String, best: Int) = StoredSession(
        id = id,
        session = CompletedSession(dips.id, 1_000, 2_000, listOf(LoggedSet(4, 2, false), LoggedSet(best, 0, true))),
    )

    /** Two rounds of burpees on the watch, as the interval timer saves them. */
    private fun intervals(id: String) = QueuedIntervalWorkout(
        id = id,
        sessions = listOf(
            CompletedSession(
                gateId = checkNotNull(IntervalCatalogue.moveById("cardio.burpees")).id,
                startedAtEpochMillis = 1_000,
                finishedAtEpochMillis = 61_000,
                sets = listOf(LoggedSet(20, null, false), LoggedSet(18, null, false)),
            ),
        ),
    )

    private val idA = "00000000-0000-4000-8000-00000000000a"
    private val idB = "00000000-0000-4000-8000-00000000000b"

    private suspend fun publish() = phone.publish(checkNotNull(phone.watchState().first()))

    @Test
    fun theWatchCopiesThePhonesGoalBestsBandsAndNextSession() = runTest {
        publish()
        val report = watch.sync()

        assertTrue(report.stateChanged)
        assertEquals(path, watchGoal.current)
        assertEquals(phoneProgress.current, watchProgress.current)
        assertEquals(phoneEquipment.bands, watchEquipment.bands)
        assertEquals(coachsPlan(), watchNext.current)
        assertTrue(watchNext.current != null)
        assertEquals(WeightUnit.LB, watchSettings.settings.value.units)
        // A second pass finds nothing to do.
        assertEquals(SyncReport(SyncOutcome.DONE), watch.sync())
    }

    @Test
    fun changingTheUnitOnThePhoneReachesTheWatch() = runTest {
        publish()
        watch.sync()
        phoneSettings.setUnits(WeightUnit.KG)
        publish()

        assertTrue(watch.sync().stateChanged)
        assertEquals(WeightUnit.KG, watchSettings.settings.value.units)
    }

    @Test
    fun deletingEverythingOnThePhoneDeletesTheWatchsCopyOnce() = runTest {
        publish()
        watch.sync()
        finishOnTheWatch(idA)
        watch.sync()

        phone.forget()
        assertTrue(watch.sync().stateChanged)

        assertEquals(1, watchPrivacy.deletions)
        assertNull(watchGoal.current)
        assertNull(watchNext.current)
        assertTrue(watchWorkouts.current.isEmpty())
        // Only the phone's request and the id of the delete are left: no plan, and no workout from the watch.
        assertEquals(listOf(SyncPaths.FORGET, SyncPaths.RESET), network.paths)
        // Deleting again would find nothing, so it doesn't happen.
        assertEquals(SyncReport(SyncOutcome.DONE), watch.sync())
        assertEquals(1, watchPrivacy.deletions)
    }

    @Test
    fun afterSettingUpAgainTheWatchKeepsTheNewPlan() = runTest {
        publish()
        watch.sync()
        phone.forget()
        watch.sync()

        publish()
        watch.sync()

        // The id of the delete stays for good; the watch has acted on it, so it deletes nothing more.
        assertEquals(listOf(SyncPaths.RESET, SyncPaths.STATE), network.paths)
        assertEquals(path, watchGoal.current)
        assertEquals(coachsPlan(), watchNext.current)
        assertEquals(1, watchPrivacy.deletions)
    }

    @Test
    fun bodyweightStaysOnThePhone() = runTest {
        publish()
        watch.sync()
        assertNull(watchEquipment.observeBodyweightKg().first())
    }

    @Test
    fun aBestSetOnTheWatchIsNeverLoweredByThePhone() = runTest {
        watchProgress.recordBest(dips.id, 9)
        publish()
        watch.sync()
        assertEquals(9, watchProgress.current[dips.id])
    }

    @Test
    fun aWorkoutTravelsToTheCoachAndTheWatchTakesTheNewPlan() = runTest {
        publish()
        watch.sync()
        finishOnTheWatch(idA)

        watch.sync()
        assertEquals(listOf(SyncPaths.STATE, SyncPaths.workout(idA)), network.paths)

        assertEquals(1, inbox.receive().delivered)
        assertEquals(setOf(idA), phoneCoach.recordedWorkouts)

        // The phone publishes a plan that takes the workout in, and the watch takes it.
        publish()
        watch.sync()
        assertNull(watchNext.awaitingWorkoutId())
        assertEquals(coachsPlan(), watchNext.current)
        assertTrue(watchWorkouts.current.isEmpty())

        // The phone sees the workout item is gone and deletes its confirmation.
        inbox.receive()
        assertEquals(listOf(SyncPaths.STATE), network.paths)
    }

    @Test
    fun aPlanMadeBeforeTheWorkoutArrivedIsNotTaken() = runTest {
        publish()
        watch.sync()
        finishOnTheWatch(idA)

        // The phone hasn't seen the workout, so its state still holds the old plan.
        phoneProgress.recordBest(dips.id, 9)
        publish()
        watch.sync()

        assertEquals(idA, watchNext.awaitingWorkoutId())
        assertNull(watchNext.current)
        // Everything else in the state is still taken.
        assertEquals(9, watchProgress.current[dips.id])
    }

    @Test
    fun workoutsWaitUntilTheCoachIsSetUp() = runTest {
        publish()
        watch.sync()
        finishOnTheWatch(idA)
        watch.sync()

        val noCoachPhone = phoneInbox(coach = FakeCoachRepository())

        assertEquals(0, noCoachPhone.receive().delivered)
        assertEquals(listOf(SyncPaths.STATE, SyncPaths.workout(idA)), network.paths)
    }

    @Test
    fun aWorkoutThatWaitedForSetupIsRecordedOnceAndOnlyOnce() = runTest {
        publish()
        watch.sync()
        finishOnTheWatch(idA)
        watch.sync()
        // Everything was deleted on the phone, then set up again.
        val lateCoach = FakeCoachRepository()
        val phoneAgain = phoneInbox(coach = lateCoach)
        assertEquals(0, phoneAgain.receive().delivered)
        assertTrue(network.paths.none { it.startsWith(SyncPaths.ACKS_PREFIX) })

        lateCoach.start(checkNotNull(phoneCoach.current).model.profile, PullUpAnswer.NOT_YET, emptySet())

        assertEquals(1, phoneAgain.receive().delivered)
        assertEquals(0, phoneAgain.receive().delivered)
        assertEquals(setOf(idA), lateCoach.recordedWorkouts)
        assertTrue(SyncPaths.ack(idA) in network.paths)
    }

    @Test
    fun unreadableWorkoutsAreSkippedAndNeverConfirmed() = runTest {
        publish()
        watch.sync()
        finishOnTheWatch(idA)
        val queued = watchWorkouts.current.single()
        network.plant("watch", SyncPaths.workout(idA), "not json".encodeToByteArray())
        // A valid payload on the wrong path: the id inside must match the path.
        network.plant("watch", SyncPaths.workout(idB), SyncCodec.encodeWorkout(queued))

        val report = inbox.receive()

        assertEquals(2, report.skipped)
        assertTrue(phoneCoach.recordedWorkouts.isEmpty())
        assertEquals(listOf(SyncPaths.STATE, SyncPaths.workout(idA), SyncPaths.workout(idB)), network.paths)
    }

    @Test
    fun theWatchDropsTheGoalWhenThePhoneHasNone() = runTest {
        watchGoal.setActiveGoal(path)

        phoneEngine(goal = FakeGoalRepository()).publishCurrent()
        watch.sync()

        assertNull(watchGoal.current)
        assertEquals(coachsPlan(), watchNext.current)
    }

    @Test
    fun aSessionFromAnOlderWatchTravelsToThePhoneAndEverythingIsTidiedUp() = runTest {
        watchSessions.saveAndQueue(session(idA, best = 8))

        watch.sync()
        assertEquals(listOf(SyncPaths.session(idA)), network.paths)

        val received = inbox.receive()
        assertEquals(1, received.delivered)
        assertEquals(8, phoneProgress.current[dips.id])
        // The phone adds its own bodyweight record to the session.
        assertEquals(74.0, phoneSessions.sessions.single().bodyweightKg)

        // The watch sees the confirmation, takes the session off its queue and deletes its item.
        assertEquals(1, watch.sync().delivered)
        assertTrue(watchSessions.queued.isEmpty())
        assertEquals(listOf(SyncPaths.ack(idA)), network.paths)

        // The phone sees the session item is gone and deletes its confirmation.
        inbox.receive()
        assertTrue(network.paths.isEmpty())
        // The session is still stored on both devices.
        assertEquals(1, watchSessions.stored.size)
        assertEquals(1, phoneSessions.stored.size)
    }

    @Test
    fun aBestRaisedOnThePhoneReachesTheWatch() = runTest {
        publish()
        watch.sync()
        phoneProgress.recordBest(dips.id, 9)

        assertTrue(phone.publishCurrent().stateChanged)
        watch.sync()

        assertEquals(9, watchProgress.current[dips.id])
    }

    @Test
    fun withNoGoalAndNoCoachThereIsNothingToPublish() = runTest {
        val noGoalPhone = phoneEngine(goal = FakeGoalRepository(), coach = FakeCoachRepository())

        assertEquals(SyncReport(SyncOutcome.DONE), noGoalPhone.publishCurrent())
        assertTrue(network.paths.isEmpty())
    }

    @Test
    fun retriesNeverDoubleCount() = runTest {
        watchSessions.saveAndQueue(session(idA, best = 8))
        repeat(3) { watch.sync() }
        repeat(3) { inbox.receive() }
        repeat(3) { watch.sync() }
        repeat(3) { inbox.receive() }

        assertEquals(1, phoneSessions.stored.size)
        assertTrue(network.paths.isEmpty())
    }

    @Test
    fun sessionsWaitWhileThePhoneIsOutOfReach() = runTest {
        watchSessions.saveAndQueue(session(idA, best = 7))
        watchSessions.saveAndQueue(session(idB, best = 8))

        // No phone pass in between: both stay queued however often the watch tries.
        repeat(2) { watch.sync() }
        assertEquals(2, watchSessions.queued.size)

        assertEquals(2, inbox.receive().delivered)
        assertEquals(2, watch.sync().delivered)
        assertTrue(watchSessions.queued.isEmpty())
    }

    @Test
    fun withoutTheDataLayerNothingIsLostAndNothingThrows() = runTest {
        watchSessions.saveAndQueue(session(idA, best = 8))
        network.isAvailable = false

        assertEquals(SyncOutcome.NO_LINK, watch.sync().outcome)
        assertEquals(SyncOutcome.NO_LINK, inbox.receive().outcome)
        assertEquals(SyncOutcome.NO_LINK, publish().outcome)
        assertEquals(1, watchSessions.queued.size)

        network.isAvailable = true
        watch.sync()
        assertEquals(1, inbox.receive().delivered)
    }

    @Test
    fun unreadableSessionsAreSkippedAndNeverConfirmed() = runTest {
        network.plant("watch", SyncPaths.session(idA), "not json".encodeToByteArray())
        // A valid payload on the wrong path: the id inside must match the path.
        network.plant("watch", SyncPaths.session(idB), SyncCodec.encodeSession(session(idA, best = 8)))

        val report = inbox.receive()

        assertEquals(2, report.skipped)
        assertTrue(phoneSessions.stored.isEmpty())
        assertEquals(listOf(SyncPaths.session(idA), SyncPaths.session(idB)), network.paths)
    }

    @Test
    fun unreadableStateIsSkippedAndChangesNothing() = runTest {
        network.plant("phone", SyncPaths.STATE, """{"v":1,"pathId":"no_such_path"}""".encodeToByteArray())

        val report = watch.sync()

        assertEquals(1, report.skipped)
        assertNull(watchGoal.current)
    }

    @Test
    fun oneWatchNeverDeletesAnotherWatchsSession() = runTest {
        network.plant("other-watch", SyncPaths.session(idB), SyncCodec.encodeSession(session(idB, best = 5)))

        watch.sync()

        assertEquals(listOf(SyncPaths.session(idB)), network.paths)
    }

    @Test
    fun aLeftoverSessionItemIsCleanedUp() = runTest {
        // Delivered and taken off the queue earlier, but the delete never went through.
        network.plant("watch", SyncPaths.session(idA), SyncCodec.encodeSession(session(idA, best = 8)))

        watch.sync()

        assertTrue(network.paths.isEmpty())
    }

    @Test
    fun lockedDataIsReportedNotThrown() = runTest {
        val lockedGoal = object : GoalRepository by watchGoal {
            override fun observeActiveGoal(): Flow<SkillPath?> = flow { throw DataUnavailableException("locked") }
        }
        val lockedWatch = watchEngine(goal = lockedGoal)
        publish()

        assertEquals(SyncOutcome.DATA_LOCKED, lockedWatch.sync().outcome)
    }

    @Test
    fun aCardioWorkoutGoesToThePhonesHistoryWithoutTheCoachAndLeavesTheWatch() = runTest {
        watchIntervals.queue(intervals(idA))
        watch.sync()
        assertEquals(listOf(SyncPaths.intervals(idA)), network.paths)

        // History doesn't need the coach, so it doesn't wait for one.
        val noCoachPhone = phoneInbox(coach = FakeCoachRepository())
        assertEquals(1, noCoachPhone.receive().delivered)
        assertEquals(listOf(listOf(20, 18)), phoneSessions.sessions.map { session -> session.sets.map { it.value } })
        assertTrue(phoneCoach.recordedWorkouts.isEmpty())
        assertTrue(phoneProgress.current.keys.none { it.startsWith("cardio.") })

        // The watch sees the confirmation and deletes its copy.
        assertEquals(1, watch.sync().delivered)
        assertTrue(watchIntervals.current.isEmpty())

        noCoachPhone.receive()
        assertTrue(network.paths.isEmpty())
    }

    @Test
    fun aCardioWorkoutSentTwiceIsSavedOnce() = runTest {
        watchIntervals.queue(intervals(idA))
        watch.sync()

        assertEquals(1, inbox.receive().delivered)
        assertEquals(0, inbox.receive().delivered)
        assertEquals(1, phoneSessions.sessions.size)
    }

    @Test
    fun unreadableCardioWorkoutsAreSkippedAndNeverConfirmed() = runTest {
        network.plant("watch", SyncPaths.intervals(idA), "not json".encodeToByteArray())
        // A valid payload on the wrong path: the id inside must match the path.
        network.plant("watch", SyncPaths.intervals(idB), SyncCodec.encodeIntervals(intervals(idA)))

        assertEquals(2, inbox.receive().skipped)
        assertTrue(phoneSessions.sessions.isEmpty())
        assertEquals(listOf(SyncPaths.intervals(idA), SyncPaths.intervals(idB)), network.paths)
    }

    @Test
    fun deletingEverythingOnThePhoneDeletesTheWatchsCardioWorkouts() = runTest {
        watchIntervals.queue(intervals(idA))
        phone.forget()

        watch.sync()

        assertTrue(watchIntervals.current.isEmpty())
        assertTrue(network.paths.none { it.startsWith(SyncPaths.INTERVALS_PREFIX) })
    }

    @Test
    fun aDeleteReachesAWatchThatWasAwayUntilThePhoneWasSetUpAgain() = runTest {
        publish()
        watch.sync()
        watchIntervals.queue(intervals(idA))
        watchProgress.recordBest(dips.id, 9)

        // The watch is out of reach for all of this: delete everything, then set up again.
        phone.forget()
        publish()
        assertTrue(SyncPaths.FORGET !in network.paths)

        assertTrue(watch.sync().stateChanged)
        assertEquals(1, watchPrivacy.deletions)
        assertTrue(watchIntervals.current.isEmpty())
        // What it holds now is only what the phone offers now.
        assertEquals(phoneProgress.current[dips.id], watchProgress.current[dips.id])
        assertEquals(coachsPlan(), watchNext.current)
        assertEquals(checkNotNull(phoneReset.id), watchReset.id)
    }

    @Test
    fun eachDeleteIsActedOnOnceAndHasItsOwnId() = runTest {
        phone.forget()
        val first = checkNotNull(phoneReset.id)
        publish()
        watch.sync()
        watch.sync()
        assertEquals(1, watchPrivacy.deletions)

        phone.forget()
        publish()
        watch.sync()
        watch.sync()
        assertEquals(2, watchPrivacy.deletions)
        assertTrue(first != phoneReset.id)
        assertEquals(phoneReset.id, watchReset.id)
    }

    @Test
    fun aResetItemThatIsntAnIdIsIgnored() = runTest {
        publish()
        watch.sync()
        network.plant("phone", SyncPaths.RESET, "delete please".encodeToByteArray())

        watch.sync()

        assertEquals(0, watchPrivacy.deletions)
        assertNull(watchReset.id)
        assertEquals(coachsPlan(), watchNext.current)
    }

    @Test
    fun aDeleteThatCouldntBeAnnouncedIsAnnouncedWhenTheAppNextStarts() = runTest {
        publish()
        watch.sync()

        // Play services is down at the moment of the delete. The id is saved all the same.
        network.isAvailable = false
        assertEquals(SyncOutcome.NO_LINK, phone.forget().outcome)
        network.isAvailable = true
        assertTrue(SyncPaths.RESET !in network.paths)
        watch.sync()
        assertEquals(0, watchPrivacy.deletions)

        // The next time the app starts, the saved id goes out before anything else.
        val scope = CoroutineScope(Dispatchers.Unconfined)
        scope.running(phoneEngine())
        watch.sync()

        assertEquals(1, watchPrivacy.deletions)
        assertEquals(checkNotNull(phoneReset.id), watchReset.id)
        scope.cancel()
    }

    @Test
    fun whenTheDatabaseEmptiesTheLastWordIsAlwaysTheDelete() = runTest {
        publish()
        watch.sync()
        val goal = FakeGoalRepository(path)
        val coach = FakeCoachRepository(coachState())
        val engine = phoneEngine(goal, coach)
        val scope = CoroutineScope(Dispatchers.Unconfined)
        scope.running(engine)

        // The delete is announced first, then the tables empty one after another. Each change
        // on the way is a state the publisher sees, some of them still holding the old plan.
        engine.forget()
        goal.clearActiveGoal()
        assertTrue(SyncPaths.STATE in network.paths)
        coach.deleteEverything()

        // Once nothing is left, the old plan is taken back and the delete stands.
        assertEquals(listOf(SyncPaths.FORGET, SyncPaths.RESET), network.paths)
        watch.sync()
        assertNull(watchNext.current)
        assertNull(watchGoal.current)
        scope.cancel()
    }

    @Test
    fun beforeSetupThereIsNothingToWithdrawAndNothingToDelete() = runTest {
        val scope = CoroutineScope(Dispatchers.Unconfined)

        scope.running(phoneEngine(FakeGoalRepository(), FakeCoachRepository(), FakeResetMarker()))

        // A phone that has never deleted anything never asks a watch to delete.
        assertTrue(network.paths.isEmpty())
        scope.cancel()
    }

    @Test
    fun afterAWorkoutTheWatchHoldsTheRealNextSessionForOnceItHasRested() = runTest {
        publish()
        watch.sync()
        // The whole session is done on the watch, then the two sync both ways.
        var run = WorkoutRun(idA, checkNotNull(watchNext.current), startedAtEpochMillis = clock.now)
        while (!run.isComplete) run = run.logSet(checkNotNull(run.nextSet).target, 2, clock.elapsed).endRest()
        clock.now += 45 * 60_000
        watchWorkouts.finishAndQueue(run, clock.now)
        watch.sync()
        inbox.receive()
        publish()
        watch.sync()

        val plan = checkNotNull(watchNext.plan)
        val model = checkNotNull(phoneCoach.current).model
        val restedAt = checkNotNull(plan.restedAtEpochMillis)

        // Started straight away, the watch gives what the phone gives now: light work on what was just trained.
        assertEquals(Coach().recommend(model, clock.now).session, plan.at(clock.now))
        assertTrue(plan.now.plans.any { !it.isHard })
        // Started two days later, with the phone never opened in between, it gives what the phone would give then.
        val twoDaysOn = clock.now + 2 * 24 * 60 * 60_000L
        assertTrue(twoDaysOn >= restedAt)
        assertEquals(Coach().recommend(model, restedAt).session, plan.at(twoDaysOn))
        assertTrue(plan.at(twoDaysOn).plans.all { it.isHard })
    }
}
