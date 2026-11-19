package app.levitx.wear

import app.levitx.core.coach.PlannedSession
import app.levitx.core.intervals.IntervalCue
import app.levitx.core.session.SessionRun
import app.levitx.core.skills.SkillCatalogue
import app.levitx.data.coach.WorkoutDraftRepository
import app.levitx.testing.FakeIntervalOutbox
import app.levitx.testing.FakeNextSessionRepository
import app.levitx.testing.FakeProgressRepository
import app.levitx.testing.FakeSessionDraftRepository
import app.levitx.testing.FakeSessionRepository
import app.levitx.testing.FakeTrainingClock
import app.levitx.testing.FakeWorkoutDraftRepository
import app.levitx.testing.FakeWorkoutOutbox
import app.levitx.testing.skillSession
import app.levitx.wear.home.PendingWorkouts
import app.levitx.wear.intervals.IntervalController
import app.levitx.wear.intervals.IntervalSignals
import app.levitx.wear.session.LeftoverGateSession
import app.levitx.wear.session.NextSession
import app.levitx.wear.session.WorkoutController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/**
 * The watch's storage, in memory, with a session from the phone's coach
 * waiting: dips (gate 2 of the muscle-up path), then squats.
 *
 * @param legacyDraft A gate session left in progress by an older version of the app.
 * @property progress The watch's copy of the gate bests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class WatchFakes(
    session: PlannedSession? = skillSession(SkillCatalogue.muscleUp.gates[1]),
    val progress: FakeProgressRepository = FakeProgressRepository(mapOf(SkillCatalogue.muscleUp.gates[1].id to 6)),
    legacyDraft: SessionRun? = null,
) {
    /** The coach's next session, as sync stores it. */
    val next = FakeNextSessionRepository(session)

    /** The workout in progress. */
    val drafts = FakeWorkoutDraftRepository()

    /** Finished workouts on their way to the phone. */
    val outbox = FakeWorkoutOutbox(progress, drafts, next)

    /** Gate sessions left by an older version of the app. */
    val legacyDrafts = FakeSessionDraftRepository(legacyDraft)

    /** Where those go. */
    val legacySessions = FakeSessionRepository(progress)

    /** The clocks. */
    val clock = FakeTrainingClock()

    /** Which of the plan's sessions applies now. */
    val nextSession = NextSession(next, clock)

    /** Cardio and mobility workouts on their way to the phone. */
    val intervalOutbox = FakeIntervalOutbox()

    /** The buzzes the interval timer gave, and whether it is holding the watch awake. */
    val signals = RecordingIntervalSignals()

    /** Workouts of every kind waiting for the phone. */
    val pending = PendingWorkouts(outbox, intervalOutbox)

    /** An interval timer on these fakes. Its actions run unconfined, like [controller]'s. */
    fun intervals() = IntervalController(intervalOutbox, clock, signals, CoroutineScope(UnconfinedTestDispatcher()))

    /**
     * A controller on these fakes. Its actions run unconfined, so each has
     * finished by the time the call returns.
     */
    fun controller(draftRepository: WorkoutDraftRepository = drafts) = WorkoutController(
        nextSession,
        draftRepository,
        outbox,
        LeftoverGateSession(legacyDrafts, legacySessions, clock),
        clock,
        CoroutineScope(UnconfinedTestDispatcher()),
    )
}

/** [IntervalSignals] that remembers what it was asked to do. */
internal class RecordingIntervalSignals : IntervalSignals {
    /** Every cue played, in order. */
    val played = mutableListOf<IntervalCue>()

    /** True while the timer is holding the watch awake. */
    var isAwake = false
        private set

    override fun play(cue: IntervalCue) {
        played += cue
    }

    override fun stayAwakeFor(millis: Long) {
        isAwake = true
    }

    override fun letSleep() {
        isAwake = false
    }
}
