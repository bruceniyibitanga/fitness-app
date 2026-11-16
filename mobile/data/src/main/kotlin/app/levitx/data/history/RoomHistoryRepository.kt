package app.levitx.data.history

import app.levitx.core.history.WorkoutRecord
import app.levitx.data.DefaultDispatcher
import app.levitx.data.database.LevitxDatabaseProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Reads finished workouts from the encrypted database.
 *
 * Room runs the query on its own threads and re-emits when a workout is
 * stored. Grouping rows into workouts runs on [defaultDispatcher], off the
 * main thread.
 */
@Singleton
internal class RoomHistoryRepository @Inject constructor(
    private val databaseProvider: LevitxDatabaseProvider,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : HistoryRepository {
    override fun observeWorkouts(): Flow<List<WorkoutRecord>> = flow {
        emitAll(databaseProvider.database().sessionDao().observeAllWithSets())
    }
        .map(::toWorkouts)
        .flowOn(defaultDispatcher)
        .distinctUntilChanged()
}
