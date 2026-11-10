package app.levitx.phone.session

import app.levitx.core.session.LoggedSet
import app.levitx.core.session.PlannedSet
import app.levitx.core.session.SessionPlan
import app.levitx.core.skills.Gate
import app.levitx.data.equipment.Band

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
     * @property bodyweightKg Bodyweight for working out load, or null if not entered.
     * @property bands The person's bands, to choose from for assisted sets.
     */
    data class Active(
        val plan: SessionPlan,
        val setIndex: Int,
        val logged: List<LoggedSet>,
        val restEndsAtElapsedMillis: Long?,
        val bodyweightKg: Double? = null,
        val bands: List<Band> = emptyList(),
    ) : SessionUiState {
        /** The set to do next. */
        val nextSet: PlannedSet
            get() = plan.sets[setIndex]

        /** True when this gate's reps have a load to show and modifiers make sense. */
        val tracksLoad: Boolean
            get() = plan.gate.loadProfile != null
    }

    /**
     * The session is saved.
     *
     * @property gate The gate trained.
     * @property previousBest Best before this session.
     * @property sessionBest Best set in this session that counts toward the gate.
     * @property usedBand True if any set was band-assisted. Those sets don't count toward the gate.
     */
    data class Finished(
        val gate: Gate,
        val previousBest: Int,
        val sessionBest: Int,
        val usedBand: Boolean = false,
    ) : SessionUiState {
        /** True when this session set a new best. */
        val isNewBest: Boolean
            get() = sessionBest > previousBest

        /** True when this session passed the gate. */
        val passedGate: Boolean
            get() = sessionBest >= gate.target
    }
}
