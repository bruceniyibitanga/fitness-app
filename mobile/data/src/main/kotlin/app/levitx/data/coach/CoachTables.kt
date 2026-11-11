package app.levitx.data.coach

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.SessionResult
import app.levitx.core.coach.bodyweightTrend
import app.levitx.data.database.ActiveGoalEntity
import app.levitx.data.database.LevitxDatabase
import app.levitx.data.database.LoggedSetEntity
import app.levitx.data.database.TrainingSessionEntity

/*
 * Writes to the tables the rest of the app reads, so they stay in step with
 * the coach. Each runs inside the caller's transaction.
 */

/** Keeps the goal table, which the watch sync reads, in step with the profile. */
internal suspend fun LevitxDatabase.writeGoal(pathId: String?, nowEpochMillis: Long) {
    if (pathId == null) {
        goalDao().clear()
    } else {
        goalDao().replace(ActiveGoalEntity(pathId = pathId, chosenAtEpochMillis = nowEpochMillis))
    }
}

/** Stores each exercise done as a session row of the workout, with its sets. */
internal suspend fun LevitxDatabase.storeSets(workoutId: String, result: SessionResult, model: AthleteModel) {
    val bodyweightKg = bodyweightTrend(model.weighIns, result.finishedAtEpochMillis)?.currentKg
    result.results.forEachIndexed { index, exercise ->
        if (exercise.skipped || exercise.sets.isEmpty()) return@forEachIndexed
        val rowId = "$workoutId/$index"
        sessionDao().insertSession(
            TrainingSessionEntity(
                id = rowId,
                gateId = exercise.plan.exercise.id,
                startedAtEpochMillis = result.startedAtEpochMillis,
                finishedAtEpochMillis = result.finishedAtEpochMillis,
                bodyweightKg = bodyweightKg,
                workoutId = workoutId,
            ),
        )
        sessionDao().insertSets(
            exercise.sets.mapIndexed { position, set ->
                LoggedSetEntity(
                    sessionId = rowId,
                    position = position,
                    value = set.value,
                    repsInReserve = set.repsInReserve,
                    isBestEffort = set.isBestEffort,
                    addedKg = set.addedKg,
                    assistanceKg = set.assistanceKg,
                )
            },
        )
    }
}

/** Writes the gate bests the coach raised to the bests table, which the path screen and the watch read. */
internal suspend fun LevitxDatabase.raiseBests(before: AthleteModel, after: AthleteModel, at: Long) {
    after.gateBests
        .filter { (gateId, best) -> best > (before.gateBests[gateId] ?: 0) }
        .forEach { (gateId, best) -> gateBestDao().recordBest(gateId, best, at) }
}
