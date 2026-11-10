package app.levitx.phone.session

import android.os.SystemClock
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

/**
 * The two clocks a session needs. An interface so tests can control time.
 */
interface TrainingClock {
    /**
     * Milliseconds since the device booted. Doesn't jump when the person
     * changes the time or time zone, so it is what rest timers count against.
     */
    fun elapsedRealtimeMillis(): Long

    /** Wall-clock time in UTC milliseconds, for timestamps that get saved. */
    fun currentTimeMillis(): Long
}

/** The real clocks. */
class SystemTrainingClock @Inject constructor() : TrainingClock {
    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()

    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}

/** Provides [SystemTrainingClock] wherever a [TrainingClock] is needed. */
@Module
@InstallIn(SingletonComponent::class)
interface TrainingClockModule {
    /** Binds the real clock. */
    @Binds
    fun bindTrainingClock(clock: SystemTrainingClock): TrainingClock
}
