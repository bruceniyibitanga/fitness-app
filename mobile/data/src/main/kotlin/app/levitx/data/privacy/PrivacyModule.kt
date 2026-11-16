package app.levitx.data.privacy

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Tells Hilt which implementation backs [PrivacyRepository]. */
@Module
@InstallIn(SingletonComponent::class)
internal interface PrivacyModule {
    @Binds
    fun bindPrivacyRepository(repository: RoomPrivacyRepository): PrivacyRepository
}
