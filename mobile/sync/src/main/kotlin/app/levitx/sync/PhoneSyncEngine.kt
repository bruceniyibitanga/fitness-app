package app.levitx.sync

import app.levitx.data.DataUnavailableException
import app.levitx.data.coach.CoachRepository
import app.levitx.data.equipment.EquipmentRepository
import app.levitx.data.session.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Payload of a confirmation item. Only its path matters; the byte is the wire version. */
private val ACK_PAYLOAD = byteArrayOf(WIRE_VERSION.toByte())

/**
 * The phone's side of sync.
 *
 * - **Out:** the goal, bests, bands and the coach's next session go to the
 *   watch as one data item. The coach itself stays on the phone.
 * - **In:** each finished workout (and each session from an older watch app)
 *   arrives as its own data item. The phone records it, then writes a
 *   confirmation item with the same id. The watch deletes its item when it
 *   sees the confirmation, and the phone then deletes the confirmation, so
 *   nothing is left behind.
 *
 * Every step can be repeated safely. An id is only recorded once, so a pass
 * that is interrupted and run again never double-counts.
 */
@Singleton
internal class PhoneSyncEngine @Inject constructor(
    private val dataLayer: DataLayer,
    private val stateSource: PhoneStateSource,
    private val equipmentRepository: EquipmentRepository,
    private val sessionRepository: SessionRepository,
    private val coachRepository: CoachRepository,
) {
    // One pass at a time: the listener service and the app can both ask for one.
    private val mutex = Mutex()

    /** What the watch needs, emitted again whenever any of it changes. Null until there is anything to send. */
    fun watchState(): Flow<PhoneState?> = stateSource.observe()

    /** Publishes [state] for the watch. It is delivered when the watch is next reachable. */
    suspend fun publish(state: PhoneState): SyncReport = guarded {
        dataLayer.put(SyncPaths.STATE, SyncCodec.encodeState(state))
        SyncReport(SyncOutcome.DONE, stateChanged = true)
    }

    /**
     * Publishes the state as it is right now. Used after sessions arrive, so
     * the watch gets the new bests even if the system stops the app before
     * the change is noticed on its own. Publishing unchanged state does nothing.
     */
    suspend fun publishCurrent(): SyncReport {
        val state = try {
            watchState().first()
        } catch (error: DataUnavailableException) {
            return SyncReport(SyncOutcome.DATA_LOCKED)
        }
        return if (state == null) SyncReport(SyncOutcome.DONE) else publish(state)
    }

    /**
     * Records and confirms every workout and session waiting on the Data
     * Layer, and tidies up old confirmations.
     */
    suspend fun receive(): SyncReport = guarded {
        val workouts = receiveWorkouts()
        val sessions = receiveSessions()
        removeFinishedAcks(workouts.waitingIds + sessions.waitingIds)
        SyncReport(
            SyncOutcome.DONE,
            delivered = workouts.stored + sessions.stored,
            skipped = workouts.skipped + sessions.skipped,
        )
    }

    /**
     * Records each workout the watch finished with the coach. With no coach
     * set up yet, they wait: confirming them would lose them.
     */
    private suspend fun receiveWorkouts(): Received {
        val items = dataLayer.list(SyncPaths.WORKOUTS_PREFIX)
        if (items.isEmpty() || coachRepository.observeState().first() == null) {
            return Received(waitingIds = items.map { it.path.removePrefix(SyncPaths.WORKOUTS_PREFIX) }.toSet())
        }
        var stored = 0
        var skipped = 0
        for (item in items) {
            val decoded = SyncCodec.decodeWorkout(item.payload)
            // The id in the payload must be the id in the path it arrived on.
            if (decoded !is Decoded.Ok || item.path != SyncPaths.workout(decoded.value.run.id)) {
                skipped++
                continue
            }
            val workout = decoded.value
            val result = checkNotNull(workout.run.result(workout.finishedAtEpochMillis)) { "checked when decoded" }
            if (coachRepository.recordWorkout(workout.run.id, result)) stored++
            // Confirm only after it is safely recorded. Confirming again is harmless.
            dataLayer.put(SyncPaths.ack(workout.run.id), ACK_PAYLOAD)
        }
        return Received(stored, skipped, items.map { it.path.removePrefix(SyncPaths.WORKOUTS_PREFIX) }.toSet())
    }

    /** Stores each gate session sent by a watch still on an older version of the app. */
    private suspend fun receiveSessions(): Received {
        val items = dataLayer.list(SyncPaths.SESSIONS_PREFIX)
        // Bodyweight never crosses the link, so it is added here from the phone's own record.
        val bodyweightKg = equipmentRepository.observeBodyweightKg().first()
        var stored = 0
        var skipped = 0
        for (item in items) {
            val decoded = SyncCodec.decodeSession(item.payload)
            // The id in the payload must be the id in the path it arrived on.
            if (decoded !is Decoded.Ok || item.path != SyncPaths.session(decoded.value.id)) {
                skipped++
                continue
            }
            val received = decoded.value
            val withBodyweight = received.copy(session = received.session.copy(bodyweightKg = bodyweightKg))
            if (sessionRepository.saveReceivedSession(withBodyweight)) stored++
            // Confirm only after it is safely stored. Confirming again is harmless.
            dataLayer.put(SyncPaths.ack(received.id), ACK_PAYLOAD)
        }
        return Received(stored, skipped, items.map { it.path.removePrefix(SyncPaths.SESSIONS_PREFIX) }.toSet())
    }

    /** A confirmation has done its job once the watch has deleted the item it answers. */
    private suspend fun removeFinishedAcks(waitingIds: Set<String>) {
        dataLayer.list(SyncPaths.ACKS_PREFIX)
            .filter { ack -> ack.isOwn && ack.path.removePrefix(SyncPaths.ACKS_PREFIX) !in waitingIds }
            .forEach { ack -> dataLayer.delete(ack.path) }
    }

    /**
     * What one kind of incoming item came to.
     *
     * @property stored Items newly recorded.
     * @property skipped Items left alone because they were unreadable.
     * @property waitingIds Ids of every item still on the Data Layer, recorded or not.
     */
    private data class Received(val stored: Int = 0, val skipped: Int = 0, val waitingIds: Set<String>)

    private suspend fun guarded(pass: suspend () -> SyncReport): SyncReport = mutex.withLock {
        try {
            pass()
        } catch (error: DataLayerUnavailableException) {
            SyncReport(SyncOutcome.NO_LINK)
        } catch (error: DataUnavailableException) {
            SyncReport(SyncOutcome.DATA_LOCKED)
        }
    }
}
