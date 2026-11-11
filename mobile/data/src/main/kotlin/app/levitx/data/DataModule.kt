package app.levitx.data

import android.content.Context
import android.content.pm.PackageManager
import app.levitx.core.coach.Coach
import app.levitx.data.coach.CoachRepository
import app.levitx.data.coach.RoomCoachRepository
import app.levitx.data.coach.RoomWorkoutDraftRepository
import app.levitx.data.coach.WorkoutDraftRepository
import app.levitx.data.equipment.EquipmentRepository
import app.levitx.data.equipment.RoomEquipmentRepository
import app.levitx.data.goal.GoalRepository
import app.levitx.data.goal.RoomGoalRepository
import app.levitx.data.progress.ProgressRepository
import app.levitx.data.progress.RoomProgressRepository
import app.levitx.data.security.DatabaseKeyStore
import app.levitx.data.session.RoomSessionDraftRepository
import app.levitx.data.session.RoomSessionRepository
import app.levitx.data.session.SessionDraftRepository
import app.levitx.data.session.SessionOutbox
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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

    @Binds
    fun bindSessionOutbox(repository: RoomSessionRepository): SessionOutbox

    @Binds
    fun bindSessionDraftRepository(repository: RoomSessionDraftRepository): SessionDraftRepository

    @Binds
    fun bindEquipmentRepository(repository: RoomEquipmentRepository): EquipmentRepository

    @Binds
    fun bindCoachRepository(repository: RoomCoachRepository): CoachRepository

    @Binds
    fun bindWorkoutDraftRepository(repository: RoomWorkoutDraftRepository): WorkoutDraftRepository

    companion object {
        // The coach holds no state, so one instance serves the whole app.
        @Provides
        @Singleton
        fun provideCoach(): Coach = Coach()

        @Provides
        @IoDispatcher
        fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

        // A SupervisorJob so one failed job doesn't cancel the others sharing the scope.
        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
