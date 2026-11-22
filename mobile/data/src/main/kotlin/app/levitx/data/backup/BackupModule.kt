package app.levitx.data.backup

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Tells Hilt which implementation backs [BackupRepository]. */
@Module
@InstallIn(SingletonComponent::class)
internal interface BackupModule {
    @Binds
    fun bindBackupRepository(repository: RoomBackupRepository): BackupRepository
}
