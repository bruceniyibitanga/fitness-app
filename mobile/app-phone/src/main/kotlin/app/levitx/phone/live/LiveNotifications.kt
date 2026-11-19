package app.levitx.phone.live

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import app.levitx.data.time.TrainingClock
import app.levitx.design.LevitxColors
import app.levitx.phone.MainActivity
import app.levitx.phone.R
import app.levitx.phone.workout.TimerKind
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val CHANNEL_ID = "workout"

/** The id of the workout notification. Any fixed number other than 0 works. */
internal const val WORKOUT_NOTIFICATION_ID = 1

/**
 * Builds the workout notification, asking Android to show it as a Live Update.
 *
 * On Android 16 and later a Live Update sits at the top of the notifications,
 * on the lock screen, and as a chip in the status bar, which shows the rest
 * countdown. Earlier versions show the same notification the ordinary way.
 * Android's guidance lists starting a workout as a fitting use.
 *
 * What it shows: the exercise, the set and how far through the workout the
 * person is, and the rest or hold clock. It is public, so that shows on a
 * locked phone. It never shows reps, loads, bodyweight or anything else from
 * their log.
 *
 * The clocks are drawn by Android from one start or end time, so the
 * notification is only posted again when something changes, not every second.
 */
@Singleton
class LiveNotifications @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clock: TrainingClock,
) {
    /**
     * A notification with just a [title] and a line of [text]. The default
     * is the one for a workout the service hasn't caught up with yet.
     */
    fun plain(title: String = context.getString(R.string.live_starting), text: String? = null): Notification = builder()
        .setContentTitle(title)
        .setContentText(text)
        .setShowWhen(false)
        .build()

    /** The notification for [session], which is doing [status]. */
    internal fun forSession(session: LiveSession, status: LiveStatus): Notification {
        val set = context.getString(R.string.live_set_of, session.setNumber, session.setCount)
        val builder = builder()
            .setContentTitle(title(session, status))
            .setContentText(text(session, status, set))
            .setStyle(progress(session))
        when (status) {
            is LiveStatus.Resting -> builder.clock(status.endsAt, countsDown = true)

            is LiveStatus.CountingIn -> builder.clock(status.startsAt, countsDown = true)

            is LiveStatus.Timing -> builder.clock(status.startedAt, countsDown = false)

            // A set waiting for the person: the status bar chip names it instead of a clock.
            LiveStatus.RestOver, LiveStatus.Ready ->
                builder
                    .setShowWhen(false)
                    .setShortCriticalText(context.getString(R.string.live_chip_set, session.setNumber))
        }
        return builder.build()
    }

    private fun title(session: LiveSession, status: LiveStatus): String = when (status) {
        is LiveStatus.Resting -> context.getString(R.string.live_title_rest)
        LiveStatus.RestOver -> context.getString(R.string.live_title_rest_over)
        is LiveStatus.CountingIn -> context.getString(R.string.live_title_count_in)
        is LiveStatus.Timing, LiveStatus.Ready -> session.exerciseName
    }

    private fun text(session: LiveSession, status: LiveStatus, set: String): String = when (status) {
        is LiveStatus.Resting -> context.getString(R.string.live_text_next, session.exerciseName, set)

        LiveStatus.RestOver, is LiveStatus.CountingIn ->
            context.getString(R.string.live_text_exercise_set, session.exerciseName, set)

        is LiveStatus.Timing -> context.getString(
            if (status.kind == TimerKind.HOLD) R.string.live_text_holding else R.string.live_text_negatives,
            set,
        )

        LiveStatus.Ready -> context.getString(
            R.string.live_text_ready,
            session.setNumber,
            session.setCount,
            session.exerciseNumber,
            session.exerciseCount,
        )
    }

    /** One segment per exercise, as long as its sets, filled up to where the person is. */
    private fun progress(session: LiveSession): NotificationCompat.ProgressStyle = NotificationCompat.ProgressStyle()
        .setProgressSegments(
            session.setsPlanned
                .filter { it > 0 }
                .map { NotificationCompat.ProgressStyle.Segment(it).setColor(LevitxColors.Accent.toArgb()) },
        )
        .setProgress(session.setsBehind)

    /** Shows a clock running from or to [elapsedAt], a time on the elapsed clock. */
    private fun NotificationCompat.Builder.clock(elapsedAt: Long, countsDown: Boolean): NotificationCompat.Builder {
        // Notifications draw clocks from wall-clock time, so the elapsed time is moved across.
        val wallAt = clock.currentTimeMillis() + (elapsedAt - clock.elapsedRealtimeMillis())
        return setWhen(wallAt)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(countsDown)
    }

    private fun builder(): NotificationCompat.Builder {
        createChannel()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_workout)
            .setColor(LevitxColors.Accent.toArgb())
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setRequestPromotedOngoing(true)
            // Updates never make a sound or buzz: the cues do that, and only when they mean something.
            .setOnlyAlertOnce(true)
            .setSilent(true)
            // Shown as soon as the workout starts, rather than after the few seconds Android may wait.
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openApp())
            .setDeleteIntent(dismissed())
    }

    /** Back to the app as it was left, like tapping its icon. */
    private fun openApp(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Tells the service the person swiped the notification away, so it isn't put back. */
    private fun dismissed(): PendingIntent = PendingIntent.getService(
        context,
        0,
        Intent(context, LiveWorkoutService::class.java).setAction(LiveWorkoutService.ACTION_DISMISSED),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Creating a channel that already exists does nothing, so this is safe to call every time. */
    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.live_channel_name),
            // Default, so it shows in the status bar; never minimum, which rules out a Live Update.
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.live_channel_description)
            // Silent at the channel too: the notification is there to be glanced at.
            setSound(null, null)
            enableVibration(false)
        }
        checkNotNull(context.getSystemService(NotificationManager::class.java)).createNotificationChannel(channel)
    }
}
