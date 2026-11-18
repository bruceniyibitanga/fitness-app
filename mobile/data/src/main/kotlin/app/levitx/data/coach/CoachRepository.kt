package app.levitx.data.coach

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.BodyArea
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.QuickTest
import app.levitx.core.coach.SessionResult
import app.levitx.core.pain.PainAssessment
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow

/**
 * Everything stored for the coach.
 *
 * @property model What the coach knows about the person, with their bests and
 *   bodyweight readings filled in from the tables the rest of the app uses.
 * @property trainingDays The days the person said they'd train. Used for
 *   reminders and the week view; the plan itself rolls from session to session.
 */
data class CoachState(val model: AthleteModel, val trainingDays: Set<DayOfWeek>)

/**
 * Stores what the coach knows and records what happens to it.
 *
 * Every change is a read, a call to `Coach` and a write in one transaction, so
 * two changes at once can't overwrite each other. The UI never writes the
 * model directly.
 */
interface CoachRepository {
    /** Emits the coach's state straight away and whenever it changes. Null before setup. */
    fun observeState(): Flow<CoachState?>

    /**
     * Sets the coach up from the setup answers. Bests already recorded on the
     * goal's gates are kept. Calling it again starts the coach over, keeping
     * bests and bodyweight.
     *
     * @param tested Results of the optional quick test, as reps per test. Empty when it was skipped.
     */
    suspend fun start(
        profile: CoachProfile,
        pullUp: PullUpAnswer,
        trainingDays: Set<DayOfWeek>,
        tested: Map<QuickTest, Int> = emptyMap(),
    )

    /** Changes the setup answers without forgetting anything learned. */
    suspend fun updateProfile(profile: CoachProfile, trainingDays: Set<DayOfWeek>)

    /**
     * Records a finished workout: its sets, any new gate bests and what the
     * coach learns, then clears the workout in progress. Safe to call again with
     * the same [workoutId]: nothing changes.
     *
     * A result dated after this device's clock is recorded as finishing now,
     * keeping its length: the device that timed it had its clock ahead.
     *
     * @return What happened. [Recorded.NOT_SET_UP] leaves everything as it
     *   was, so the caller can keep the workout and try again after setup.
     */
    suspend fun recordWorkout(workoutId: String, result: SessionResult): Recorded

    /**
     * Ids of the workouts recorded most recently, newest first, at most
     * [RECENT_WORKOUTS]. The watch uses it to tell when the phone's plan
     * already takes in a workout it finished.
     */
    fun observeRecentWorkoutIds(): Flow<List<String>>

    /**
     * Records that the person took the rest day the coach offered, at
     * [nowEpochMillis]. This and the other small updates below do nothing
     * before setup, or after everything was deleted: there is nothing to update.
     */
    suspend fun recordRest(nowEpochMillis: Long)

    /** Records the advice from a pain check-in for [area]. */
    suspend fun recordPain(area: BodyArea, assessment: PainAssessment)

    /**
     * Records that [prompt] was shown at [atEpochMillis], so it isn't shown
     * again until its own rule says so.
     *
     * @throws app.levitx.data.DataUnavailableException if the data can't be read or written.
     */
    suspend fun markShown(prompt: CoachPrompt, atEpochMillis: Long)

    /**
     * Deletes a finished workout and every set in it, and takes it back out
     * of what the coach knows: the week's sets drop and any best it set goes
     * back to the best of the workouts left. Targets the coach moved since
     * stay, and settle again over the next sessions.
     *
     * @return True when there was such a workout.
     */
    suspend fun deleteWorkout(workoutId: String): Boolean
}

/** What [CoachRepository.recordWorkout] did with a workout. */
enum class Recorded {
    /** It was new, and is now stored. */
    NEW,

    /** It was stored before. Nothing changed. */
    ALREADY_STORED,

    /** The coach isn't set up (yet, or any more), so it couldn't be recorded. Nothing changed. */
    NOT_SET_UP,
}

/** How many recent workout ids [CoachRepository.observeRecentWorkoutIds] reports. */
const val RECENT_WORKOUTS = 10

/** Things the coach says or asks once, then remembers it did. */
enum class CoachPrompt {
    /** The fast weight loss notice: once per spell of fast loss. */
    WEIGHT_NOTICE,

    /** The quick weigh-in before a session: at most once a week, saved or skipped. */
    WEIGH_IN,

    /** The week review: once a week, when the person opens it. */
    WEEK_REVIEW,
}

/** [this] with [prompt] noted as shown at [atEpochMillis]. */
fun AthleteModel.withShown(prompt: CoachPrompt, atEpochMillis: Long): AthleteModel = when (prompt) {
    CoachPrompt.WEIGHT_NOTICE -> withWeightNoticeShown()
    CoachPrompt.WEIGH_IN -> withWeighInAsked(atEpochMillis)
    CoachPrompt.WEEK_REVIEW -> withWeekReviewed(atEpochMillis)
}
