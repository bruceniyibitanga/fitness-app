package app.levitx.data.backup

import app.levitx.data.coach.CoachCodec
import app.levitx.data.coach.UnreadableCoachStateException
import app.levitx.data.coach.toModel
import kotlinx.serialization.Serializable

/**
 * Format of the backup's contents. Bump it when a change can't be read by the
 * code as it was before. Adding a field with a default doesn't need a bump.
 */
internal const val BACKUP_DOCUMENT_VERSION = 1

/*
 * Limits on what a backup may hold. A backup is decrypted, so it was made with
 * the person's words, but it can still be old, damaged in a way GCM doesn't
 * see (made by a buggy build) or crafted by someone who has the words. Every
 * value is checked before anything reaches the database, the same way data
 * from the watch is. The limits sit far above years of real training.
 */
internal const val MAX_SESSIONS = 20_000
internal const val MAX_SETS_PER_SESSION = 200
internal const val MAX_WEIGH_INS = 10_000
internal const val MAX_BANDS = 50
internal const val MAX_GATE_BESTS = 1_000
internal const val MAX_ID_LENGTH = 100
internal const val MAX_COACH_CHARS = 1_048_576
internal const val MAX_SET_VALUE = 10_000
internal const val MAX_REPS_IN_RESERVE = 10
internal const val MAX_LOAD_KG = 500.0

/** Times from more than a day ahead of the phone's clock are not believed. */
private const val CLOCK_SLACK_MILLIS = 24L * 60 * 60 * 1000

/**
 * Everything a backup restores, row for row as the encrypted database holds
 * it, so a restore is a plain copy back. Not here, on purpose: the workout in
 * progress, the watch's queues and the next session (all short-lived, and
 * rebuilt), and settings, which live outside the database and are a few taps.
 *
 * @property v Format, [BACKUP_DOCUMENT_VERSION].
 * @property madeAt When the backup was made, in UTC milliseconds.
 * @property coach The coach's stored document, as stored, or null before setup.
 */
@Serializable
internal data class BackupDocument(
    val v: Int,
    val madeAt: Long,
    val coach: String?,
    val goal: BackedUpGoal? = null,
    val weighIns: List<BackedUpWeighIn> = emptyList(),
    val bands: List<BackedUpBand> = emptyList(),
    val gateBests: List<BackedUpGateBest> = emptyList(),
    val sessions: List<BackedUpSession> = emptyList(),
)

/** The goal row. */
@Serializable
internal data class BackedUpGoal(val pathId: String, val chosenAt: Long)

/** A weigh-in. */
@Serializable
internal data class BackedUpWeighIn(val kg: Double, val at: Long)

/** A resistance band. */
@Serializable
internal data class BackedUpBand(val id: String, val name: String, val assistanceKg: Double, val createdAt: Long)

/** A best at a skill gate. */
@Serializable
internal data class BackedUpGateBest(val gateId: String, val best: Int, val at: Long)

/** A session and its sets. */
@Serializable
internal data class BackedUpSession(
    val id: String,
    val gateId: String,
    val startedAt: Long,
    val finishedAt: Long,
    val bodyweightKg: Double? = null,
    val workoutId: String? = null,
    val sets: List<BackedUpSet> = emptyList(),
)

/** A logged set. */
@Serializable
internal data class BackedUpSet(
    val position: Int,
    val value: Int,
    val repsInReserve: Int? = null,
    val isBestEffort: Boolean = false,
    val addedKg: Double = 0.0,
    val assistanceKg: Double = 0.0,
)

/**
 * This document, once every value has been checked against the limits above
 * and the coach document reads as a model this version understands.
 *
 * @param nowEpochMillis The phone's time, for checking dates aren't in the future.
 * @throws BackupException with [BackupProblem.NEWER_VERSION] for a newer
 *   format, or [BackupProblem.UNREADABLE_CONTENTS] for anything else wrong.
 *   Nothing is used from a document that fails, so a restore is all or nothing.
 */
internal fun BackupDocument.checked(nowEpochMillis: Long): BackupDocument {
    if (v > BACKUP_DOCUMENT_VERSION) fail(BackupProblem.NEWER_VERSION)
    val time = 0L..nowEpochMillis + CLOCK_SLACK_MILLIS
    val valid = v >= 1 &&
        madeAt in time &&
        coachReads() &&
        (goal == null || (goal.pathId.isId() && goal.chosenAt in time)) &&
        weighInsAreValid(time) &&
        bandsAreValid(time) &&
        gateBestsAreValid(time) &&
        sessionsAreValid(time)
    if (!valid) fail(BackupProblem.UNREADABLE_CONTENTS)
    return this
}

private fun BackupDocument.weighInsAreValid(time: LongRange): Boolean =
    weighIns.size <= MAX_WEIGH_INS && weighIns.all { it.kg.isBodyweight() && it.at in time }

private fun BackupDocument.bandsAreValid(time: LongRange): Boolean = bands.size <= MAX_BANDS &&
    bands.map { it.id }.distinct().size == bands.size &&
    bands.all { it.id.isId() && it.name.isBandName() && it.assistanceKg.isLoad() && it.createdAt in time }

private fun BackupDocument.gateBestsAreValid(time: LongRange): Boolean = gateBests.size <= MAX_GATE_BESTS &&
    gateBests.map { it.gateId }.distinct().size == gateBests.size &&
    gateBests.all { it.gateId.isId() && it.best in 0..MAX_SET_VALUE && it.at in time }

private fun BackupDocument.sessionsAreValid(time: LongRange): Boolean = sessions.size <= MAX_SESSIONS &&
    sessions.map { it.id }.distinct().size == sessions.size &&
    sessions.all { it.isValid(time) }

private fun BackupDocument.coachReads(): Boolean = when {
    coach == null -> true

    coach.length > MAX_COACH_CHARS -> false

    else -> try {
        CoachCodec.decodeCoach(coach).toModel()
        true
    } catch (_: UnreadableCoachStateException) {
        false
    } catch (_: IllegalArgumentException) {
        // The model's own rules refused it, for example a goal that no longer exists.
        false
    }
}

private fun BackedUpSession.isValid(time: LongRange): Boolean = id.isId() &&
    gateId.isId() &&
    (workoutId == null || workoutId.isId()) &&
    startedAt in time &&
    finishedAt in time &&
    finishedAt >= startedAt &&
    (bodyweightKg == null || bodyweightKg.isBodyweight()) &&
    sets.size <= MAX_SETS_PER_SESSION &&
    sets.map { it.position }.distinct().size == sets.size &&
    sets.all { it.isValid() }

private fun BackedUpSet.isValid(): Boolean = position in 0 until MAX_SETS_PER_SESSION &&
    value in 0..MAX_SET_VALUE &&
    (repsInReserve == null || repsInReserve in 0..MAX_REPS_IN_RESERVE) &&
    addedKg.isLoad() &&
    assistanceKg.isLoad()
