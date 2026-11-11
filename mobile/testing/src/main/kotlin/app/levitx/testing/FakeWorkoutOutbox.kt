package app.levitx.testing

import app.levitx.core.coach.WorkoutRun
import app.levitx.data.coach.QueuedWorkout
import app.levitx.data.coach.WorkoutOutbox
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory [WorkoutOutbox] that does what the real one does in its
 * transaction: raises [progress]'s bests, clears [drafts], and has [next]
 * wait for the phone.
 */
class FakeWorkoutOutbox(
    private val progress: FakeProgressRepository = FakeProgressRepository(),
    private val drafts: FakeWorkoutDraftRepository = FakeWorkoutDraftRepository(),
    private val next: FakeNextSessionRepository = FakeNextSessionRepository(),
) : WorkoutOutbox {
    private val queued = MutableStateFlow<List<QueuedWorkout>>(emptyList())

    /** Workouts still queued, for assertions. */
    val current: List<QueuedWorkout>
        get() = queued.value

    override suspend fun finishAndQueue(run: WorkoutRun, finishedAtEpochMillis: Long): Map<String, Int> {
        val result = run.result(finishedAtEpochMillis)
        var raised = emptyMap<String, Int>()
        if (result != null && queued.value.none { it.run.id == run.id }) {
            queued.update { it + QueuedWorkout(run, result.finishedAtEpochMillis) }
            raised = result.newGateBests(progress.current)
            raised.forEach { (gateId, best) -> progress.recordBest(gateId, best) }
            next.awaitPhone(run.id)
        }
        drafts.discard()
        return raised
    }

    override fun observeQueued(): Flow<List<QueuedWorkout>> = queued.asStateFlow()

    override suspend fun markDelivered(workoutId: String) {
        queued.update { workouts -> workouts.filterNot { it.run.id == workoutId } }
    }
}
