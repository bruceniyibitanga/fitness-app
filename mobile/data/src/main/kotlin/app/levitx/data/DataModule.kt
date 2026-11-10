package app.levitx.data

import app.levitx.data.progress.InMemoryProgressRepository
import app.levitx.data.progress.ProgressRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Tells Hilt which implementation to use for each repository interface. */
@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {
    @Binds
    fun bindProgressRepository(repository: InMemoryProgressRepository): ProgressRepository
}
