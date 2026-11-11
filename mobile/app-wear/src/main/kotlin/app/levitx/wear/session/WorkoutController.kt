package app.levitx.wear.session

import app.levitx.core.coach.ExercisePlan
import app.levitx.core.coach.WorkoutRun
import app.levitx.core.session.SetLoad
import app.levitx.core.skills.GateMeasure
import app.levitx.core.skills.SkillCatalogue
import app.levitx.data.ApplicationScope
import app.levitx.data.DataUnavailableException
import app.levitx.data.coach.NextSessionRepository
import app.levitx.data.coach.WorkoutDraftRepository
import app.levitx.data.coach.WorkoutOutbox
import app.levitx.data.time.TrainingClock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Where the watch is with a workout. */
sealed interface WorkoutState {
    /** Checking for a workout left in progress. */
    data object Loading : WorkoutState

    /** The person's data couldn't be unlocked on this device. */
    data object Unavailable : WorkoutState

    /**
     * No workout in progress.
     *
     * @property load The load the skill work of the next workout will use.
     */
    data class Idle(val load: SetLoad = SetLoad.None) : WorkoutState

    /** A workout was asked for and is being set up. */
    data object Starting : WorkoutState

    /**
     * A workout is in progress.
     *
     * @property run The workout so far. Never complete: a complete one is saved straight away.
     */
    data class Running(val run: WorkoutRun) : WorkoutState

    /**
     * The workout is saved and queued for the phone.
     *
     * @property sets Sets done.
     * @property newBest The biggest new best on a skill gate, if there was one.
     * @property usedBand True if any set was band-assisted. Those sets don't count toward a gate.
     */
    data class Finished(val sets: Int, val newBest: GateBest? = null, val usedBand: Boolean = false) : WorkoutState
}

/**
 * A new best on a skill gate.
 *
 * @property gateTitle The gate, as the phone's path screen names it.
 * @property measure Reps or seconds.
 * @property value The new best.
 * @property passedGate True when it reaches the gate's target.
 */
data class GateBest(val gateTitle: String, val measure: GateMeasure, val value: Int, val passedGate: Boolean)

/**
 * Owns the workout in progress on the watch.
 *
 * The workout is the one the phone's coach planned (see
 * [NextSessionRepository]); the rules for moving through it are
 * [WorkoutRun]'s, the same ones the phone uses. This class connects them to
 * the screens and keeps them safe.
 *
 * It lives as long as the app process rather than one screen, which is what
 * the Wear OS workout guidance asks for: someone can swipe back to the watch
 * face mid-rest and return to the same set. Every change is also saved to the
 * encrypted database, so the workout survives the app being killed or
 * crashing between sets.
 *
 * Actions return straight away and run one at a time on [scope], so a tap
 * never waits on the database and a save is never cut short by a screen
 * closing. Screens watch [state].
 */
