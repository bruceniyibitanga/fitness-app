package app.levitx.wear.session

import app.levitx.core.session.SessionRun
import app.levitx.core.session.SetLoad
import app.levitx.core.session.trainingDay
import app.levitx.core.skills.Gate
import app.levitx.data.ApplicationScope
import app.levitx.data.DataUnavailableException
import app.levitx.data.goal.GoalRepository
import app.levitx.data.progress.ProgressRepository
import app.levitx.data.session.SessionDraftRepository
import app.levitx.data.session.SessionOutbox
import app.levitx.data.session.StoredSession
import app.levitx.data.time.TrainingClock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val MILLIS_PER_SECOND = 1_000L

/** Where the watch is with a session. */
sealed interface WorkoutState {
    /** Checking for a session left in progress. */
    data object Loading : WorkoutState

    /** The person's data couldn't be unlocked on this device. */
    data object Unavailable : WorkoutState

    /**
     * No session in progress.
     *
     * @property load The load the next session will start with.
     */
    data class Idle(val load: SetLoad = SetLoad.None) : WorkoutState

    /** A session was asked for and is being planned. */
    data object Starting : WorkoutState

    /**
     * A session is in progress.
     *
     * @property run The session so far.
     */
    data class Running(val run: SessionRun) : WorkoutState

    /**
     * The session is saved and queued for the phone.
     *
     * @property gate The gate trained.
     * @property previousBest Best before this session.
     * @property sessionBest Best set in this session that counts toward the gate.
     * @property usedBand True if any set was band-assisted. Those sets don't count toward the gate.
     */
    data class Finished(val gate: Gate, val previousBest: Int, val sessionBest: Int, val usedBand: Boolean) :
        WorkoutState {
        /** True when this session set a new best. */
        val isNewBest: Boolean
            get() = sessionBest > previousBest

        /** True when this session passed the gate. */
        val passedGate: Boolean
            get() = sessionBest >= gate.target
    }
}

/**
 * Owns the session in progress on the watch.
 *
 * It lives as long as the app process rather than one screen, which is what
 * the Wear OS workout guidance asks for: someone can swipe back to the watch
 * face mid-rest and return to the same set. Every change is also saved to the
 * encrypted database, so the session survives the app being killed or
 * crashing between sets.
 *
 * Actions return straight away and run one at a time on [scope], so a tap
 * never waits on the database and a save is never cut short by a screen
 * closing. Screens watch [state].
 */
@Singleton
class WorkoutController @Inject constructor(
    private val goalRepository: GoalRepository,
    private val progressRepository: ProgressRepository,
    private val draftRepository: SessionDraftRepository,
    private val outbox: SessionOutbox,
    private val clock: TrainingClock,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow<WorkoutState>(WorkoutState.Loading)
    private val mutex = Mutex()

    /** The load the next session starts with. Kept between sessions: most people train with the same setup. */
    private var nextLoad = SetLoad.None

    /** What the watch is doing now. */
    val state: StateFlow<WorkoutState> = mutableState.asStateFlow()

    init {
        // Pick up a session left in progress when the app was last closed or killed.
        act {
            val draft = draftRepository.load()
            when {
                draft == null -> mutableState.value = WorkoutState.Idle(nextLoad)

                // Every set was logged but the app died before the session was saved.
                draft.isComplete -> finish(draft)

                else -> save(draft.withSaneRest(clock.elapsedRealtimeMillis()))
            }
        }
    }

    /** Sets the vest, belt or band for the next session. Ignored while a session is running. */
    fun setLoad(load: SetLoad) = act {
        if (mutableState.value is WorkoutState.Idle) {
            nextLoad = load
            mutableState.value = WorkoutState.Idle(load)
        }
    }

    /** Starts today's session. If one is already in progress, it carries on instead. */
    fun start() {
        // Shown at once, so the session screen never flashes "no session" while the plan is read.
        mutableState.update { current ->
            if (current is WorkoutState.Idle || current is WorkoutState.Finished) WorkoutState.Starting else current
        }
        act {
            val current = mutableState.value
            if (current !is WorkoutState.Running && current !is WorkoutState.Unavailable) startRun()
        }
    }

    /**
     * Records the set just done and starts rest, or saves the session if it
     * was the last set.
     *
     * @param value Reps or seconds done.
     */
    fun logSet(value: Int) = act {
        val running = mutableState.value as? WorkoutState.Running ?: return@act
        // Reps in reserve aren't asked for on the watch: one more question on a
        // small screen between sets isn't worth it. Null means "didn't say".
        val next = running.run.logSet(value, repsInReserve = null, nowElapsedMillis = clock.elapsedRealtimeMillis())
        if (next.isComplete) finish(next) else save(next)
    }

    /** Ends rest early, or after it runs out. */
    fun endRest() = act {
        val running = mutableState.value as? WorkoutState.Running ?: return@act
        if (running.run.isResting) save(running.run.endRest())
    }

    /** Ends the session now and saves the sets done. With none done, the session is simply dropped. */
    fun finishEarly() = act {
        val running = mutableState.value as? WorkoutState.Running ?: return@act
        finish(running.run)
    }

    /** Leaves the summary. */
    fun dismissSummary() = act {
        if (mutableState.value is WorkoutState.Finished) mutableState.value = WorkoutState.Idle(nextLoad)
    }

    private suspend fun startRun() {
        val day = combine(goalRepository.observeActiveGoal(), progressRepository.observeBests()) { path, bests ->
            path?.trainingDay(bests)
        }.first()
        val plan = day?.plan
        if (plan == null) {
            mutableState.value = WorkoutState.Idle(nextLoad)
            return
        }
        save(
            SessionRun(
                id = UUID.randomUUID().toString(),
                plan = plan,
                previousBest = day.currentBest,
                startedAtEpochMillis = clock.currentTimeMillis(),
                // Holds have no load to track, so a load chosen for another gate doesn't apply.
                load = if (plan.gate.loadProfile != null) nextLoad else SetLoad.None,
            ),
        )
    }

    private suspend fun save(run: SessionRun) {
        // The screen updates first; the write follows a moment later.
        mutableState.value = WorkoutState.Running(run)
        draftRepository.save(run)
    }

    private suspend fun finish(run: SessionRun) {
        val session = run.complete(finishedAtEpochMillis = clock.currentTimeMillis())
        // Saving under the run's id means doing this twice (after a crash, say) stores one session.
        if (session != null) outbox.saveAndQueue(StoredSession(run.id, session))
        draftRepository.clear()
        mutableState.value = if (session == null) {
            WorkoutState.Idle(nextLoad)
        } else {
            WorkoutState.Finished(
                gate = run.plan.gate,
                previousBest = run.previousBest,
                sessionBest = session.best,
                usedBand = session.sets.any { !it.countsTowardGate },
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
 * Rest is timed on the clock that counts from boot. After a restart that
 * clock starts again, so a saved rest could look hours long. Rest can never
 * be longer than the plan's, so anything longer is ended.
 */
private fun SessionRun.withSaneRest(nowElapsedMillis: Long): SessionRun {
    val endsAt = restEndsAtElapsedMillis ?: return this
    val longestPossible = nowElapsedMillis + plan.restSeconds * MILLIS_PER_SECOND
    return if (endsAt > longestPossible) endRest() else this
}
