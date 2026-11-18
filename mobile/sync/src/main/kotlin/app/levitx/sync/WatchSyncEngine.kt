package app.levitx.sync

import app.levitx.data.DataUnavailableException
import app.levitx.data.coach.WorkoutOutbox
import app.levitx.data.intervals.IntervalOutbox
import app.levitx.data.privacy.PrivacyRepository
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
 * - **Out:** every queued workout, cardio and mobility included, is put on
 *   the Data Layer and stays queued until the phone's confirmation arrives.
 *   Only then is it taken off the queue and its data item deleted. Sessions queued by an older version of
 *   the app go the same way.
 *
 * - **Forgetting:** when the person deletes everything on the phone, the
 *   phone puts up [SyncPaths.FORGET]. While it is there, the watch deletes
 *   everything it holds, queued workouts included, and takes nothing new.
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
    private val intervalOutbox: IntervalOutbox,
    private val privacy: PrivacyRepository,
) {
    // One pass at a time: the listener service and the app can both ask for one.
    private val mutex = Mutex()

    /**
     * Runs one full pass: take in the phone's state, then send and confirm
     * workouts. Or, if the phone asked, delete everything instead.
     */
    suspend fun sync(): SyncReport = mutex.withLock {
        try {
            if (phoneForgot()) forgetEverything() else exchange()
        } catch (error: DataLayerUnavailableException) {
            SyncReport(SyncOutcome.NO_LINK)
        } catch (error: DataUnavailableException) {
            SyncReport(SyncOutcome.DATA_LOCKED)
        }
    }

    private suspend fun exchange(): SyncReport {
        val stateResult = applyPhoneState()
        val delivered = sendQueued()
        return SyncReport(
            outcome = SyncOutcome.DONE,
            delivered = delivered,
            skipped = if (stateResult == StateResult.SKIPPED) 1 else 0,
            stateChanged = stateResult == StateResult.APPLIED,
        )
    }

    private suspend fun phoneForgot(): Boolean = dataLayer.list(SyncPaths.FORGET).any { it.path == SyncPaths.FORGET }

    /**
     * Deletes everything on the watch, as the person did on the phone, then
     * removes the watch's own workout items from the Data Layer.
     *
     * Only deletes when there is something to delete. Deleting changes the
     * database, which starts another pass; that pass finds nothing and stops.
     */
    private suspend fun forgetEverything(): SyncReport {
        val holdsAnything = applier.holdsAnything() ||
            workoutOutbox.observeQueued().first().isNotEmpty() ||
            intervalOutbox.observeQueued().first().isNotEmpty() ||
            outbox.observeQueued().first().isNotEmpty()
        if (holdsAnything) privacy.deleteEverything()
        // The queues are empty now, so this deletes whatever the watch still had on the Data Layer.
        sendQueued()
        return SyncReport(SyncOutcome.DONE, stateChanged = holdsAnything)
    }

    private suspend fun applyPhoneState(): StateResult {
        val item = dataLayer.list(SyncPaths.STATE).firstOrNull { it.path == SyncPaths.STATE }
        return when (val decoded = item?.let { SyncCodec.decodeState(it.payload) }) {
            null -> StateResult.NONE
            is Decoded.Ok -> if (applier.applyState(decoded.value)) StateResult.APPLIED else StateResult.NONE
            else -> StateResult.SKIPPED
        }
    }

    /**
     * @return How many workouts, cardio and mobility workouts and sessions
     *   from an older version of the app the phone confirmed.
     */
    private suspend fun sendQueued(): Int {
        val confirmed = confirmedIds()
        val workouts = workoutOutbox.observeQueued().first()
            .associate { workout -> workout.run.id to { SyncCodec.encodeWorkout(workout) } }
        val intervals = intervalOutbox.observeQueued().first()
            .associate { workout -> workout.id to { SyncCodec.encodeIntervals(workout) } }
        val sessions = outbox.observeQueued().first()
            .associate { stored -> stored.id to { SyncCodec.encodeSession(stored) } }
        return send(SyncPaths.WORKOUTS_PREFIX, workouts, confirmed, workoutOutbox::markDelivered) +
            send(SyncPaths.INTERVALS_PREFIX, intervals, confirmed, intervalOutbox::markDelivered) +
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
