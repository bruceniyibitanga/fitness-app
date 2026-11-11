package app.levitx.phone

import android.app.Application
import app.levitx.sync.PhoneSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** The phone app's process-wide entry point. Hilt builds its dependency graph here. */
@HiltAndroidApp
class LevitxPhoneApplication : Application() {
    /** Keeps the watch in step with the phone. Set by Hilt. */
    @Inject
    lateinit var phoneSync: PhoneSync

    override fun onCreate() {
        super.onCreate()
        // Returns at once: the work runs on a background scope, so app start isn't slowed.
        phoneSync.start()
    }
}
