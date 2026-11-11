package app.levitx.data.coach

import androidx.room.withTransaction
import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.BodyArea
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.CoachTuning
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.SessionResult
import app.levitx.core.pain.PainAssessment
import app.levitx.data.database.CoachStateEntity
import app.levitx.data.database.LevitxDatabase
import app.levitx.data.database.LevitxDatabaseProvider
import app.levitx.data.time.TrainingClock
import java.time.DayOfWeek
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1_000

/** Bodyweight readings older than this are no use to the coach's trend. */
private const val BODYWEIGHT_DAYS = CoachTuning.HISTORY_DAYS

/** Stores the coach's state in the encrypted database. */
@Singleton
internal class RoomCoachRepository @Inject constructor(
    private val databaseProvider: LevitxDatabaseProvider,
    private val coach: Coach,
    private val clock: TrainingClock,
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
    }.distinctUntilChanged()

    override suspend fun start(profile: CoachProfile, pullUp: PullUpAnswer, trainingDays: Set<DayOfWeek>) {
        val database = databaseProvider.database()
        val now = clock.currentTimeMillis()
        database.withTransaction {
            database.write(AthleteModel.starting(profile, pullUp), trainingDays, now)
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

    override suspend fun recordWorkout(workoutId: String, result: SessionResult): Boolean {
        val database = databaseProvider.database()
        return database.withTransaction {
            if (database.sessionDao().hasWorkout(workoutId)) {
                false
            } else {
                val now = clock.currentTimeMillis()
                val current = database.load(now) ?: error("The coach isn't set up")
                val updated = coach.record(current.model, result)
                database.storeSets(workoutId, result, current.model)
                database.raiseBests(before = current.model, after = updated, at = result.finishedAtEpochMillis)
                database.write(updated, current.trainingDays, now)
                database.coachDao().clearDraft()
                true
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

    override suspend fun markWeightNoticeShown() = update { it.withWeightNoticeShown() }

    /** Reads the model, changes it with [change] and writes it back, in one transaction. */
    private suspend fun update(change: (AthleteModel) -> AthleteModel) {
        val database = databaseProvider.database()
        val now = clock.currentTimeMillis()
        database.withTransaction {
            val current = database.load(now) ?: error("The coach isn't set up")
            database.write(change(current.model), current.trainingDays, now)
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
