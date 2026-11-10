package app.levitx.wear

import android.app.Application
import app.levitx.sync.WatchSync
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** The watch app's process-wide entry point. Hilt builds its dependency graph here. */
@HiltAndroidApp
class LevitxWearApplication : Application() {
    /** Keeps the watch in step with the phone. Set by Hilt. */
    @Inject
    lateinit var watchSync: WatchSync

    override fun onCreate() {
        super.onCreate()
        // Returns at once: the work runs on a background scope, so app start isn't slowed.
        watchSync.start()
    }
}
