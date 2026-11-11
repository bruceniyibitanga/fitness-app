package app.levitx.phone.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.levitx.core.load.LoadLimits
import app.levitx.core.session.LoggedSet
import app.levitx.core.session.SessionRun
import app.levitx.core.session.SetLoad
import app.levitx.core.session.TrainingDay
import app.levitx.core.session.trainingDay
import app.levitx.data.DataUnavailableException
import app.levitx.data.equipment.Band
import app.levitx.data.equipment.EquipmentRepository
import app.levitx.data.goal.GoalRepository
import app.levitx.data.progress.ProgressRepository
import app.levitx.data.session.SessionRepository
import app.levitx.data.time.TrainingClock
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val KEY_ID = "id"
private const val KEY_STARTED_AT = "startedAt"
private const val KEY_VALUES = "values"
private const val KEY_RESERVES = "reserves"
private const val KEY_REST_ENDS_AT = "restEndsAt"
private const val KEY_ADDED = "added"
private const val KEY_ASSISTANCE = "assistance"

/** Stored in place of a null reps-in-reserve, since saved state holds plain int arrays. */
private const val NO_RESERVE = -1
private const val NOT_RESTING = -1L

/**
 * Runs one session: shows each set, records what the person did (including
 * any vest, belt or band), times the rest in between, then saves the whole
 * session in one go.
 *
 * The rules for moving through a session (what a logged set looks like, when
 * rest starts, what gets saved) are [SessionRun]'s, the same ones the watch
 * uses. This class only connects them to the screen and keeps them safe.
 *
 * The plan is fixed when the session starts, so the targets don't move mid-
 * session. The run is copied into [SavedStateHandle] after every change, so a
 * session survives the app being killed in the background between sets.
 */
