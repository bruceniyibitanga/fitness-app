package app.levitx.testing

import app.levitx.core.coach.WorkoutRun
import app.levitx.data.coach.WorkoutDraftRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory [WorkoutDraftRepository]. */
class FakeWorkoutDraftRepository(initial: WorkoutRun? = null) : WorkoutDraftRepository {
    private val draft = MutableStateFlow(initial)

    /** The workout in progress, for assertions. */
    val current: WorkoutRun?
        get() = draft.value

    override fun observeDraft(): Flow<WorkoutRun?> = draft.asStateFlow()

    override suspend fun save(run: WorkoutRun) {
        draft.value = run
    }

    override suspend fun discard() {
        draft.value = null
    }
}
