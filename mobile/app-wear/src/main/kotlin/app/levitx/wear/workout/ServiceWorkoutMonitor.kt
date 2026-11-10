package app.levitx.wear.workout

import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "LevitxWorkout"

/** [WorkoutMonitor] backed by [WorkoutService] and [ExerciseTracker]. */
@Singleton
class ServiceWorkoutMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    tracker: ExerciseTracker,
) : WorkoutMonitor {
    private val active = MutableStateFlow(false)
    private val asked = MutableStateFlow(false)

    override val heartRate: StateFlow<Int?> = tracker.heartRate

    override val isActive: StateFlow<Boolean> = active.asStateFlow()

    override val hasAsked: StateFlow<Boolean> = asked.asStateFlow()

    override fun canTrack(): Boolean = WorkoutPermissions.canTrack(context)

    override fun markAsked() {
        asked.value = true
    }

    override fun ensureStarted() {
        if (active.value || !canTrack()) return
        try {
            context.startForegroundService(Intent(context, WorkoutService::class.java))
        } catch (error: IllegalStateException) {
            // Android refuses to start a workout from the background
            // (ForegroundServiceStartNotAllowedException). The session works
            // without the service, and this is called again when the session
            // screen is next on screen.
            Log.w(TAG, "Workout service not started: ${error.javaClass.simpleName}")
        }
    }

    /** Called by [WorkoutService] when it starts running. */
    internal fun onServiceStarted() {
        active.value = true
    }

    /** Called by [WorkoutService] when it stops. */
    internal fun onServiceStopped() {
        active.value = false
    }
}

/** Tells Hilt which [WorkoutMonitor] the app uses. */
@Module
@InstallIn(SingletonComponent::class)
interface WorkoutModule {
    /** Binds the service-backed monitor. */
    @Binds
    fun bindWorkoutMonitor(monitor: ServiceWorkoutMonitor): WorkoutMonitor
}
