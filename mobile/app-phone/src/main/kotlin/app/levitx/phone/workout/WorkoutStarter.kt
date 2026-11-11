package app.levitx.phone.workout

import app.levitx.core.coach.PlannedSession
import app.levitx.core.coach.SuggestionChoice
import app.levitx.core.coach.WorkoutRun
import app.levitx.data.coach.WorkoutDraftRepository
import app.levitx.data.time.TrainingClock
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Starts a workout by saving it as the draft, the one place the workout in
 * progress lives. Today and the Train tab both start workouts through here,
 * so the rule is written once: a workout already in progress is never
 * replaced, it is resumed.
 */
class WorkoutStarter @Inject constructor(
    private val drafts: WorkoutDraftRepository,
    private val clock: TrainingClock,
) {
    /**
     * Saves [session] as a new workout, unless one is already in progress.
     *
     * @param choice What the person picked when the coach offered an easier day, or null.
     * @return [Started.NEW] when [session] was saved, [Started.RESUMING] when a
     *   workout was already in progress and that one should open instead.
     * @throws app.levitx.data.DataUnavailableException if the data can't be read or written.
     */
    suspend fun start(session: PlannedSession, choice: SuggestionChoice? = null): Started {
        if (drafts.observeDraft().first() != null) return Started.RESUMING
        drafts.save(
            WorkoutRun(
                // Random, so saving the finished workout twice can never store it twice.
                id = UUID.randomUUID().toString(),
                session = session,
                startedAtEpochMillis = clock.currentTimeMillis(),
                choice = choice,
            ),
        )
        return Started.NEW
    }

    /** What [start] did. Either way there is a workout to open. */
    enum class Started {
        /** The session was saved as a new workout. */
        NEW,

        /** A workout was already in progress, so it was kept and should be resumed. */
        RESUMING,
    }
}
