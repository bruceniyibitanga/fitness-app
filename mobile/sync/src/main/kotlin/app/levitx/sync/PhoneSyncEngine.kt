package app.levitx.sync

import app.levitx.data.DataUnavailableException
import app.levitx.data.privacy.ResetMarker
import app.levitx.data.settings.SettingsRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex

/**
 * What the phone offers the watch.
 *
 * The goal, bests, bands, the unit and the coach's next session go to the
 * watch as one data item. The coach itself stays on the phone. When the
 * person deletes everything, the item is taken away and the watch is told to
 * delete its copy. What comes back from the watch is [PhoneInbox]'s job.
 *
 * Every step can be repeated safely: putting the same item again changes
 * nothing.
 */
@Singleton
internal class PhoneSyncEngine @Inject constructor(
    private val dataLayer: DataLayer,
    private val stateSource: PhoneStateSource,
    private val settingsRepository: SettingsRepository,
    private val resetMarker: ResetMarker,
) {
    // One pass at a time: a publish and a delete must not interleave.
    private val mutex = Mutex()

    /**
     * What the watch needs, emitted again whenever any of it changes. Null
     * until there is anything to send. The unit rides along so the watch
     * shows weights the way the phone does.
     */
    fun watchState(): Flow<PhoneState?> = combine(
        stateSource.observe(),
        settingsRepository.observe().map { it.units }.distinctUntilChanged(),
    ) { state, units -> state?.copy(units = units) }

    /** Publishes [state] for the watch. It is delivered when the watch is next reachable. */
    suspend fun publish(state: PhoneState): SyncReport = mutex.syncPass {
        dataLayer.put(SyncPaths.STATE, SyncCodec.encodeState(state))
        // The person set up again after deleting everything: the watch can keep what it gets from now on.
        dataLayer.delete(SyncPaths.FORGET)
        SyncReport(SyncOutcome.DONE, stateChanged = true)
    }

    /**
     * When the person deletes everything: tells the watch to delete its copy
     * too, and takes the phone's published state off the Data Layer, so
     * nothing about them is offered to the watch again.
     *
     * The delete gets a new random id, saved before anything is sent. If the
     * watch can't be told now (no link, or the app is closed first), the id
     * is offered again every time the app starts ([announceLastReset]), and
     * the watch deletes its copy when it first sees an id it hasn't acted on,
     * even if that is days later and the person has set up again since.
     */
    suspend fun forget(): SyncReport {
        resetMarker.set(UUID.randomUUID().toString())
        return withdraw()
    }

    /**
     * Offers the watch the id of the last "delete everything" again, if
     * there has ever been one. The item is the same every time, so repeating
     * it costs nothing, and it heals a delete whose first announcement never
     * got through.
     *
     * @return The report, or null when the person has never deleted everything.
     */
    suspend fun announceLastReset(): SyncReport? = resetMarker.current()?.let { resetId ->
        mutex.syncPass {
            dataLayer.put(SyncPaths.RESET, ResetCodec.encode(resetId))
            SyncReport(SyncOutcome.DONE)
        }
    }

    /**
     * Keeps the Data Layer saying what is true for as long as it is
     * collected: each new state is published, and when there is nothing to
     * offer (before setup, or after everything was deleted) that is said too.
     * Emits the report of each pass.
     *
     * Because "nothing to offer" is acted on like any other state, the end
     * result is right whatever order a delete and a last publish from the
     * emptying database happen to land in.
     */
    fun publishing(): Flow<SyncReport> = watchState().map { state ->
        if (state == null) withdraw() else publish(state)
    }

    /**
     * Takes the state off the Data Layer and, if the person has ever deleted
     * everything, puts up the request to delete and the id of the last one.
     * A phone that has never deleted anything never asks a watch to delete.
     */
    private suspend fun withdraw(): SyncReport = mutex.syncPass {
        dataLayer.delete(SyncPaths.STATE)
        resetMarker.current()?.let { resetId ->
            dataLayer.put(SyncPaths.RESET, ResetCodec.encode(resetId))
            dataLayer.put(SyncPaths.FORGET, ByteArray(0))
        }
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
}
