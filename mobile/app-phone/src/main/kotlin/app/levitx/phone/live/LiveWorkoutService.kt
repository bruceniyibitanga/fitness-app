package app.levitx.phone.live

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.levitx.data.time.TrainingClock
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** How long the service waits for the workout screen to publish the workout before giving up. */
private const val START_GRACE_MILLIS = 5_000L

/**
 * Keeps the app running through a workout, and shows the workout notification.
 *
 * Without it, Android may pause or close the app soon after the person locks
 * the phone or opens another app, and the beep at the end of rest never
 * comes. A foreground service tells Android the person is using the app even
 * though it isn't on screen. Its type is `health`, the type Android gives
 * exercise trackers. ADR 0008 has the reasoning.
 *
 * It doesn't hold the workout (the workout screen does) or play the cues
 * ([WorkoutCuer] does). It follows [LiveWorkout] and stops itself as soon as
 * no workout is running, so a workout works the same, on screen, if the
 * service can't start. A cardio or mobility workout on the interval timer is
 * kept going the same way, with a simpler notification.
 */
@AndroidEntryPoint
class LiveWorkoutService : Service() {
    /** The workout to follow. Set by Hilt. */
    @Inject
    lateinit var live: LiveWorkout

    /** Builds the notification. Set by Hilt. */
    @Inject
    lateinit var notifications: LiveNotifications

    /** The elapsed clock, for when rest runs out. Set by Hilt. */
    @Inject
    lateinit var clock: TrainingClock

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var following: Job? = null

    // Nothing binds to this service. It follows LiveWorkout instead.
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISMISSED) {
            live.dismissNotice()
            // Only reached this way if the service had already stopped; then it has nothing to do.
            if (following == null) stopSelf(startId)
            return START_NOT_STICKY
        }
        // Android requires this within a few seconds of every start, even one that ends at once.
        val shown = showInForeground(current())
        if (!shown) {
            stopSelf()
        } else if (following == null) {
            following = scope.launch { follow() }
        }
        // If Android closes the app, don't start again on its own: the workout screen starts this.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /** The notification for the workout now, or a plain one until it is published. */
    private fun current(): Notification = live.session.value
        ?.let { notifications.forSession(it, liveStatus(it, clock.elapsedRealtimeMillis())) }
        ?: live.intervals.value?.let(::forIntervals)
        ?: notifications.plain()

    /** The notification for a cardio or mobility workout: its name and the step it is on. */
    private fun forIntervals(intervals: LiveIntervals): Notification =
        notifications.plain(intervals.workoutName, intervals.noticeText(this))

    private suspend fun follow() {
        try {
            val showing = combine(live.session, live.intervals, live.noticeDismissed) { session, intervals, dismissed ->
                Showing(session?.forNotice(), intervals, dismissed)
            }
            // The screen publishes the workout before starting this, but don't count on the order.
            withTimeoutOrNull(START_GRACE_MILLIS) { showing.first { it.hasWorkout } } ?: return
            showing
                .takeWhile { it.hasWorkout }
                .distinctUntilChanged()
                // A newer value cancels the wait for rest to run out.
                .collectLatest { now ->
                    when {
                        now.dismissed -> Unit
                        now.session != null -> showUntilSettled(now.session)
                        now.intervals != null -> showInForeground(forIntervals(now.intervals))
                    }
                }
        } finally {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /** Shows [session], then shows it again each time it moves on by itself: rest running out, a count-in ending. */
    private suspend fun showUntilSettled(session: LiveSession) {
        while (true) {
            val status = liveStatus(session, clock.elapsedRealtimeMillis())
            showInForeground(notifications.forSession(session, status))
            val at = status.changesAt() ?: return
            delay((at - clock.elapsedRealtimeMillis()).coerceAtLeast(0))
        }
    }

    /**
     * Shows [notification] as this service's notification. Calling it again
     * updates it. Returns false if Android refused, which can happen if the
     * app went to the background in the moment it took to start.
     */
    private fun showInForeground(notification: Notification): Boolean = try {
        // The "health" type only exists from Android 14. Before that the service has no type.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(WORKOUT_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(WORKOUT_NOTIFICATION_ID, notification)
        }
        true
    } catch (_: IllegalStateException) {
        // ForegroundServiceStartNotAllowedException is an IllegalStateException.
        false
    } catch (_: SecurityException) {
        // Only if the manifest lost the permissions the health type needs.
        false
    }

    /**
     * What there is to show: a strength workout, a cardio or mobility
     * workout, or neither, and whether the person swiped the notification away.
     */
    private data class Showing(val session: LiveSession?, val intervals: LiveIntervals?, val dismissed: Boolean) {
        val hasWorkout: Boolean
            get() = session != null || intervals != null
    }

    /** Starting the service, and what the notification sends it. */
    companion object {
        /** Sent when the person swipes the notification away. */
        internal const val ACTION_DISMISSED = "app.levitx.phone.live.DISMISSED"

        /**
         * Starts the service for the workout in progress. Call it while the
         * workout is on screen: Android only lets an app start a foreground
         * service from the background in a few special cases.
         */
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, LiveWorkoutService::class.java))
            } catch (_: IllegalStateException) {
                // Refused because the app is no longer in front. The workout carries on without it.
            }
        }
    }
}
