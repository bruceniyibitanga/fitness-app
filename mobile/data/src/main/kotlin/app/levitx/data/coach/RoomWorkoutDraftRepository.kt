package app.levitx.data.coach

import app.levitx.core.coach.WorkoutRun
import app.levitx.data.database.LevitxDatabaseProvider
import app.levitx.data.database.WorkoutDraftEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** Keeps the workout in progress in the encrypted database. */
@Singleton
internal class RoomWorkoutDraftRepository @Inject constructor(private val databaseProvider: LevitxDatabaseProvider) :
    WorkoutDraftRepository {
    override fun observeDraft(): Flow<WorkoutRun?> = flow {
        emitAll(databaseProvider.database().coachDao().observeDraft())
    }
        .map { row -> row?.let { CoachCodec.decodeWorkoutOrNull(it.json)?.toRunOrNull() } }
        .distinctUntilChanged()

    override suspend fun save(run: WorkoutRun) {
        val json = CoachCodec.encode(workoutDocumentOf(run))
        databaseProvider.database().coachDao().replaceDraft(WorkoutDraftEntity(json = json))
    }

    override suspend fun discard() {
        databaseProvider.database().coachDao().clearDraft()
    }
}
