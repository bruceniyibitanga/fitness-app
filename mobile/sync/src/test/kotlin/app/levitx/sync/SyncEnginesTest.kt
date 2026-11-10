package app.levitx.sync

import app.levitx.core.session.CompletedSession
import app.levitx.core.session.LoggedSet
import app.levitx.core.skills.SkillCatalogue
import app.levitx.core.skills.SkillPath
import app.levitx.data.DataUnavailableException
import app.levitx.data.equipment.Band
import app.levitx.data.goal.GoalRepository
import app.levitx.data.session.StoredSession
import app.levitx.testing.FakeEquipmentRepository
import app.levitx.testing.FakeGoalRepository
import app.levitx.testing.FakeProgressRepository
import app.levitx.testing.FakeSessionRepository
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
    private val phone =
        PhoneSyncEngine(network.onDevice("phone"), phoneGoal, phoneProgress, phoneEquipment, phoneSessions)

    private val watchGoal = FakeGoalRepository()
    private val watchProgress = FakeProgressRepository()
    private val watchEquipment = FakeEquipmentRepository()
    private val watchSessions = FakeSessionRepository(watchProgress)
    private val watch =
        WatchSyncEngine(network.onDevice("watch"), watchGoal, watchProgress, watchEquipment, watchSessions)

    private fun session(id: String, best: Int) = StoredSession(
        id = id,
        session = CompletedSession(dips.id, 1_000, 2_000, listOf(LoggedSet(4, 2, false), LoggedSet(best, 0, true))),
    )

    private val idA = "00000000-0000-4000-8000-00000000000a"
    private val idB = "00000000-0000-4000-8000-00000000000b"

    private suspend fun publish() = phone.publish(checkNotNull(phone.watchState().first()))

    @Test
    fun theWatchCopiesThePhonesGoalBestsAndBands() = runTest {
        publish()
        val report = watch.sync()

        assertTrue(report.stateChanged)
        assertEquals(path, watchGoal.current)
        assertEquals(phoneProgress.current, watchProgress.current)
        assertEquals(phoneEquipment.bands, watchEquipment.bands)
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
    fun aSessionTravelsToThePhoneAndEverythingIsTidiedUp() = runTest {
        watchSessions.saveAndQueue(session(idA, best = 8))

        watch.sync()
        assertEquals(listOf(SyncPaths.session(idA)), network.paths)

        val received = phone.receiveSessions()
        assertEquals(1, received.delivered)
        assertEquals(8, phoneProgress.current[dips.id])
        // The phone adds its own bodyweight record to the session.
        assertEquals(74.0, phoneSessions.sessions.single().bodyweightKg)

        // The watch sees the confirmation, takes the session off its queue and deletes its item.
        assertEquals(1, watch.sync().delivered)
        assertTrue(watchSessions.queued.isEmpty())
        assertEquals(listOf(SyncPaths.ack(idA)), network.paths)

        // The phone sees the session item is gone and deletes its confirmation.
        phone.receiveSessions()
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
    fun withNoGoalThereIsNothingToPublish() = runTest {
        val noGoalPhone = PhoneSyncEngine(
            network.onDevice("phone"),
            FakeGoalRepository(),
            phoneProgress,
            phoneEquipment,
            phoneSessions,
        )

        assertEquals(SyncReport(SyncOutcome.DONE), noGoalPhone.publishCurrent())
        assertTrue(network.paths.isEmpty())
    }

    @Test
    fun retriesNeverDoubleCount() = runTest {
        watchSessions.saveAndQueue(session(idA, best = 8))
        repeat(3) { watch.sync() }
        repeat(3) { phone.receiveSessions() }
        repeat(3) { watch.sync() }
        repeat(3) { phone.receiveSessions() }

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

        assertEquals(2, phone.receiveSessions().delivered)
        assertEquals(2, watch.sync().delivered)
        assertTrue(watchSessions.queued.isEmpty())
    }

    @Test
    fun withoutTheDataLayerNothingIsLostAndNothingThrows() = runTest {
        watchSessions.saveAndQueue(session(idA, best = 8))
        network.isAvailable = false

        assertEquals(SyncOutcome.NO_LINK, watch.sync().outcome)
        assertEquals(SyncOutcome.NO_LINK, phone.receiveSessions().outcome)
        assertEquals(SyncOutcome.NO_LINK, publish().outcome)
        assertEquals(1, watchSessions.queued.size)

        network.isAvailable = true
        watch.sync()
        assertEquals(1, phone.receiveSessions().delivered)
    }

    @Test
    fun unreadableSessionsAreSkippedAndNeverConfirmed() = runTest {
        network.plant("watch", SyncPaths.session(idA), "not json".encodeToByteArray())
        // A valid payload on the wrong path: the id inside must match the path.
        network.plant("watch", SyncPaths.session(idB), SyncCodec.encodeSession(session(idA, best = 8)))

        val report = phone.receiveSessions()

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
        val lockedWatch =
            WatchSyncEngine(network.onDevice("watch"), lockedGoal, watchProgress, watchEquipment, watchSessions)
        publish()

        assertEquals(SyncOutcome.DATA_LOCKED, lockedWatch.sync().outcome)
    }
}
