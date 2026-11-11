package app.levitx.sync

import app.levitx.core.coach.Coach
import app.levitx.core.coach.WorkoutRun
import app.levitx.core.session.CompletedSession
import app.levitx.core.session.LoggedSet
import app.levitx.core.skills.SkillCatalogue
import app.levitx.core.skills.SkillPath
import app.levitx.data.DataUnavailableException
import app.levitx.data.coach.CoachRepository
import app.levitx.data.equipment.Band
import app.levitx.data.goal.GoalRepository
import app.levitx.data.session.StoredSession
import app.levitx.testing.FakeCoachRepository
import app.levitx.testing.FakeEquipmentRepository
import app.levitx.testing.FakeGoalRepository
import app.levitx.testing.FakeNextSessionRepository
import app.levitx.testing.FakeProgressRepository
import app.levitx.testing.FakeSessionRepository
import app.levitx.testing.FakeTrainingClock
import app.levitx.testing.FakeWorkoutOutbox
import app.levitx.testing.coachState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
    private val phone = phoneEngine()

    private val watchGoal = FakeGoalRepository()
    private val watchProgress = FakeProgressRepository()
    private val watchEquipment = FakeEquipmentRepository()
    private val watchSessions = FakeSessionRepository(watchProgress)
    private val watchNext = FakeNextSessionRepository()
    private val watchWorkouts = FakeWorkoutOutbox(watchProgress, next = watchNext)
    private val watch = watchEngine()

    private fun phoneEngine(goal: GoalRepository = phoneGoal, coach: CoachRepository = phoneCoach) = PhoneSyncEngine(
        network.onDevice("phone"),
        PhoneStateSource(goal, phoneProgress, phoneEquipment, coach, Coach(), clock),
        phoneEquipment,
        phoneSessions,
        coach,
    )

    private fun watchEngine(goal: GoalRepository = watchGoal) = WatchSyncEngine(
        network.onDevice("watch"),
        PhoneStateApplier(goal, watchProgress, watchEquipment, watchNext),
        watchSessions,
        watchWorkouts,
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
        // A second pass finds nothing to do.
        assertEquals(SyncReport(SyncOutcome.DONE), watch.sync())
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

        assertEquals(1, phone.receive().delivered)
        assertEquals(setOf(idA), phoneCoach.recordedWorkouts)

        // The phone publishes a plan that takes the workout in, and the watch takes it.
        publish()
        watch.sync()
        assertNull(watchNext.awaitingWorkoutId())
        assertEquals(coachsPlan(), watchNext.current)
        assertTrue(watchWorkouts.current.isEmpty())

        // The phone sees the workout item is gone and deletes its confirmation.
        phone.receive()
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

        val noCoachPhone = phoneEngine(coach = FakeCoachRepository())

        assertEquals(0, noCoachPhone.receive().delivered)
        assertEquals(listOf(SyncPaths.STATE, SyncPaths.workout(idA)), network.paths)
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

        val report = phone.receive()

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

        val received = phone.receive()
        assertEquals(1, received.delivered)
        assertEquals(8, phoneProgress.current[dips.id])
        // The phone adds its own bodyweight record to the session.
        assertEquals(74.0, phoneSessions.sessions.single().bodyweightKg)

        // The watch sees the confirmation, takes the session off its queue and deletes its item.
        assertEquals(1, watch.sync().delivered)
        assertTrue(watchSessions.queued.isEmpty())
        assertEquals(listOf(SyncPaths.ack(idA)), network.paths)

        // The phone sees the session item is gone and deletes its confirmation.
        phone.receive()
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
        repeat(3) { phone.receive() }
        repeat(3) { watch.sync() }
        repeat(3) { phone.receive() }

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

        assertEquals(2, phone.receive().delivered)
        assertEquals(2, watch.sync().delivered)
        assertTrue(watchSessions.queued.isEmpty())
    }

    @Test
    fun withoutTheDataLayerNothingIsLostAndNothingThrows() = runTest {
        watchSessions.saveAndQueue(session(idA, best = 8))
        network.isAvailable = false

        assertEquals(SyncOutcome.NO_LINK, watch.sync().outcome)
        assertEquals(SyncOutcome.NO_LINK, phone.receive().outcome)
        assertEquals(SyncOutcome.NO_LINK, publish().outcome)
        assertEquals(1, watchSessions.queued.size)

        network.isAvailable = true
        watch.sync()
        assertEquals(1, phone.receive().delivered)
    }

    @Test
    fun unreadableSessionsAreSkippedAndNeverConfirmed() = runTest {
        network.plant("watch", SyncPaths.session(idA), "not json".encodeToByteArray())
        // A valid payload on the wrong path: the id inside must match the path.
        network.plant("watch", SyncPaths.session(idB), SyncCodec.encodeSession(session(idA, best = 8)))

        val report = phone.receive()

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
}