@Singleton
class WorkoutController @Inject constructor(
    private val nextSessionRepository: NextSessionRepository,
    private val drafts: WorkoutDraftRepository,
    private val workoutOutbox: WorkoutOutbox,
    private val leftoverGateSession: LeftoverGateSession,
    private val clock: TrainingClock,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow<WorkoutState>(WorkoutState.Loading)
    private val mutex = Mutex()

    /**
     * The load for the skill work of the next workout. Kept between workouts:
     * most people train with the same setup.
     */
    private var nextLoad = SetLoad.None

    /** What the watch is doing now. */
    val state: StateFlow<WorkoutState> = mutableState.asStateFlow()

    /** The workout in progress, or null. */
    private val running: WorkoutRun?
        get() = (mutableState.value as? WorkoutState.Running)?.run

    init {
        act {
            leftoverGateSession.save()
            // Pick up a workout left in progress when the app was last closed or killed.
            val draft = drafts.observeDraft().first()
            if (draft == null) {
                mutableState.value = WorkoutState.Idle(nextLoad)
            } else {
                // A complete draft means every set was logged but the app died before
                // the workout was saved. keep() saves it now.
                keep(draft.withSaneRest(clock.elapsedRealtimeMillis()))
            }
        }
    }

    /** Sets the vest, belt or band for the next workout's skill work. Ignored while a workout is running. */
    fun setLoad(load: SetLoad) = act {
        if (mutableState.value is WorkoutState.Idle) {
            nextLoad = load
            mutableState.value = WorkoutState.Idle(load)
        }
    }

    /** Starts the next session the coach planned. If a workout is already in progress, it carries on instead. */
    fun start() {
        // Shown at once, so the workout screen never flashes "no workout" while the plan is read.
        mutableState.update { current ->
            if (current is WorkoutState.Idle || current is WorkoutState.Finished) WorkoutState.Starting else current
        }
        act {
            val current = mutableState.value
            if (current is WorkoutState.Running || current is WorkoutState.Unavailable) return@act
            val session = nextSessionRepository.observeNext().first()
            if (session == null) {
                mutableState.value = WorkoutState.Idle(nextLoad)
            } else {
                keep(
                    WorkoutRun(UUID.randomUUID().toString(), session, startedAtEpochMillis = clock.currentTimeMillis()),
                )
            }
        }
    }

    /**
     * Records the set just done and starts rest, or saves the workout if it
     * was the last set.
     *
     * @param value Reps or seconds done.
     */
    fun logSet(value: Int) = act {
        val run = running ?: return@act
        val plan = run.current ?: return@act
        // Reps in reserve aren't asked for on the watch: one more question on a
        // small screen between sets isn't worth it. Null means "didn't say".
        val next = run.withLoad(plan.loadWith(nextLoad))
            .logSet(value, repsInReserve = null, nowElapsedMillis = clock.elapsedRealtimeMillis())
        // A run that didn't change means the set wasn't accepted (during rest, say).
        if (next.logged != run.logged) keep(next)
    }

    /** Ends rest early, or after it runs out. */
    fun endRest() = act {
        val run = running ?: return@act
        if (run.isResting) keep(run.endRest())
    }

    /** Moves on to the next exercise. Skipped if nothing was done, ended early if some was. */
    fun skipExercise() = act {
        val run = running ?: return@act
        keep(run.skipExercise())
    }

    /** Ends the workout now and saves the sets done. With none done, the workout is simply dropped. */
    fun finishEarly() = act {
        val run = running ?: return@act
        finish(run)
    }

    /** Leaves the summary. */
    fun dismissSummary() = act {
        if (mutableState.value is WorkoutState.Finished) mutableState.value = WorkoutState.Idle(nextLoad)
    }

    /** Saves [run] as the workout in progress, or the finished workout if it is complete. */
    private suspend fun keep(run: WorkoutRun) {
        if (run.isComplete) {
            finish(run)
        } else {
            // The screen updates first; the write follows a moment later.
            mutableState.value = WorkoutState.Running(run)
            drafts.save(run)
        }
    }

    private suspend fun finish(run: WorkoutRun) {
        val finishedAt = clock.currentTimeMillis()
        // One transaction: queued for the phone, bests raised, draft cleared.
        // It is keyed on the run's id, so doing this twice (after a crash, say) queues one workout.
        val raised = workoutOutbox.finishAndQueue(run, finishedAt)
        val result = run.result(finishedAt)
        mutableState.value = if (result == null) {
            WorkoutState.Idle(nextLoad)
        } else {
            WorkoutState.Finished(
                sets = result.results.sumOf { it.sets.size },
                newBest = biggestNewBest(raised),
                usedBand = result.results.any { exercise -> exercise.sets.any { it.assistanceKg > 0.0 } },
            )
        }
    }

    /** Runs [action] after any action already running. Locked data becomes a state, not a crash. */
    private fun act(action: suspend () -> Unit) {
        scope.launch {
            mutex.withLock {
                try {
                    action()
                } catch (error: DataUnavailableException) {
                    mutableState.value = WorkoutState.Unavailable
                }
            }
        }
    }
}

/**
 * The load a set of this plan is logged with, given the [chosen] load. The
 * load chosen on the watch is for the skill work, as it always was: that is
 * what a vest or band is set up for. Other exercises are logged at
 * bodyweight, and holds have no load to track at all.
 */
private fun ExercisePlan.loadWith(chosen: SetLoad): SetLoad =
    if (gateId != null && exercise.loadProfile != null) chosen else SetLoad.None

/** The biggest of the [raised] gate bests, or null when none was raised. */
private fun biggestNewBest(raised: Map<String, Int>): GateBest? = raised
    .mapNotNull { (gateId, best) ->
        SkillCatalogue.gateById(gateId)?.let { GateBest(it.title, it.measure, best, passedGate = best >= it.target) }
    }
    .maxByOrNull { it.value }
