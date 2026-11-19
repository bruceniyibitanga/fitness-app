package app.levitx.testing

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.BodyArea
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.QuickTest
import app.levitx.core.coach.SessionResult
import app.levitx.core.pain.PainAssessment
import app.levitx.data.coach.CoachPrompt
import app.levitx.data.coach.CoachRepository
import app.levitx.data.coach.CoachState
import app.levitx.data.coach.RECENT_WORKOUTS
import app.levitx.data.coach.Recorded
import app.levitx.data.coach.withShown
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

    /** Empties the coach, as "delete everything" empties the real one. */
    fun deleteEverything() {
        state.value = null
        recorded.value = emptyList()
    }

    override fun observeState(): Flow<CoachState?> = state.asStateFlow()

    override suspend fun start(
        profile: CoachProfile,
        pullUp: PullUpAnswer,
        trainingDays: Set<DayOfWeek>,
        tested: Map<QuickTest, Int>,
    ) {
        state.value = CoachState(AthleteModel.starting(profile, pullUp, tested), trainingDays)
    }

    override suspend fun updateProfile(profile: CoachProfile, trainingDays: Set<DayOfWeek>) {
        val current = checkNotNull(state.value) { "The coach isn't set up" }
        state.value = CoachState(current.model.copy(profile = profile), trainingDays)
    }

    override suspend fun recordWorkout(workoutId: String, result: SessionResult): Recorded = when {
        workoutId in recorded.value -> Recorded.ALREADY_STORED

        state.value == null -> Recorded.NOT_SET_UP

        else -> {
            recorded.update { it + workoutId }
            state.changeModel { coach.record(it, result) }
            drafts?.discard()
            Recorded.NEW
        }
    }

    override fun observeRecentWorkoutIds(): Flow<List<String>> = recorded.map { it.reversed().take(RECENT_WORKOUTS) }

    override suspend fun recordRest(nowEpochMillis: Long) = state.changeModel { coach.recordRest(it, nowEpochMillis) }

    override suspend fun recordPain(area: BodyArea, assessment: PainAssessment) = state.changeModel {
        it.withPain(area, assessment)
    }

    override suspend fun markShown(prompt: CoachPrompt, atEpochMillis: Long) =
        state.changeModel { it.withShown(prompt, atEpochMillis) }

    // Forgets the id only: the fake keeps no sets to take back out of the model.
    override suspend fun deleteWorkout(workoutId: String): Boolean {
        val known = workoutId in recorded.value
        recorded.update { it - workoutId }
        return known
    }
}

/** Changes the model with [change]. Like the real repository, does nothing before setup. */
private fun MutableStateFlow<CoachState?>.changeModel(change: (AthleteModel) -> AthleteModel) {
    update { current -> current?.copy(model = change(current.model)) }
}
