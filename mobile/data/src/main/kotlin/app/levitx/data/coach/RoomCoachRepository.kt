package app.levitx.data.coach

import androidx.room.withTransaction
import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.BodyArea
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.CoachTuning
import app.levitx.core.coach.ExerciseLookup
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.QuickTest
import app.levitx.core.coach.SessionResult
import app.levitx.core.coach.withPreferred
import app.levitx.core.history.Forgetting
import app.levitx.core.pain.PainAssessment
import app.levitx.data.DefaultDispatcher
import app.levitx.data.database.CoachStateEntity
import app.levitx.data.database.LevitxDatabase
import app.levitx.data.database.LevitxDatabaseProvider
import app.levitx.data.history.toWorkouts
import app.levitx.data.time.TrainingClock
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1_000

/** Bodyweight readings older than this are no use to the coach's trend. */
private const val BODYWEIGHT_DAYS = CoachTuning.HISTORY_DAYS

/** Stores the coach's state in the encrypted database. */
@Singleton
internal class RoomCoachRepository @Inject constructor(
    private val databaseProvider: LevitxDatabaseProvider,
    private val coach: Coach,
    private val clock: TrainingClock,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : CoachRepository {
    override fun observeState(): Flow<CoachState?> = flow {
        val database = databaseProvider.database()
        val since = clock.currentTimeMillis() - BODYWEIGHT_DAYS * MILLIS_PER_DAY
        emitAll(
            combine(
                database.coachDao().observeState(),
                database.gateBestDao().observeAll(),
                database.equipmentDao().observeBodyweightSince(since),
            ) { row, bests, bodyweights ->
                row?.let { assembleCoachState(CoachCodec.decodeCoach(it.json), bests, bodyweights) }
            },
        )
    }
        // Reading the document and merging the tables is real work, so it is kept off the caller's thread.
        .flowOn(defaultDispatcher)
        .distinctUntilChanged()

    override suspend fun start(
        profile: CoachProfile,
        pullUp: PullUpAnswer,
        trainingDays: Set<DayOfWeek>,
        tested: Map<QuickTest, Int>,
    ) {
        val database = databaseProvider.database()
        val now = clock.currentTimeMillis()
        database.withTransaction {
            database.write(AthleteModel.starting(profile, pullUp, tested), trainingDays, now)
            database.writeGoal(profile.goalPathId, now)
        }
    }

    override suspend fun updateProfile(profile: CoachProfile, trainingDays: Set<DayOfWeek>) {
        val database = databaseProvider.database()
        val now = clock.currentTimeMillis()
        database.withTransaction {
            val current = database.load(now) ?: error("The coach isn't set up")
            database.write(current.model.copy(profile = profile), trainingDays, now)
            database.writeGoal(profile.goalPathId, now)
        }
    }

    override suspend fun recordWorkout(workoutId: String, result: SessionResult): Recorded {
        val database = databaseProvider.database()
        return database.withTransaction {
            // Both read inside the transaction, so neither can change between the check and the write.
            if (database.sessionDao().hasWorkout(workoutId)) {
                Recorded.ALREADY_STORED
            } else {
                val now = clock.currentTimeMillis()
                val current = database.load(now)
                if (current == null) {
                    Recorded.NOT_SET_UP
                } else {
                    // Nothing is stored as finishing in the future, whatever the clock that timed it said.
                    val dated = result.endingBy(now)
                    val updated = coach.record(current.model, dated)
                    database.storeSets(workoutId, dated, current.model)
                    database.raiseBests(before = current.model, after = updated, at = dated.finishedAtEpochMillis)
                    database.write(updated, current.trainingDays, now)
                    database.coachDao().clearDraft()
                    Recorded.NEW
                }
            }
        }
    }

    override fun observeRecentWorkoutIds(): Flow<List<String>> = flow {
        emitAll(databaseProvider.database().sessionDao().observeRecentWorkoutIds(RECENT_WORKOUTS))
    }.distinctUntilChanged()

    override suspend fun recordRest(nowEpochMillis: Long) = update { coach.recordRest(it, nowEpochMillis) }

    override suspend fun recordPain(area: BodyArea, assessment: PainAssessment) = update {
        it.withPain(area, assessment)
    }

    override suspend fun prefer(exerciseId: String) = update { model ->
        ExerciseLookup.byId(exerciseId)?.let(model::withPreferred) ?: model
    }

    override suspend fun markShown(prompt: CoachPrompt, atEpochMillis: Long) =
        update { it.withShown(prompt, atEpochMillis) }

    override suspend fun deleteWorkout(workoutId: String): Boolean {
        val database = databaseProvider.database()
        return database.withTransaction {
            val workouts = toWorkouts(database.sessionDao().allWithSets())
            val workout = workouts.firstOrNull { it.id == workoutId }
            val now = clock.currentTimeMillis()
            val current = database.load(now)
            if (workout == null || current == null) {
                false
            } else {
                val remaining = workouts - workout
                val updated = Forgetting.forget(current.model, workout, remaining)
                database.sessionDao().deleteWorkout(workoutId)
                database.resetBests(current.model.gateBests, updated.gateBests, remaining)
                database.write(updated, current.trainingDays, now)
                true
            }
        }
    }

    /**
     * Reads the model, changes it with [change] and writes it back, in one
     * transaction. With no coach set up there is nothing to change: a tap
     * that lands just after everything was deleted is dropped, not a crash.
     */
    private suspend fun update(change: (AthleteModel) -> AthleteModel) {
        val database = databaseProvider.database()
        val now = clock.currentTimeMillis()
        database.withTransaction {
            database.load(now)?.let { current ->
                database.write(change(current.model), current.trainingDays, now)
            }
        }
    }
}

/** The state at [nowEpochMillis], or null before setup. Call inside a transaction. */
private suspend fun LevitxDatabase.load(nowEpochMillis: Long): CoachState? {
    val row = coachDao().state() ?: return null
    val since = nowEpochMillis - BODYWEIGHT_DAYS * MILLIS_PER_DAY
    return assembleCoachState(
        CoachCodec.decodeCoach(row.json),
        gateBestDao().all(),
        equipmentDao().bodyweightSince(since),
    )
}

/** Writes [model] and [trainingDays] as the coach's state, stamped [nowEpochMillis]. */
private suspend fun LevitxDatabase.write(model: AthleteModel, trainingDays: Set<DayOfWeek>, nowEpochMillis: Long) {
    val json = CoachCodec.encode(coachDocumentOf(model, trainingDays))
    coachDao().replaceState(CoachStateEntity(json = json, updatedAtEpochMillis = nowEpochMillis))
}
