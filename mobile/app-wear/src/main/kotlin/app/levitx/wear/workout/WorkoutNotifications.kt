package app.levitx.wear.workout

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import app.levitx.core.session.SessionRun
import app.levitx.wear.MainActivity
import app.levitx.wear.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val CHANNEL_ID = "workout"

/** The id of the workout notification. Any fixed number other than 0 works. */
internal const val WORKOUT_NOTIFICATION_ID = 1

/**
 * Builds the "session in progress" notification and its ongoing activity.
 *
 * A foreground service must show a notification. Pairing it with an
 * OngoingActivity is what puts the workout on the watch face and in the
 * launcher, one tap away from the app, as the Wear OS quality guidelines
 * require for anything long-running.
 *
 * The notification says where the session is up to (a set number, or rest).
 * It is marked public, so that much shows on a locked watch; it never
 * includes reps, heart rate or anything else personal.
 */
@Singleton
class WorkoutNotifications @Inject constructor(@param:ApplicationContext private val context: Context) {
    /** The notification for a session that is starting or being restored. */
    fun starting(): Notification = build(context.getString(R.string.workout_status_starting))

    /** The notification for where [run] is up to. */
    fun forRun(run: SessionRun): Notification = build(
        if (run.isResting) {
            context.getString(R.string.workout_status_rest)
        } else {
            context.getString(R.string.workout_status_set, run.nextSetIndex + 1, run.plan.sets.size)
        },
    )

    private fun build(status: String): Notification {
        createChannel()
        // Tapping the notification or the watch-face indicator returns to the app as it was left.
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.workout_notification_title))
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_workout)
            // The category tells Wear OS this is a workout, which sets its priority among ongoing activities.
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setContentIntent(openApp)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        OngoingActivity.Builder(context, WORKOUT_NOTIFICATION_ID, builder)
            .setStaticIcon(R.drawable.ic_workout)
            .setTouchIntent(openApp)
            .setStatus(Status.Builder().addTemplate(status).build())
            .build()
            // Adds the ongoing-activity data to the builder. Must be called before build().
            .apply(context)
        return builder.build()
    }

    /** Creating a channel that already exists does nothing, so this is safe to call every time. */
    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.workout_channel_name),
            // Low: it is there to be glanced at, and must never buzz or make a sound itself.
            NotificationManager.IMPORTANCE_LOW,
        )
        checkNotNull(context.getSystemService(NotificationManager::class.java)).createNotificationChannel(channel)
    }
}
