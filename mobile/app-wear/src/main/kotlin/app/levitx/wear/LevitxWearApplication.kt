package app.levitx.wear

import android.app.Application
import app.levitx.sync.WatchSync
import app.levitx.wear.tile.TileRefresher
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/** The watch app's process-wide entry point. Hilt builds its dependency graph here. */
@HiltAndroidApp
class LevitxWearApplication : Application() {
    /** Keeps the watch in step with the phone. Set by Hilt. */
    @Inject
    lateinit var watchSync: WatchSync

    /** Keeps the Tile up to date. Set by Hilt. */
    @Inject
    lateinit var tileRefresher: TileRefresher

    override fun onCreate() {
        super.onCreate()
        // Both return at once: the work runs on a background scope, so app start isn't slowed.
        watchSync.start()
        tileRefresher.start()
    }
}
