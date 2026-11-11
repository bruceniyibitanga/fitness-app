package app.levitx.wear.session

import app.levitx.data.session.SessionDraftRepository
import app.levitx.data.session.SessionOutbox
import app.levitx.data.session.StoredSession
import app.levitx.data.time.TrainingClock
import javax.inject.Inject

/**
 * Older versions of the app ran one gate at a time on the watch. A gate
 * session one of them left in progress is saved as it stands and sent to the
 * phone the old way, so updating the app mid-session loses nothing.
 */
class LeftoverGateSession @Inject constructor(
    private val drafts: SessionDraftRepository,
    private val outbox: SessionOutbox,
    private val clock: TrainingClock,
) {
    /** Saves and queues the leftover session, if there is one with sets done, and clears it. */
    suspend fun save() {
        val leftover = drafts.load() ?: return
        leftover.complete(finishedAtEpochMillis = clock.currentTimeMillis())?.let { session ->
            outbox.saveAndQueue(StoredSession(leftover.id, session))
        }
        drafts.clear()
    }
}