@HiltViewModel
class SessionViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val progressRepository: ProgressRepository,
    private val sessionRepository: SessionRepository,
    private val equipmentRepository: EquipmentRepository,
    private val clock: TrainingClock,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val state = MutableStateFlow<SessionUiState>(SessionUiState.Loading)

    /** The current screen state. */
    val uiState: StateFlow<SessionUiState> = state.asStateFlow()

    private var run: SessionRun? = null
    private var bodyweightKg: Double? = null
    private var bands: List<Band> = emptyList()

    init {
        viewModelScope.launch {
            try {
                val today = combine(goalRepository.observeActiveGoal(), progressRepository.observeBests()) {
                        path,
                        bests,
                    ->
                    path?.trainingDay(bests)
                }.first()
                val restored = today?.let { restoreRun(it) }
                if (restored == null) {
                    state.value = SessionUiState.NothingToTrain
                } else {
                    // Bodyweight is read once, so every set in the session uses the same figure.
                    bodyweightKg = equipmentRepository.observeBodyweightKg().first()
                    bands = equipmentRepository.observeBands().first()
                    update(restored)
                }
            } catch (error: DataUnavailableException) {
                state.value = SessionUiState.Unavailable
            }
        }
    }

    /** Milliseconds on the rest clock, for the screen's countdown. */
    fun elapsedRealtimeMillis(): Long = clock.elapsedRealtimeMillis()

    /**
     * Records the set just done. Starts rest, or saves the session if it was
     * the last set.
     *
     * @param value Reps or seconds done.
     * @param repsInReserve Reps the person felt they had left, or null for holds.
     * @param addedKg Vest or belt weight, 0 for none.
     * @param assistanceKg Band help, 0 for none.
     */
    fun logSet(value: Int, repsInReserve: Int?, addedKg: Double = 0.0, assistanceKg: Double = 0.0) {
        val current = run ?: return
        if (state.value !is SessionUiState.Active) return
        val next = current
            .withLoad(SetLoad(addedKg = addedKg.asModifierKg(), assistanceKg = assistanceKg.asModifierKg()))
            .logSet(value, repsInReserve, nowElapsedMillis = clock.elapsedRealtimeMillis())
        // A run that didn't change means the set wasn't accepted (during rest, say).
        if (next.logged == current.logged) return
        update(next)
        if (next.isComplete) finish()
    }

    /** Ends rest early, or after it runs out. */
    fun endRest() {
        val current = run ?: return
        if (state.value is SessionUiState.Active) update(current.endRest())
    }

    /** Ends the session now. Saves whatever sets were done; with none, there is nothing to save. */
    fun finish() {
        val current = run ?: return
        if (state.value !is SessionUiState.Active) return
        val session = current.complete(finishedAtEpochMillis = clock.currentTimeMillis(), bodyweightKg = bodyweightKg)
        if (session == null) {
            state.value = SessionUiState.NothingToTrain
            return
        }
        // Shown straight away, so a second tap can't save the session twice.
        state.value = SessionUiState.Loading
        viewModelScope.launch {
            try {
                sessionRepository.saveCompletedSession(session)
                state.value = SessionUiState.Finished(
                    gate = current.plan.gate,
                    previousBest = current.previousBest,
                    sessionBest = session.best,
                    usedBand = session.sets.any { !it.countsTowardGate },
                )
            } catch (error: DataUnavailableException) {
                state.value = SessionUiState.Unavailable
            }
        }
    }

    /** Keeps [next] as the session, copies it to saved state, and shows it. */
    private fun update(next: SessionRun) {
        run = next
        savedState[KEY_ID] = next.id
        savedState[KEY_STARTED_AT] = next.startedAtEpochMillis
        savedState[KEY_VALUES] = next.logged.map { it.value }.toIntArray()
        savedState[KEY_RESERVES] = next.logged.map { it.repsInReserve ?: NO_RESERVE }.toIntArray()
        savedState[KEY_ADDED] = next.logged.map { it.addedKg }.toDoubleArray()
        savedState[KEY_ASSISTANCE] = next.logged.map { it.assistanceKg }.toDoubleArray()
        savedState[KEY_REST_ENDS_AT] = next.restEndsAtElapsedMillis ?: NOT_RESTING
        state.value = SessionUiState.Active(
            plan = next.plan,
            setIndex = next.nextSetIndex,
            logged = next.logged,
            restEndsAtElapsedMillis = next.restEndsAtElapsedMillis,
            bodyweightKg = bodyweightKg,
            bands = bands,
        )
    }

    /**
     * The session in saved state if there is one (the app was killed mid-
     * session), otherwise a new one for today. Null when there is nothing to train.
     */
    private fun restoreRun(day: TrainingDay): SessionRun? {
        val plan = day.plan ?: return null
        val values: IntArray = savedState[KEY_VALUES] ?: IntArray(0)
        val reserves: IntArray = savedState[KEY_RESERVES] ?: IntArray(0)
        val added: DoubleArray = savedState[KEY_ADDED] ?: DoubleArray(0)
        val assistance: DoubleArray = savedState[KEY_ASSISTANCE] ?: DoubleArray(0)
        val logged = values.take(plan.sets.size).mapIndexed { index, value ->
            LoggedSet(
                value = value.coerceAtLeast(0),
                repsInReserve = reserves.getOrNull(index)?.takeIf { it != NO_RESERVE }?.coerceAtLeast(0),
                isBestEffort = plan.sets[index].isBestEffort,
                addedKg = added.getOrElse(index) { 0.0 }.asModifierKg(),
                assistanceKg = assistance.getOrElse(index) { 0.0 }.asModifierKg(),
            )
        }
        return SessionRun(
            id = savedState.get<String>(KEY_ID)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
            plan = plan,
            previousBest = day.currentBest,
            startedAtEpochMillis = savedState[KEY_STARTED_AT] ?: clock.currentTimeMillis(),
            logged = logged,
            restEndsAtElapsedMillis = savedState.get<Long>(KEY_REST_ENDS_AT)?.takeIf { it != NOT_RESTING },
        )
    }
}

/** A vest, belt or band figure the load rules accept: never negative, never above the limit, never NaN. */
private fun Double.asModifierKg(): Double = if (isFinite()) coerceIn(0.0, LoadLimits.MAX_MODIFIER_KG) else 0.0
