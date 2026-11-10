package app.levitx.data

import android.content.Context
import android.content.pm.PackageManager
import app.levitx.data.goal.GoalRepository
import app.levitx.data.goal.RoomGoalRepository
import app.levitx.data.progress.ProgressRepository
import app.levitx.data.progress.RoomProgressRepository
import app.levitx.data.security.DatabaseKeyStore
import app.levitx.data.session.RoomSessionRepository
import app.levitx.data.session.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** Name of the encrypted database key file in `noBackupFilesDir`. */
private const val DATABASE_KEY_FILE_NAME = "database_key.bin"

/** Tells Hilt how to build the data layer and which implementation backs each interface. */
@Module
@InstallIn(SingletonComponent::class)
internal interface DataModule {
    @Binds
    fun bindProgressRepository(repository: RoomProgressRepository): ProgressRepository

    @Binds
    fun bindGoalRepository(repository: RoomGoalRepository): GoalRepository

    @Binds
    fun bindSessionRepository(repository: RoomSessionRepository): SessionRepository

    companion object {
        @Provides
        @IoDispatcher
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

        @Provides
        @Singleton
        fun provideDatabaseKeyStore(
            @ApplicationContext context: Context,
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
        ): DatabaseKeyStore = DatabaseKeyStore(
            // noBackupFilesDir is never included in any backup, on any Android version.
            keyFile = File(context.noBackupFilesDir, DATABASE_KEY_FILE_NAME),
            ioDispatcher = ioDispatcher,
            useStrongBox = context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE),
        )
    }
}
