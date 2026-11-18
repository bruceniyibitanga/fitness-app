package app.levitx.testing

import app.levitx.core.coach.PlanAhead
import app.levitx.core.coach.PlannedSession
import app.levitx.data.coach.NextSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory [NextSessionRepository].
 *
 * @param initial The session waiting on the watch, as a plan with nothing to switch to later.
 */
class FakeNextSessionRepository(initial: PlannedSession? = null) : NextSessionRepository {
    private val next = MutableStateFlow(initial?.let(::PlanAhead))
    private var awaiting: String? = null

    /** The whole plan, for assertions. */
    val plan: PlanAhead?
        get() = next.value

    /** The session for now, for assertions. */
    val current: PlannedSession?
        get() = next.value?.now

    /** The workout the next session waits on, for assertions. */
    val awaitingId: String?
        get() = awaiting

    override fun observeNext(): Flow<PlanAhead?> = next.asStateFlow()

    override suspend fun awaitingWorkoutId(): String? = awaiting

    override suspend fun replace(plan: PlanAhead?) {
        next.value = plan
        awaiting = null
    }

    /** Puts [session] on the watch as the whole plan, the way most tests want it. */
    suspend fun replace(session: PlannedSession) = replace(PlanAhead(session))

    /** What [app.levitx.data.coach.WorkoutOutbox.finishAndQueue] does to the next session. */
    fun awaitPhone(workoutId: String) {
        next.value = null
        awaiting = workoutId
    }
}
