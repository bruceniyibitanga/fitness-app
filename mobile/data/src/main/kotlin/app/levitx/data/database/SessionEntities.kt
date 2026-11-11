package app.levitx.data.database

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * A finished training session.
 *
 * A coach workout with several exercises is stored as one row per exercise,
 * sharing a [workoutId].
 *
 * @property id Random UUID, so ids never clash when sessions sync between phone and watch.
 * @property gateId The gate trained, a `Gate.id` from `:core`. For coach workouts
 *   it is any exercise id, which is a gate id for skill work.
 * @property startedAtEpochMillis When it started, in UTC milliseconds.
 * @property finishedAtEpochMillis When it finished, in UTC milliseconds.
 * @property bodyweightKg Bodyweight at the time, if known (added in version 3).
 * @property workoutId The coach workout this was part of, or null for a single-gate
 *   session from before the coach or from the watch (added in version 5).
 */
@Entity(tableName = "training_session", indices = [Index("workoutId")])
internal data class TrainingSessionEntity(
    @PrimaryKey val id: String,
    val gateId: String,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long,
    val bodyweightKg: Double? = null,
    @ColumnInfo(defaultValue = "NULL") val workoutId: String? = null,
)

/**
 * One set within a [TrainingSessionEntity]. Deleted with its session.
 *
 * @property sessionId The session it belongs to.
 * @property position 0-based order within the session.
 * @property value Reps or seconds done.
 * @property repsInReserve Reps the person felt they had left, or null.
 * @property isBestEffort True for the session's best-effort set.
 * @property addedKg Vest or belt weight (added in version 3; 0 for older rows).
 * @property assistanceKg Band assistance (added in version 3; 0 for older rows).
 */
@Entity(
    tableName = "logged_set",
    primaryKeys = ["sessionId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = TrainingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
internal data class LoggedSetEntity(
    val sessionId: String,
    val position: Int,
    val value: Int,
    val repsInReserve: Int?,
    val isBestEffort: Boolean,
    @ColumnInfo(defaultValue = "0") val addedKg: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val assistanceKg: Double = 0.0,
)

/**
 * A session with its sets, as Room loads them together.
 *
 * @property session The session row.
 * @property sets Its sets, in no particular order; sort by position.
 */
internal data class SessionWithSets(
    @Embedded val session: TrainingSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId") val sets: List<LoggedSetEntity>,
)

/**
 * Marks a session that still has to reach the other device (added in version 4).
 *
 * The row is written in the same transaction as the session, so a session can
 * never be saved without being queued. It is removed once the other device
 * confirms it has the session.
 *
 * @property sessionId The queued session. Deleted with it.
 * @property queuedAtEpochMillis When it was queued, in UTC milliseconds. Sessions are sent oldest first.
 */
@Entity(
    tableName = "session_outbox",
    foreignKeys = [
        ForeignKey(
            entity = TrainingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
internal data class SessionOutboxEntity(@PrimaryKey val sessionId: String, val queuedAtEpochMillis: Long)
