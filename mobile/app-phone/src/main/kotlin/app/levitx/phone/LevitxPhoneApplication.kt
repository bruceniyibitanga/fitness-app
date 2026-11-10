package app.levitx.phone

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** The phone app's process-wide entry point. Hilt builds its dependency graph here. */
@HiltAndroidApp
class LevitxPhoneApplication : Application()
