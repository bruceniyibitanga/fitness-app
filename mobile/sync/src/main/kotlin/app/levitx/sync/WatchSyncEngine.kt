package app.levitx.sync

import app.levitx.data.DataUnavailableException
import app.levitx.data.coach.WorkoutOutbox
import app.levitx.data.session.SessionOutbox
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The watch's side of sync.
 *
 * - **In:** the phone's state is copied into the watch's own database by
 *   [PhoneStateApplier].
 * - **Out:** every queued workout is put on the Data Layer and stays queued
 *   until the phone's confirmation arrives. Only then is it taken off the
 *   queue and its data item deleted. Sessions queued by an older version of
 *   the app go the same way.
 *
 * A workout has a unique id and is recorded once per device, so nothing here
 * can lose or double-count anything, whatever order things happen in.
 */
@Singleton
internal class WatchSyncEngine @Inject constructor(
    private val dataLayer: DataLayer,
    private val applier: PhoneStateApplier,
    private val outbox: SessionOutbox,
    private val workoutOutbox: WorkoutOutbox,
) {
    // One pass at a time: the listener service and the app can both ask for one.
    private val mutex = Mutex()

    /** Runs one full pass: take in the phone's state, then send and confirm workouts. */
    suspend fun sync(): SyncReport = mutex.withLock {
        try {
            val stateResult = applyPhoneState()
            val delivered = sendQueued()
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
            is Decoded.Ok -> if (applier.applyState(decoded.value)) StateResult.APPLIED else StateResult.NONE
            else -> StateResult.SKIPPED
        }
    }

    /** @return How many workouts (and sessions from an older version of the app) the phone confirmed. */
    private suspend fun sendQueued(): Int {
        val confirmed = confirmedIds()
        val workouts = workoutOutbox.observeQueued().first()
            .associate { workout -> workout.run.id to { SyncCodec.encodeWorkout(workout) } }
        val sessions = outbox.observeQueued().first()
            .associate { stored -> stored.id to { SyncCodec.encodeSession(stored) } }
        return send(SyncPaths.WORKOUTS_PREFIX, workouts, confirmed, workoutOutbox::markDelivered) +
            send(SyncPaths.SESSIONS_PREFIX, sessions, confirmed, outbox::markDelivered)
    }

    /**
     * Puts each of [queued] (id to payload) on the Data Layer under [prefix],
     * or, once the phone has [confirmed] it, takes it off the queue with
     * [markDelivered] and deletes its item.
     *
     * @return How many the phone confirmed.
     */
    private suspend fun send(
        prefix: String,
        queued: Map<String, () -> ByteArray>,
        confirmed: Set<String>,
        markDelivered: suspend (String) -> Unit,
    ): Int {
        var delivered = 0
        for ((id, payload) in queued) {
            if (id in confirmed) {
                // Off the queue first. If the delete below doesn't happen, the
                // leftover item is cleaned up on the next pass.
                markDelivered(id)
                dataLayer.delete(prefix + id)
                delivered++
            } else {
                dataLayer.put(prefix + id, payload())
            }
        }
        removeLeftoverItems(prefix, stillQueuedPaths = queued.keys.map { prefix + it }.toSet())
        return delivered
    }

    /** Ids the phone has confirmed, workouts and sessions alike. */
    private suspend fun confirmedIds(): Set<String> = dataLayer.list(SyncPaths.ACKS_PREFIX)
        .map { it.path.removePrefix(SyncPaths.ACKS_PREFIX) }
        .toSet()

    /**
     * Deletes this watch's items under [prefix] that are no longer queued.
     * Items from another watch are left alone: they are that watch's to delete.
     */
    private suspend fun removeLeftoverItems(prefix: String, stillQueuedPaths: Set<String>) {
        dataLayer.list(prefix)
            .filter { item -> item.isOwn && item.path !in stillQueuedPaths }
            .forEach { item -> dataLayer.delete(item.path) }
    }

    private enum class StateResult { NONE, APPLIED, SKIPPED }
}
