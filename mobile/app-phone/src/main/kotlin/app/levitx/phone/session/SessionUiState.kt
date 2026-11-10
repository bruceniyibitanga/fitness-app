package app.levitx.phone.session

import app.levitx.core.session.LoggedSet
import app.levitx.core.session.PlannedSet
import app.levitx.core.session.SessionPlan
import app.levitx.core.skills.Gate

/** What the session screen shows. */
sealed interface SessionUiState {
    /** Working out today's plan. */
    data object Loading : SessionUiState

    /** Data couldn't be unlocked. */
    data object Unavailable : SessionUiState

    /** No goal, or the path is complete: nothing to train. */
    data object NothingToTrain : SessionUiState

    /**
     * A session in progress.
     *
     * @property plan Today's plan.
     * @property setIndex 0-based index of the set to do next.
     * @property logged Sets done so far.
     * @property restEndsAtElapsedMillis When rest before the next set ends, on the
     *   [TrainingClock.elapsedRealtimeMillis] clock; null when not resting.
     */
    data class Active(
        val plan: SessionPlan,
        val setIndex: Int,
        val logged: List<LoggedSet>,
        val restEndsAtElapsedMillis: Long?,
    ) : SessionUiState {
        /** The set to do next. */
        val nextSet: PlannedSet
            get() = plan.sets[setIndex]
    }

    /**
     * The session is saved.
     *
     * @property gate The gate trained.
     * @property previousBest Best before this session.
     * @property sessionBest Best set in this session.
     */
    data class Finished(val gate: Gate, val previousBest: Int, val sessionBest: Int) : SessionUiState {
        /** True when this session set a new best. */
        val isNewBest: Boolean
            get() = sessionBest > previousBest

        /** True when this session passed the gate. */
        val passedGate: Boolean
            get() = sessionBest >= gate.target
    }
}
