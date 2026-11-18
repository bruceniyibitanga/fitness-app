package app.levitx.sync

import app.levitx.data.DataUnavailableException
import app.levitx.data.coach.CoachRepository
import app.levitx.data.equipment.EquipmentRepository
import app.levitx.data.session.SessionRepository
import app.levitx.data.settings.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The phone's side of sync.
 *
 * - **Out:** the goal, bests, bands and the coach's next session go to the
 *   watch as one data item. The coach itself stays on the phone.
 * - **In:** each finished workout, cardio and mobility workouts included
 *   (and each session from an older watch app), arrives as its own data
 *   item. Cardio and mobility go straight into the history, never the
 *   coach. The phone records it, then writes a confirmation item with the
 *   same id. The watch deletes its item when it sees the confirmation, and
 *   the phone then deletes the confirmation, so nothing is left behind.
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
    private val settingsRepository: SettingsRepository,
) {
    // One pass at a time: the listener service and the app can both ask for one.
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
    suspend fun publish(state: PhoneState): SyncReport = guarded {
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
     */
    suspend fun forget(): SyncReport = guarded {
        dataLayer.put(SyncPaths.FORGET, ByteArray(0))
        dataLayer.delete(SyncPaths.STATE)
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
        val received = listOf(receiveWorkouts(), receiveIntervals(), receiveSessions())
        removeFinishedAcks(received.flatMap { it.waitingIds }.toSet())
        SyncReport(
            SyncOutcome.DONE,
            delivered = received.sumOf { it.stored },
            skipped = received.sumOf { it.skipped },
        )
    }

    /**
     * Records each workout the watch finished with the coach. With no coach
     * set up yet, they wait: confirming them would lose them.
     */
    private suspend fun receiveWorkouts(): Received {
        if (coachRepository.observeState().first() == null) {
            val items = dataLayer.list(SyncPaths.WORKOUTS_PREFIX)
            return Received(waitingIds = items.map { it.path.removePrefix(SyncPaths.WORKOUTS_PREFIX) }.toSet())
        }
        return dataLayer.receiveEach(SyncPaths.WORKOUTS_PREFIX, SyncCodec::decodeWorkout, { it.run.id }) { workout ->
            val result = checkNotNull(workout.run.result(workout.finishedAtEpochMillis)) { "checked when decoded" }
            coachRepository.recordWorkout(workout.run.id, result)
        }
    }

    /**
     * Saves each cardio and mobility workout the watch finished straight into
     * the history. They never reach the coach, so they don't wait for one.
     */
    private suspend fun receiveIntervals(): Received =
        dataLayer.receiveEach(SyncPaths.INTERVALS_PREFIX, SyncCodec::decodeIntervals, { it.id }) { workout ->
            sessionRepository.saveIntervalWorkout(workout.id, workout.sessions)
        }

    /** Stores each gate session sent by a watch still on an older version of the app. */
    private suspend fun receiveSessions(): Received {
        // Bodyweight never crosses the link, so it is added here from the phone's own record.
        val bodyweightKg = equipmentRepository.observeBodyweightKg().first()
        return dataLayer.receiveEach(SyncPaths.SESSIONS_PREFIX, SyncCodec::decodeSession, { it.id }) { received ->
            sessionRepository.saveReceivedSession(
                received.copy(session = received.session.copy(bodyweightKg = bodyweightKg)),
            )
        }
    }

    /** A confirmation has done its job once the watch has deleted the item it answers. */
    private suspend fun removeFinishedAcks(waitingIds: Set<String>) {
        dataLayer.list(SyncPaths.ACKS_PREFIX)
            .filter { ack -> ack.isOwn && ack.path.removePrefix(SyncPaths.ACKS_PREFIX) !in waitingIds }
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
