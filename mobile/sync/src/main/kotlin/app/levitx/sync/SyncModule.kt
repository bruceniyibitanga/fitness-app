package app.levitx.sync

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Tells Hilt that the Data Layer is reached through Google Play services. */
@Module
@InstallIn(SingletonComponent::class)
internal interface SyncModule {
    @Binds
    fun bindDataLayer(dataLayer: PlayServicesDataLayer): DataLayer
}
