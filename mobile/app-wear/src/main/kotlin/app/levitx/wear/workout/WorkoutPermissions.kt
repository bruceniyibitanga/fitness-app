package app.levitx.wear.workout

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Wear OS 6. From here heart rate has its own permission. */
private const val GRANULAR_HEALTH_PERMISSIONS_SDK = 36

/** Android 13. From here showing notifications needs a permission. */
private const val NOTIFICATION_PERMISSION_SDK = 33

// Written out rather than taken from android.Manifest: BODY_SENSORS is being
// retired and the newer two don't exist on every version this app supports.
private const val BODY_SENSORS = "android.permission.BODY_SENSORS"
private const val READ_HEART_RATE = "android.permission.health.READ_HEART_RATE"
private const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"

/**
 * The permissions the workout asks the person for, which depend on the
 * version of Wear OS.
 */
object WorkoutPermissions {
    /** The permission that allows reading heart rate on this device. */
    val heartRate: String =
        if (Build.VERSION.SDK_INT >= GRANULAR_HEALTH_PERMISSIONS_SDK) READ_HEART_RATE else BODY_SENSORS

    /** Everything to ask for when the first session starts. */
    val toRequest: Array<String> =
        if (Build.VERSION.SDK_INT >= NOTIFICATION_PERMISSION_SDK) {
            arrayOf(heartRate, POST_NOTIFICATIONS)
        } else {
            arrayOf(heartRate)
        }

    /**
     * True when heart rate may be read. The workout service needs it: Android
     * refuses to start a "health" foreground service without a sensor permission.
     */
    fun canTrack(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, heartRate) == PackageManager.PERMISSION_GRANTED
}
