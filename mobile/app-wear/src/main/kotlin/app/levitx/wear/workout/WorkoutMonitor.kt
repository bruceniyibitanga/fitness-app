package app.levitx.wear.workout

import kotlinx.coroutines.flow.StateFlow

/**
 * What the session screens need from the workout running behind them.
 *
 * The real one, [ServiceWorkoutMonitor], is backed by a foreground service and
 * Health Services. It is an interface so the screens' ViewModels can be
 * tested without a watch.
 */
interface WorkoutMonitor {
    /** Heart rate in beats per minute, or null when there is no reading. Never stored. */
    val heartRate: StateFlow<Int?>

    /**
     * True while the workout service is running. It then keeps the session
     * alive off-screen and gives the rest alert itself.
     */
    val isActive: StateFlow<Boolean>

    /** True once the person has been asked about tracking since the app last started. */
    val hasAsked: StateFlow<Boolean>

    /** True when heart rate may be read, which the workout service needs to run. */
    fun canTrack(): Boolean

    /** Records that the person was asked, whatever they answered, so they aren't asked again this run. */
    fun markAsked()

    /**
     * Starts the workout service if it may run and isn't running already.
     * Call only while the app is on screen: Android doesn't let a workout
     * start from the background.
     */
    fun ensureStarted()
}
