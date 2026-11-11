package app.levitx.phone.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.coach.Effort
import app.levitx.core.coach.SessionResult
import app.levitx.core.coach.WorkoutRun
import app.levitx.core.coach.bodyweightTrend
import app.levitx.core.load.LoadLimits
import app.levitx.core.session.SetLoad
import app.levitx.core.skills.SkillCatalogue
import app.levitx.data.coach.CoachRepository
import app.levitx.data.coach.WorkoutDraftRepository
import app.levitx.data.equipment.EquipmentRepository
import app.levitx.data.time.TrainingClock
import app.levitx.phone.handlingDataFailures
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Runs the workout in progress: shows each set, records what the person did,
 * times the rest, and hands the finished workout to the coach.
 *
 * The rules for moving through a workout are [WorkoutRun]'s. This class only
 * connects them to the screen and keeps them safe: after every change the run
 * is saved as the draft, so a workout survives the app being killed.
 */
@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val coachRepository: CoachRepository,
    private val drafts: WorkoutDraftRepository,
    private val equipmentRepository: EquipmentRepository,
    private val clock: TrainingClock,
) : ViewModel() {
    private val state = MutableStateFlow<WorkoutUiState>(WorkoutUiState.Loading)

    /** The current screen state. */
    val uiState: StateFlow<WorkoutUiState> = state.asStateFlow()

    /** Draft writes go through this one at a time, in order, so an older run can't overwrite a newer one. */
    private val writes = Mutex()
    private var gateBestsBefore: Map<String, Int> = emptyMap()

    init {
        viewModelScope.launch {
            handlingDataFailures(onFailure = { state.value = WorkoutUiState.Unavailable }) {
                val run = drafts.observeDraft().first()
                val coach = coachRepository.observeState().first()
                if (run == null || run.isComplete || coach == null) {
                    state.value = WorkoutUiState.NoWorkout
                } else {
                    gateBestsBefore = coach.model.gateBests
                    // Read once, so every set in the workout uses the same figure.
                    val bodyweight = bodyweightTrend(coach.model.weighIns, clock.currentTimeMillis())?.currentKg
                    state.value = WorkoutUiState.Active(run, bodyweight, equipmentRepository.observeBands().first())
                }
            }
        }
    }

    /** Milliseconds on the rest clock, for the screen's countdown. */
    fun elapsedRealtimeMillis(): Long = clock.elapsedRealtimeMillis()

    /**
     * Records the set just done. Starts rest, or saves the workout if it was the last set.
     *
     * @param value Reps or seconds done.
     * @param effort How it felt, or null for holds.
     * @param addedKg Vest or belt weight, 0 for none.
     * @param assistanceKg Band help, 0 for none.
     */
    fun logSet(value: Int, effort: Effort?, addedKg: Double = 0.0, assistanceKg: Double = 0.0) {
        val active = state.value as? WorkoutUiState.Active ?: return
        val next = active.run
            .withLoad(SetLoad(addedKg = addedKg.asModifierKg(), assistanceKg = assistanceKg.asModifierKg()))
            .logSet(value, effort?.repsInReserve, nowElapsedMillis = clock.elapsedRealtimeMillis())
        // A run that didn't change means the set wasn't accepted (during rest, say).
        if (next.logged == active.run.logged) return
        advance(active, next)
    }

    /** Ends rest early, or after it runs out. */
    fun endRest() {
        val active = state.value as? WorkoutUiState.Active ?: return
        advance(active, active.run.endRest())
    }

    /** Moves on to the next exercise. Skipped if nothing was done, ended early if some was. */
    fun skipExercise() {
        val active = state.value as? WorkoutUiState.Active ?: return
        advance(active, active.run.skipExercise())
    }

    /** Ends the workout now and saves what was done. With nothing done, it is thrown away. */
    fun finish() {
        val active = state.value as? WorkoutUiState.Active ?: return
        finish(active.run)
    }

    private fun advance(active: WorkoutUiState.Active, next: WorkoutRun) {
        if (next.isComplete) {
            finish(next)
        } else {
            state.value = active.copy(run = next)
            viewModelScope.launch {
                writes.withLock {
                    handlingDataFailures(onFailure = { state.value = WorkoutUiState.Unavailable }) { drafts.save(next) }
                }
            }
        }
    }

    private fun finish(run: WorkoutRun) {
        val result = run.result(finishedAtEpochMillis = clock.currentTimeMillis())
        // Shown straight away, so a second tap can't save the workout twice.
        state.value = WorkoutUiState.Saving
        viewModelScope.launch {
            // After every draft write already queued, so none of them can bring the draft back.
            writes.withLock {
                handlingDataFailures(onFailure = { state.value = WorkoutUiState.Unavailable }) {
                    if (result == null) {
                        drafts.discard()
                        state.value = WorkoutUiState.NoWorkout
                    } else {
                        coachRepository.recordWorkout(run.id, result)
                        state.value = WorkoutUiState.Finished(
                            sets = result.results.sumOf { it.sets.size },
                            newBest = newBest(result),
                        )
                    }
                }
            }
        }
    }

    /** The biggest step past a gate's previous best in [result], if there was one. */
    private fun newBest(result: SessionResult): NewBest? = result.results
        .mapNotNull { exercise ->
            val gate = exercise.plan.gateId?.let(SkillCatalogue::gateById) ?: return@mapNotNull null
            val best = exercise.sets.filter { it.countsTowardGate }.maxOfOrNull { it.value } ?: 0
            NewBest(gate.title, gate.measure, best).takeIf { best > (gateBestsBefore[gate.id] ?: 0) }
        }
        .maxByOrNull { it.value }
}

/** A vest, belt or band figure the load rules accept: never negative, never above the limit, never NaN. */
private fun Double.asModifierKg(): Double = if (isFinite()) coerceIn(0.0, LoadLimits.MAX_MODIFIER_KG) else 0.0
