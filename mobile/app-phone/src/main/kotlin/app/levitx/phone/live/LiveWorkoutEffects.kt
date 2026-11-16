package app.levitx.phone.live

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * What the workout screen starts while a workout is [active]: the service
 * that keeps it going off screen, and, the first time, Android's question
 * about notifications. Both happen while the screen is in front, which is
 * when Android allows them.
 */
@Composable
fun LiveWorkoutEffects(active: Boolean, prompt: NotificationPromptViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val shouldAsk by prompt.shouldAsk.collectAsStateWithLifecycle()
    // Either answer is fine. Without notifications the workout runs and beeps the same,
    // with nothing on the lock screen.
    val askForNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(active) {
        if (active) LiveWorkoutService.start(context)
    }
    LaunchedEffect(active, shouldAsk) {
        if (!active || !shouldAsk) return@LaunchedEffect
        // Marked first, so a second pass through here can't ask twice.
        prompt.asked()
        // Before Android 13, notifications need no permission.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            askForNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
