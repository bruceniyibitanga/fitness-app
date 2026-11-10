package app.levitx.sync

import app.levitx.data.DataUnavailableException
import app.levitx.data.equipment.EquipmentRepository
import app.levitx.data.goal.GoalRepository
import app.levitx.data.progress.ProgressRepository
import app.levitx.data.session.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Payload of a confirmation item. Only its path matters; the byte is the wire version. */
private val ACK_PAYLOAD = byteArrayOf(WIRE_VERSION.toByte())

/**
 * The phone's side of sync.
 *
 * - **Out:** the goal, bests and bands go to the watch as one data item.
 * - **In:** each finished session arrives as its own data item. The phone
 *   stores it, then writes a confirmation item with the same id. The watch
 *   deletes its session item when it sees the confirmation, and the phone
 *   then deletes the confirmation, so nothing is left behind.
 *
 * Every step can be repeated safely. A session id is only stored once, so a
 * pass that is interrupted and run again never double-counts.
 */
@Singleton
internal class PhoneSyncEngine @Inject constructor(
    private val dataLayer: DataLayer,
    private val goalRepository: GoalRepository,
    private val progressRepository: ProgressRepository,
    private val equipmentRepository: EquipmentRepository,
    private val sessionRepository: SessionRepository,
) {
    // One pass at a time: the listener service and the app can both ask for one.
    private val mutex = Mutex()

    /** What the watch needs, emitted again whenever any of it changes. Null until a goal is chosen. */
    fun watchState(): Flow<PhoneState?> = combine(
        goalRepository.observeActiveGoal(),
        progressRepository.observeBests(),
        equipmentRepository.observeBands(),
    ) { path, bests, bands -> path?.let { PhoneState(it, bests, bands) } }.distinctUntilChanged()

    /** Publishes [state] for the watch. It is delivered when the watch is next reachable. */
    suspend fun publish(state: PhoneState): SyncReport = guarded {
        dataLayer.put(SyncPaths.STATE, SyncCodec.encodeState(state))
        SyncReport(SyncOutcome.DONE, stateChanged = true)
    }

    /** Stores and confirms every session waiting on the Data Layer, and tidies up old confirmations. */
    suspend fun receiveSessions(): SyncReport = guarded {
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
        removeFinishedAcks(waitingSessionPaths = items.map { it.path }.toSet())
        SyncReport(SyncOutcome.DONE, delivered = stored, skipped = skipped)
    }

    /** A confirmation has done its job once the watch has deleted the session item it answers. */
    private suspend fun removeFinishedAcks(waitingSessionPaths: Set<String>) {
        dataLayer.list(SyncPaths.ACKS_PREFIX)
            .filter { ack -> ack.isOwn }
            .filter { ack -> SyncPaths.session(ack.path.removePrefix(SyncPaths.ACKS_PREFIX)) !in waitingSessionPaths }
            .forEach { ack -> dataLayer.delete(ack.path) }
    }

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
