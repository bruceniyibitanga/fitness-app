package app.levitx.sync

import app.levitx.data.DataUnavailableException
import app.levitx.data.settings.SettingsRepository
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
     * After the person deletes everything: tells the watch to delete its copy
     * too, and takes the phone's published state off the Data Layer, so
     * nothing about them is offered to the watch again. The watch acts on it
     * the next time it is connected, even if that is days later.
     *
     * @param resetId The id of this delete, which stays on offer for good.
     */
    suspend fun forget(resetId: String): SyncReport = withdraw(resetId)

    /**
     * Makes the Data Layer say what is true when the phone has nothing to
     * offer: no state, and, if the person has ever deleted everything
     * ([resetId] isn't null), the request to delete and its id.
     *
     * The publisher calls this whenever the state turns out empty, so the
     * end result is right whatever order a delete and a last publish from
     * the emptying database happen to land in.
     */
    suspend fun withdraw(resetId: String?): SyncReport = mutex.syncPass {
        dataLayer.delete(SyncPaths.STATE)
        if (resetId != null) {
            dataLayer.put(SyncPaths.RESET, ResetCodec.encode(resetId))
            dataLayer.put(SyncPaths.FORGET, ByteArray(0))
        }
        SyncReport(SyncOutcome.DONE, stateChanged = true)
    }

    /**
     * Offers the watch the id of the last "delete everything" again. The
     * item is the same every time, so repeating it costs nothing, and it
     * heals a delete whose first announcement never got through.
     */
    suspend fun announceReset(resetId: String): SyncReport = mutex.syncPass {
        dataLayer.put(SyncPaths.RESET, ResetCodec.encode(resetId))
        SyncReport(SyncOutcome.DONE)
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
