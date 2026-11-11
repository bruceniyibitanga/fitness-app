package app.levitx.wear.workout

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import app.levitx.wear.session.WorkoutController
import app.levitx.wear.session.WorkoutState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Runs the workout while a session is in progress.
 *
 * The Wear OS workout guide puts sensors, the ongoing activity and anything
 * that must outlive the screen in a foreground service, so the session keeps
 * going when the person lowers their wrist or swipes to the watch face. This
 * one does three things, each in its own class:
 * - [ExerciseTracker] records a Health Services exercise and reads heart rate.
 * - [WorkoutNotifications] shows where the session is up to.
 * - [RestAlert] buzzes when rest ends.
 *
 * It doesn't hold the session. That stays with [WorkoutController]; the
 * service watches it and stops itself as soon as no session is running.
 */
@AndroidEntryPoint
class WorkoutService : Service() {
    /** The session being run. Set by Hilt. */
    @Inject
    lateinit var controller: WorkoutController

    /** Health Services recording. Set by Hilt. */
    @Inject
    lateinit var tracker: ExerciseTracker

    /** Builds the notification. Set by Hilt. */
    @Inject
    lateinit var notifications: WorkoutNotifications

    /** The end-of-rest buzz. Set by Hilt. */
    @Inject
    lateinit var restAlert: RestAlert

    /** Tells the screens whether this service is running. Set by Hilt. */
    @Inject
    lateinit var monitor: ServiceWorkoutMonitor

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var work: Job? = null

    // Nothing binds to this service. The screens watch the controller instead.
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Android requires this within a few seconds of the service being started.
        showInForeground(notifications.starting())
        if (work == null) work = scope.launch { runWorkout() }
        // If the system kills the app, don't restart on its own: a workout may
        // only start while the app is on screen. The session screen restarts it.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        monitor.onServiceStopped()
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun runWorkout() {
        monitor.onServiceStarted()
        try {
            tracker.start()
            controller.state
                // The session may still be loading or being planned when the service starts.
                .dropWhile { it is WorkoutState.Loading || it is WorkoutState.Starting }
                .takeWhile { it is WorkoutState.Running }
                // collectLatest cancels the wait for rest to end when the person skips it.
                .collectLatest { state ->
                    val run = (state as WorkoutState.Running).run
                    showInForeground(notifications.forRun(run))
                    val restEndsAt = run.restEndsAtElapsedMillis
                    if (restEndsAt != null) {
                        restAlert.awaitEndOfRest(restEndsAt)
                        controller.endRest()
                    }
                }
        } finally {
            // Runs even when the service is being destroyed, so the sensor is never left on.
            withContext(NonCancellable) { tracker.stop() }
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /** Shows [notification] as this service's notification. Calling it again updates the text. */
    private fun showInForeground(notification: Notification) {
        // The "health" type only exists from Android 14. Before that the service has no type.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(WORKOUT_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(WORKOUT_NOTIFICATION_ID, notification)
        }
    }
}
