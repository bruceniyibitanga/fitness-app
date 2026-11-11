package app.levitx.testing

import app.levitx.core.coach.PlannedSession
import app.levitx.data.coach.NextSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory [NextSessionRepository]. */
class FakeNextSessionRepository(initial: PlannedSession? = null) : NextSessionRepository {
    private val next = MutableStateFlow(initial)
    private var awaiting: String? = null

    /** The next session, for assertions. */
    val current: PlannedSession?
        get() = next.value

    /** The workout the next session waits on, for assertions. */
    val awaitingId: String?
        get() = awaiting

    override fun observeNext(): Flow<PlannedSession?> = next.asStateFlow()

    override suspend fun awaitingWorkoutId(): String? = awaiting

    override suspend fun replace(session: PlannedSession?) {
        next.value = session
        awaiting = null
    }

    /** What [app.levitx.data.coach.WorkoutOutbox.finishAndQueue] does to the next session. */
    fun awaitPhone(workoutId: String) {
        next.value = null
        awaiting = workoutId
    }
}
