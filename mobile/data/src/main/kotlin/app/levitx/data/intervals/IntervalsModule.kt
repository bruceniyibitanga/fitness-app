package app.levitx.data.intervals

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Tells Hilt which implementation backs [IntervalOutbox]. */
@Module
@InstallIn(SingletonComponent::class)
internal interface IntervalsModule {
    @Binds
    fun bindIntervalOutbox(outbox: RoomIntervalOutbox): IntervalOutbox
}
