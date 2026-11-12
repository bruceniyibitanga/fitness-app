package app.levitx.data.history

import app.levitx.core.history.ExerciseRecord
import app.levitx.core.history.WorkoutRecord
import app.levitx.core.session.LoggedSet
import app.levitx.data.database.SessionWithSets

/**
 * Groups stored rows into workouts, newest first. A coach workout is stored as
 * one row per exercise sharing a workout id, with the exercise's place in the
 * row id ("workoutId/2"). A session from before the coach, or a single gate
 * session from the watch, has no workout id and is a workout of its own.
 */
internal fun toWorkouts(rows: List<SessionWithSets>): List<WorkoutRecord> = rows
    .groupBy { it.session.workoutId ?: it.session.id }
    .map { (id, exercises) ->
        val ordered = exercises.sortedBy { it.session.id.substringAfterLast('/').toIntOrNull() ?: 0 }
        WorkoutRecord(
            id = id,
            startedAtEpochMillis = ordered.minOf { it.session.startedAtEpochMillis },
            finishedAtEpochMillis = ordered.maxOf { it.session.finishedAtEpochMillis },
            exercises = ordered.map { row ->
                ExerciseRecord(
                    exerciseId = row.session.gateId,
                    sets = row.sets.sortedBy { it.position }.map { set ->
                        LoggedSet(
                            value = set.value,
                            repsInReserve = set.repsInReserve,
                            isBestEffort = set.isBestEffort,
                            addedKg = set.addedKg,
                            assistanceKg = set.assistanceKg,
                        )
                    },
                    bodyweightKg = row.session.bodyweightKg,
                )
            },
        )
    }
    .sortedByDescending { it.finishedAtEpochMillis }
