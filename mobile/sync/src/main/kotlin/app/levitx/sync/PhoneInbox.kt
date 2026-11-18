package app.levitx.sync

import app.levitx.data.coach.CoachRepository
import app.levitx.data.coach.Recorded
import app.levitx.data.equipment.EquipmentRepository
import app.levitx.data.session.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex

/**
 * What the phone takes in from the watch.
 *
 * Each finished workout, cardio and mobility workouts included (and each
 * session from an older watch app), arrives as its own data item. Cardio and
 * mobility go straight into the history, never the coach. The phone records
 * an item, then writes a confirmation item with the same id. The watch
 * deletes its item when it sees the confirmation, and the phone then deletes
 * the confirmation, so nothing is left behind.
 *
 * Every step can be repeated safely. An id is only recorded once, so a pass
 * that is interrupted and run again never double-counts.
 */
@Singleton
internal class PhoneInbox @Inject constructor(
    private val dataLayer: DataLayer,
    private val equipmentRepository: EquipmentRepository,
    private val sessionRepository: SessionRepository,
    private val coachRepository: CoachRepository,
) {
    // One pass at a time: the listener service and the app can both ask for one.
    private val mutex = Mutex()

    /**
     * Records and confirms every workout and session waiting on the Data
     * Layer, and tidies up old confirmations.
     */
    suspend fun receive(): SyncReport = mutex.syncPass {
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
     * set up they wait: confirming them would lose them. The repository
     * decides that in the same transaction as the write, so setup or "delete
     * everything" landing mid-pass can't slip between a check and the save.
     */
    private suspend fun receiveWorkouts(): Received =
        dataLayer.receiveEach(SyncPaths.WORKOUTS_PREFIX, SyncCodec::decodeWorkout, { it.run.id }) { workout ->
            val result = checkNotNull(workout.run.result(workout.finishedAtEpochMillis)) { "checked when decoded" }
            when (coachRepository.recordWorkout(workout.run.id, result)) {
                Recorded.NEW -> Stored.NEW
                Recorded.ALREADY_STORED -> Stored.ALREADY_HERE
                Recorded.NOT_SET_UP -> Stored.NOT_YET
            }
        }

    /**
     * Saves each cardio and mobility workout the watch finished straight into
     * the history. They never reach the coach, so they don't wait for one.
     */
    private suspend fun receiveIntervals(): Received =
        dataLayer.receiveEach(SyncPaths.INTERVALS_PREFIX, SyncCodec::decodeIntervals, { it.id }) { workout ->
            sessionRepository.saveIntervalWorkout(workout.id, workout.sessions).toStored()
        }

    /** Stores each gate session sent by a watch still on an older version of the app. */
    private suspend fun receiveSessions(): Received {
        // Bodyweight never crosses the link, so it is added here from the phone's own record.
        val bodyweightKg = equipmentRepository.observeBodyweightKg().first()
        return dataLayer.receiveEach(SyncPaths.SESSIONS_PREFIX, SyncCodec::decodeSession, { it.id }) { received ->
            sessionRepository.saveReceivedSession(
                received.copy(session = received.session.copy(bodyweightKg = bodyweightKg)),
            ).toStored()
        }
    }

    /** A confirmation has done its job once the watch has deleted the item it answers. */
    private suspend fun removeFinishedAcks(waitingIds: Set<String>) {
        dataLayer.list(SyncPaths.ACKS_PREFIX)
            .filter { ack -> ack.isOwn && ack.path.removePrefix(SyncPaths.ACKS_PREFIX) !in waitingIds }
            .forEach { ack -> dataLayer.delete(ack.path) }
    }
}
