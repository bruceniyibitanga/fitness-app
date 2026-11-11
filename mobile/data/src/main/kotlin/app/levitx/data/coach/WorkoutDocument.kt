package app.levitx.data.coach

import app.levitx.core.coach.ExerciseLookup
import app.levitx.core.coach.ExercisePlan
import app.levitx.core.coach.Intensity
import app.levitx.core.coach.PlannedSession
import app.levitx.core.coach.SuggestionChoice
import app.levitx.core.coach.WorkoutRun
import app.levitx.core.session.LoggedSet
import app.levitx.core.session.PlannedSet
import app.levitx.core.session.SetLoad
import kotlinx.serialization.Serializable

/** Format of the stored workout in progress. A draft that can't be read is thrown away, so no steps are kept. */
internal const val WORKOUT_DOCUMENT_VERSION = 1

/**
 * A stored `WorkoutRun`. Exercises are kept as ids and looked up again when it
 * is read, so the exercise catalogue stays the one place they are described.
 */
@Serializable
internal data class WorkoutDocument(
    val v: Int,
    val id: String,
    val startedAt: Long,
    val choice: String? = null,
    val plans: List<StoredPlan>,
    val logged: List<List<StoredSet>>,
    val skipped: List<Int> = emptyList(),
    val exerciseIndex: Int,
    val restEndsAt: Long? = null,
    val addedKg: Double = 0.0,
    val assistanceKg: Double = 0.0,
)

/** A stored `ExercisePlan`. */
@Serializable
internal data class StoredPlan(
    val exerciseId: String,
    val sets: List<StoredPlannedSet>,
    val restSeconds: Int,
    val repsInReserve: Int,
    val intensity: String,
    val gateId: String? = null,
)

/** A stored `PlannedSet`. */
@Serializable
internal data class StoredPlannedSet(val target: Int, val isBestEffort: Boolean)

/** A stored `LoggedSet`. */
@Serializable
internal data class StoredSet(
    val value: Int,
    val repsInReserve: Int? = null,
    val isBestEffort: Boolean,
    val addedKg: Double = 0.0,
    val assistanceKg: Double = 0.0,
)

/** The document for [run]. */
internal fun workoutDocumentOf(run: WorkoutRun): WorkoutDocument = WorkoutDocument(
    v = WORKOUT_DOCUMENT_VERSION,
    id = run.id,
    startedAt = run.startedAtEpochMillis,
    choice = run.choice?.name,
    plans = run.session.plans.map { plan ->
        StoredPlan(
            exerciseId = plan.exercise.id,
            sets = plan.sets.map { StoredPlannedSet(it.target, it.isBestEffort) },
            restSeconds = plan.restSeconds,
            repsInReserve = plan.repsInReserve,
            intensity = plan.intensity.name,
            gateId = plan.gateId,
        )
    },
    logged = run.logged.map { sets ->
        sets.map { StoredSet(it.value, it.repsInReserve, it.isBestEffort, it.addedKg, it.assistanceKg) }
    },
    skipped = run.skipped.sorted(),
    exerciseIndex = run.exerciseIndex,
    restEndsAt = run.restEndsAtElapsedMillis,
    addedKg = run.load.addedKg,
    assistanceKg = run.load.assistanceKg,
)

/**
 * The workout this document describes, or null when it can't be rebuilt: an
 * exercise this version doesn't know, or values that break the run's rules.
 */
internal fun WorkoutDocument.toRunOrNull(): WorkoutRun? = runCatching {
    val session = PlannedSession(
        plans.map { stored ->
            ExercisePlan(
                exercise = checkNotNull(ExerciseLookup.byId(stored.exerciseId)),
                sets = stored.sets.map { PlannedSet(it.target, it.isBestEffort) },
                restSeconds = stored.restSeconds,
                repsInReserve = stored.repsInReserve,
                intensity = checkNotNull(stored.intensity.toEnumOrNull<Intensity>()),
                gateId = stored.gateId,
            )
        },
    )
    WorkoutRun(
        id = id,
        session = session,
        startedAtEpochMillis = startedAt,
        choice = choice?.toEnumOrNull<SuggestionChoice>(),
        logged = logged.map { sets ->
            sets.map { LoggedSet(it.value, it.repsInReserve, it.isBestEffort, it.addedKg, it.assistanceKg) }
        },
        skipped = skipped.toSet(),
        exerciseIndex = exerciseIndex,
        restEndsAtElapsedMillis = restEndsAt,
        load = SetLoad(addedKg, assistanceKg),
    )
}.getOrNull()
