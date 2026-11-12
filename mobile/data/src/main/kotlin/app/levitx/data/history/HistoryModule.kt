package app.levitx.data.history

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Tells Hilt which implementation backs [HistoryRepository]. */
@Module
@InstallIn(SingletonComponent::class)
internal interface HistoryModule {
    @Binds
    fun bindHistoryRepository(repository: RoomHistoryRepository): HistoryRepository
}
