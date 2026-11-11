package app.levitx.data.coach

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.BodyArea
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.PullUpAnswer
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
     */
    suspend fun start(profile: CoachProfile, pullUp: PullUpAnswer, trainingDays: Set<DayOfWeek>)

    /** Changes the setup answers without forgetting anything learned. */
    suspend fun updateProfile(profile: CoachProfile, trainingDays: Set<DayOfWeek>)

    /**
     * Records a finished workout: its sets, any new gate bests and what the
     * coach learns, then clears the workout in progress. Safe to call again with
     * the same [workoutId]: nothing changes.
     *
     * @return True when the workout was new.
     * @throws IllegalStateException before setup.
     */
    suspend fun recordWorkout(workoutId: String, result: SessionResult): Boolean

    /**
     * Ids of the workouts recorded most recently, newest first, at most
     * [RECENT_WORKOUTS]. The watch uses it to tell when the phone's plan
     * already takes in a workout it finished.
     */
    fun observeRecentWorkoutIds(): Flow<List<String>>

    /** Records that the person took the rest day the coach offered, at [nowEpochMillis]. */
    suspend fun recordRest(nowEpochMillis: Long)

    /** Records the advice from a pain check-in for [area]. */
    suspend fun recordPain(area: BodyArea, assessment: PainAssessment)

    /** Records that the fast weight loss notice has been shown, so it isn't shown again this spell. */
    suspend fun markWeightNoticeShown()
}

/** How many recent workout ids [CoachRepository.observeRecentWorkoutIds] reports. */
const val RECENT_WORKOUTS = 10
