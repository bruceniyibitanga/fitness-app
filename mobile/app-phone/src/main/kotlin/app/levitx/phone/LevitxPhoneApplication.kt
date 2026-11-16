package app.levitx.phone

import android.app.Application
import app.levitx.phone.live.WorkoutCuer
import app.levitx.sync.PhoneSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** The phone app's process-wide entry point. Hilt builds its dependency graph here. */
@HiltAndroidApp
class LevitxPhoneApplication : Application() {
    /** Keeps the watch in step with the phone. Set by Hilt. */
    @Inject
    lateinit var phoneSync: PhoneSync

    /** Beeps and buzzes through workouts. Set by Hilt. */
    @Inject
    lateinit var workoutCuer: WorkoutCuer

    override fun onCreate() {
        super.onCreate()
        // Both return at once: the work runs on a background scope, so app start isn't slowed.
        phoneSync.start()
        // Waits, doing nothing, until a workout starts.
        workoutCuer.start()
    }
}
