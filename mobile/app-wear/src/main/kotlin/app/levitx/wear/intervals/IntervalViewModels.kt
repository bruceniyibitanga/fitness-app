package app.levitx.wear.intervals

import androidx.lifecycle.ViewModel
import app.levitx.core.intervals.IntervalCatalogue
import app.levitx.core.intervals.IntervalWorkout
import app.levitx.wear.workout.WorkoutMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/*
 * The two cardio and mobility screens hold no state of their own: the
 * workout lives in IntervalController, so it outlasts them. These connect
 * each screen to it.
 */

/** Connects the list of cardio and mobility workouts to [IntervalController]. */
@HiltViewModel
class IntervalListViewModel @Inject constructor(private val controller: IntervalController) : ViewModel() {
    /** Every workout the watch can run. The catalogue is built in, so the phone isn't needed. */
    val workouts: List<IntervalWorkout> = IntervalCatalogue.all

    /** Starts the workout with [workoutId], unless one is already running. */
    fun start(workoutId: String) = controller.start(workoutId)
}

/** Connects the interval timer screen to [IntervalController] and the workout service. */
@HiltViewModel
class IntervalTimerViewModel @Inject constructor(
    private val controller: IntervalController,
    private val monitor: WorkoutMonitor,
) : ViewModel() {
    /** What the timer is doing now. */
    val state: StateFlow<IntervalState> = controller.state

    /** Stops or restarts the clock. */
    fun togglePause() = controller.togglePause()

    /** Ends the step under way. */
    fun skip() = controller.skip()

    /** Ends the workout, keeping what was done. */
    fun end() = controller.end()

    /** Clears the done screen. */
    fun dismiss() = controller.dismiss()

    /**
     * Starts the workout service, so the timer outlives the screen and the
     * workout shows on the watch face. Only when it's allowed to run (it
     * needs the permission the strength session asks for); without it the
     * timer still runs while the app is open.
     */
    fun keepAlive() = monitor.ensureStarted()
}
