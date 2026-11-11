package app.levitx.sync

import app.levitx.data.DataUnavailableException
import app.levitx.data.equipment.EquipmentRepository
import app.levitx.data.goal.GoalRepository
import app.levitx.data.progress.ProgressRepository
import app.levitx.data.session.SessionOutbox
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The watch's side of sync.
 *
 * - **In:** the phone's goal, bests and bands are copied into the watch's own
 *   database, so the watch plans and runs sessions with the phone out of reach.
 * - **Out:** every queued session is put on the Data Layer and stays queued
 *   until the phone's confirmation arrives. Only then is it taken off the
 *   queue and its data item deleted.
 *
 * None of this can lose or double-count anything, whatever order things
 * happen in:
 * - The goal belongs to the phone. The watch only ever copies it.
 * - A best only ever goes up, so applying bests twice or late changes nothing.
 * - A session has a unique id and is stored once per device.
 */
@Singleton
internal class WatchSyncEngine @Inject constructor(
    private val dataLayer: DataLayer,
    private val goalRepository: GoalRepository,
    private val progressRepository: ProgressRepository,
    private val equipmentRepository: EquipmentRepository,
    private val outbox: SessionOutbox,
) {
    // One pass at a time: the listener service and the app can both ask for one.
    private val mutex = Mutex()

    /** Runs one full pass: take in the phone's state, then send and confirm sessions. */
    suspend fun sync(): SyncReport = mutex.withLock {
        try {
            val stateResult = applyPhoneState()
            val delivered = sendSessions()
            SyncReport(
                outcome = SyncOutcome.DONE,
                delivered = delivered,
                skipped = if (stateResult == StateResult.SKIPPED) 1 else 0,
                stateChanged = stateResult == StateResult.APPLIED,
            )
        } catch (error: DataLayerUnavailableException) {
            SyncReport(SyncOutcome.NO_LINK)
        } catch (error: DataUnavailableException) {
            SyncReport(SyncOutcome.DATA_LOCKED)
        }
    }

    private suspend fun applyPhoneState(): StateResult {
        val item = dataLayer.list(SyncPaths.STATE).firstOrNull { it.path == SyncPaths.STATE }
        return when (val decoded = item?.let { SyncCodec.decodeState(it.payload) }) {
            null -> StateResult.NONE
            is Decoded.Ok -> if (apply(decoded.value)) StateResult.APPLIED else StateResult.NONE
            else -> StateResult.SKIPPED
        }
    }

    /** @return True when anything on the watch changed. */
    private suspend fun apply(state: PhoneState): Boolean {
        var changed = false
        if (goalRepository.observeActiveGoal().first() != state.path) {
            goalRepository.setActiveGoal(state.path)
            changed = true
        }
        val bests = progressRepository.observeBests().first()
        state.bests.filter { (gateId, best) -> best > (bests[gateId] ?: 0) }.forEach { (gateId, best) ->
            // recordBest keeps the higher value, so a best set on the watch is never lowered.
            progressRepository.recordBest(gateId, best)
            changed = true
        }
        if (equipmentRepository.observeBands().first() != state.bands) {
            equipmentRepository.replaceBands(state.bands)
            changed = true
        }
        return changed
    }

    /** @return How many sessions the phone confirmed in this pass. */
    private suspend fun sendSessions(): Int {
        val queued = outbox.observeQueued().first()
        val confirmedIds = dataLayer.list(SyncPaths.ACKS_PREFIX)
            .map { it.path.removePrefix(SyncPaths.ACKS_PREFIX) }
            .toSet()
        var delivered = 0
        for (stored in queued) {
            if (stored.id in confirmedIds) {
                // Off the queue first. If the delete below doesn't happen, the
                // leftover item is cleaned up on the next pass.
                outbox.markDelivered(stored.id)
                dataLayer.delete(SyncPaths.session(stored.id))
                delivered++
            } else {
                dataLayer.put(SyncPaths.session(stored.id), SyncCodec.encodeSession(stored))
            }
        }
        removeLeftoverSessionItems(stillQueuedPaths = queued.map { SyncPaths.session(it.id) }.toSet())
        return delivered
    }

    /**
     * Deletes this watch's session items that are no longer queued. Items
     * from another watch are left alone: they are that watch's to delete.
     */
    private suspend fun removeLeftoverSessionItems(stillQueuedPaths: Set<String>) {
        dataLayer.list(SyncPaths.SESSIONS_PREFIX)
            .filter { item -> item.isOwn && item.path !in stillQueuedPaths }
            .forEach { item -> dataLayer.delete(item.path) }
    }

    private enum class StateResult { NONE, APPLIED, SKIPPED }
}
