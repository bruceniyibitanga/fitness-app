package app.levitx.testing

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.BodyArea
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.SessionResult
import app.levitx.core.pain.PainAssessment
import app.levitx.data.coach.CoachRepository
import app.levitx.data.coach.CoachState
import app.levitx.data.coach.RECENT_WORKOUTS
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory [CoachRepository] that learns through the real `Coach`, so tests
 * see real behaviour. Like the real one, recording a workout clears [drafts].
 */
class FakeCoachRepository(
    initial: CoachState? = null,
    private val drafts: FakeWorkoutDraftRepository? = null,
    private val coach: Coach = Coach(),
) : CoachRepository {
    private val state = MutableStateFlow(initial)

    // Newest last, like the order workouts are recorded in.
    private val recorded = MutableStateFlow<List<String>>(emptyList())

    /** The current state, for assertions. */
    val current: CoachState?
        get() = state.value

    /** Workouts recorded so far, by id. */
    val recordedWorkouts: Set<String>
        get() = recorded.value.toSet()

    override fun observeState(): Flow<CoachState?> = state.asStateFlow()

    override suspend fun start(profile: CoachProfile, pullUp: PullUpAnswer, trainingDays: Set<DayOfWeek>) {
        state.value = CoachState(AthleteModel.starting(profile, pullUp), trainingDays)
    }

    override suspend fun updateProfile(profile: CoachProfile, trainingDays: Set<DayOfWeek>) {
        state.value = setUp().let { CoachState(it.model.copy(profile = profile), trainingDays) }
    }

    override suspend fun recordWorkout(workoutId: String, result: SessionResult): Boolean {
        if (workoutId in recorded.value) return false
        recorded.update { it + workoutId }
        update { coach.record(it, result) }
        drafts?.discard()
        return true
    }

    override fun observeRecentWorkoutIds(): Flow<List<String>> = recorded.map { it.reversed().take(RECENT_WORKOUTS) }

    override suspend fun recordRest(nowEpochMillis: Long) = update { coach.recordRest(it, nowEpochMillis) }

    override suspend fun recordPain(area: BodyArea, assessment: PainAssessment) = update {
        it.withPain(area, assessment)
    }

    override suspend fun markWeightNoticeShown() = update { it.withWeightNoticeShown() }

    private fun update(change: (AthleteModel) -> AthleteModel) {
        val current = setUp()
        state.value = current.copy(model = change(current.model))
    }

    private fun setUp(): CoachState = checkNotNull(state.value) { "The coach isn't set up" }
}
