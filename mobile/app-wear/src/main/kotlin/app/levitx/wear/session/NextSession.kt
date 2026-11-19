package app.levitx.wear.session

import app.levitx.core.coach.PlannedSession
import app.levitx.data.coach.NextSessionRepository
import app.levitx.data.time.TrainingClock
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

/**
 * The session the person would do if they started on the watch right now.
 *
 * The phone sends a plan with two sessions: one for now and one for once
 * everything trained recently has rested (see `PlanAhead`). Which applies
 * depends on the time, so every part of the watch that shows or starts the
 * next session asks here, and they can't disagree.
 */
class NextSession @Inject constructor(
    private val repository: NextSessionRepository,
    private val clock: TrainingClock,
) {
    /**
     * The session for now, or null when the watch has none. Emits straight
     * away, when the phone sends a new plan, and once more when the rested
     * session takes over, so a screen left open doesn't keep showing the
     * light one.
     */
    fun observe(): Flow<PlannedSession?> = channelFlow {
        // A newer plan cancels the wait for the older one's rested session.
        repository.observeNext().collectLatest { plan ->
            val now = clock.currentTimeMillis()
            send(plan?.at(now))
            val restedAt = plan?.restedAtEpochMillis
            if (restedAt != null && restedAt > now) {
                delay(restedAt - now)
                send(plan.at(clock.currentTimeMillis()))
            }
        }
    }.distinctUntilChanged()

    /** The session to start this moment, read fresh from storage, or null when there is none. */
    suspend fun current(): PlannedSession? = repository.observeNext().first()?.at(clock.currentTimeMillis())
}
