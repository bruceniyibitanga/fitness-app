package app.levitx.wear

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** The watch app's process-wide entry point. Hilt builds its dependency graph here. */
@HiltAndroidApp
class LevitxWearApplication : Application()
