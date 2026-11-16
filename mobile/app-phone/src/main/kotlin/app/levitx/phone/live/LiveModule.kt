package app.levitx.phone.live

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Provides the device's speaker, motor and wake lock to [WorkoutCuer]. */
@Module
@InstallIn(SingletonComponent::class)
internal interface LiveModule {
    /** Beeps and buzzes on this phone. */
    @Binds
    fun bindCueOutput(output: DeviceCueOutput): CueOutput

    /** Keeps the processor awake with a partial wake lock. */
    @Binds
    fun bindKeepAwake(awake: WakeLockKeepAwake): KeepAwake
}
