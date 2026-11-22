package app.levitx.phone.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Every screen the app can navigate to. Each destination is a key on the
 * back stack.
 *
 * Keys are @Serializable so the back stack can be saved and restored after
 * the system kills the app in the background. They never carry health data,
 * because saved state is written to disk. Screens read what they need from
 * the encrypted database instead.
 */
sealed interface Destination : NavKey {
    /** Setup: the goal, how to train and the week, then the first plan. */
    @Serializable
    data object Onboarding : Destination

    /** The tabs: Today, Train, Plan, Skills and Body. */
    @Serializable
    data object Main : Destination

    /** The workout in progress. It is read from the saved draft, so this key needs nothing else. */
    @Serializable
    data object Workout : Destination

    /**
     * A single workout picked from the Train tab, before it starts.
     *
     * @property focus The [app.levitx.core.coach.WorkoutFocus] name. Stored as text so a renamed focus can't
     *   break restoring the back stack; an unknown name opens nothing.
     */
    @Serializable
    data class FocusWorkout(val focus: String) : Destination

    /** Last week, looked back on. */
    @Serializable
    data object WeekReview : Destination

    /** Search across exercises, programs and workouts. The query isn't part of the key. */
    @Serializable
    data object Search : Destination

    /** Every skill program, by level. */
    @Serializable
    data object AllPrograms : Destination

    /**
     * A cardio or mobility workout picked from the Train tab, before it starts.
     *
     * @property workoutId The workout's catalogue id. Not personal data.
     */
    @Serializable
    data class IntervalWorkout(val workoutId: String) : Destination

    /**
     * The timer for a cardio or mobility workout. The run itself isn't kept
     * here: saved state goes to disk, and the timer starts afresh if the app
     * is killed.
     *
     * @property workoutId The workout's catalogue id. Not personal data.
     */
    @Serializable
    data class IntervalTimer(val workoutId: String) : Destination

    /**
     * How to do one exercise, and where it sits on its ladder.
     *
     * @property exerciseId The exercise's catalogue id. Not personal data.
     */
    @Serializable
    data class ExerciseDetail(val exerciseId: String) : Destination

    /**
     * A skill program, with the option to make it the plan.
     *
     * @property pathId The skill path's id.
     */
    @Serializable
    data class Program(val pathId: String) : Destination

    /** Progress: load over time, every movement, the week's sets, and gates passed. */
    @Serializable
    data object Progress : Destination

    /**
     * One exercise's progress.
     *
     * @property exerciseId The exercise's catalogue id. Not personal data.
     */
    @Serializable
    data class ExerciseProgress(val exerciseId: String) : Destination

    /** Every workout, a month at a time. */
    @Serializable
    data object History : Destination

    /**
     * One past workout.
     *
     * @property workoutId The workout's random id. It says nothing about the
     *   person; what was done is read from the encrypted database.
     */
    @Serializable
    data class PastWorkout(val workoutId: String) : Destination

    /**
     * The pain check: where it hurts, warning signs, and how bad it is. The
     * key carries nothing, and the answers in progress aren't saved with the
     * back stack either: they are health data, and saved state goes to disk.
     */
    @Serializable
    data object PainCheck : Destination

    /** Settings, the person's data and about, from the Body tab. */
    @Serializable
    data object Settings : Destination

    /** Who took the photos in the app. */
    @Serializable
    data object PhotoCredits : Destination

    /**
     * Backing up to an encrypted file. The key carries nothing: the words
     * live only in the screen's ViewModel, never in saved state.
     */
    @Serializable
    data object Backup : Destination

    /** Restoring from a backup, from Settings or from the first setup screen. */
    @Serializable
    data object Restore : Destination
}
