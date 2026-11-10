package app.levitx.wear.workout

import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [WorkoutMonitor] tests set by hand. */
class FakeWorkoutMonitor(var permissionGranted: Boolean = true) : WorkoutMonitor {
    override val heartRate = MutableStateFlow<Int?>(null)

    override val isActive = MutableStateFlow(false)

    override val hasAsked = MutableStateFlow(false)

    /** How many times the service was asked to start. */
    var startRequests = 0
        private set

    override fun canTrack(): Boolean = permissionGranted

    override fun markAsked() {
        hasAsked.value = true
    }

    override fun ensureStarted() {
        startRequests++
        // Like the real one: the service only runs with the permission.
        if (permissionGranted) isActive.value = true
    }
}
