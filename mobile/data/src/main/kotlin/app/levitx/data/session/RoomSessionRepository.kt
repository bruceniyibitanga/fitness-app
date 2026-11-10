package app.levitx.data.session

import androidx.room.withTransaction
import app.levitx.core.session.CompletedSession
import app.levitx.data.database.LevitxDatabaseProvider
import app.levitx.data.database.LoggedSetEntity
import app.levitx.data.database.TrainingSessionEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Stores sessions in the encrypted database. */
@Singleton
internal class RoomSessionRepository @Inject constructor(
    private val databaseProvider: LevitxDatabaseProvider,
) : SessionRepository {
    override suspend fun saveCompletedSession(session: CompletedSession) {
        val database = databaseProvider.database()
        val sessionId = UUID.randomUUID().toString()
        database.withTransaction {
            database.sessionDao().insertSession(
                TrainingSessionEntity(
                    id = sessionId,
                    gateId = session.gateId,
                    startedAtEpochMillis = session.startedAtEpochMillis,
                    finishedAtEpochMillis = session.finishedAtEpochMillis,
                ),
            )
            database.sessionDao().insertSets(
                session.sets.mapIndexed { index, set ->
                    LoggedSetEntity(
                        sessionId = sessionId,
                        position = index,
                        value = set.value,
                        repsInReserve = set.repsInReserve,
                        isBestEffort = set.isBestEffort,
                    )
                },
            )
            database.gateBestDao().recordBest(
                gateId = session.gateId,
                best = session.best,
                achievedAtEpochMillis = session.finishedAtEpochMillis,
            )
        }
    }
}
