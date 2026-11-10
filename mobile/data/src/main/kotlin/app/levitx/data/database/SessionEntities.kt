package app.levitx.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A finished training session.
 *
 * @property id Random UUID, so ids never clash when sessions sync between phone and watch.
 * @property gateId The gate trained, a `Gate.id` from `:core`.
 * @property startedAtEpochMillis When it started, in UTC milliseconds.
 * @property finishedAtEpochMillis When it finished, in UTC milliseconds.
 */
@Entity(tableName = "training_session")
internal data class TrainingSessionEntity(
    @PrimaryKey val id: String,
    val gateId: String,
    val startedAtEpochMillis: Long,
    val finishedAtEpochMillis: Long,
)

/**
 * One set within a [TrainingSessionEntity]. Deleted with its session.
 *
 * @property sessionId The session it belongs to.
 * @property position 0-based order within the session.
 * @property value Reps or seconds done.
 * @property repsInReserve Reps the person felt they had left, or null.
 * @property isBestEffort True for the session's best-effort set.
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
)
