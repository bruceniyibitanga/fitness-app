package app.levitx.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The only row id the coach tables use: there is one person, one coach state and one workout in progress. */
internal const val COACH_ROW_ID = 0

/**
 * What the coach has learned about the person, as a JSON document (added in version 5).
 *
 * One document rather than a table per field, because the coach reads and
 * writes it whole and it is small. The document has its own format version;
 * see `CoachDocument` in the `coach` package. Bests and bodyweight are not in
 * it: they stay in their own tables, which the rest of the app already reads.
 *
 * @property id Always [COACH_ROW_ID].
 * @property json The document.
 * @property updatedAtEpochMillis When it was last written, in UTC milliseconds.
 */
@Entity(tableName = "coach_state")
internal data class CoachStateEntity(
    @PrimaryKey val id: Int = COACH_ROW_ID,
    val json: String,
    val updatedAtEpochMillis: Long,
)

/**
 * The workout in progress, as a JSON document saved after every set, so it
 * survives the app being killed (added in version 5).
 *
 * @property id Always [COACH_ROW_ID].
 * @property json The document; see `WorkoutDocument` in the `coach` package.
 */
@Entity(tableName = "workout_draft")
internal data class WorkoutDraftEntity(@PrimaryKey val id: Int = COACH_ROW_ID, val json: String)

/**
 * The next session the phone's coach planned, as the watch keeps it, so the
 * watch can run it with the phone out of reach (added in version 6). Only
 * the plan crosses: exercises, sets and targets, no health data.
 *
 * @property id Always [COACH_ROW_ID].
 * @property json The plan (see `PlanDocument` in the `coach` package), or null when there is none.
 * @property awaitingWorkoutId A workout finished on the watch that the phone's
 *   coach hasn't recorded yet, or null. Until it has, the phone's plan is from
 *   before that workout, so it isn't taken.
 */
@Entity(tableName = "next_session")
internal data class NextSessionEntity(
    @PrimaryKey val id: Int = COACH_ROW_ID,
    val json: String?,
    val awaitingWorkoutId: String? = null,
)

/**
 * A workout finished on this device that the other device hasn't confirmed
 * yet (added in version 6). It is deleted once confirmed.
 *
 * @property id The workout's id, a random UUID.
 * @property json The workout; see `WorkoutDocument` in the `coach` package.
 * @property finishedAtEpochMillis When it finished, in UTC milliseconds.
 * @property queuedAtEpochMillis When it was queued, for sending oldest first.
 */
@Entity(tableName = "workout_outbox")
internal data class WorkoutOutboxEntity(
    @PrimaryKey val id: String,
    val json: String,
    val finishedAtEpochMillis: Long,
    val queuedAtEpochMillis: Long,
)
