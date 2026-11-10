package app.levitx.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The person's best result for one gate.
 *
 * @property gateId A `Gate.id` from `:core`. Gate ids are stable, so they are safe to store.
 * @property best Reps or seconds, depending on the gate's measure.
 * @property achievedAtEpochMillis When the best was set, in UTC milliseconds.
 */
@Entity(tableName = "gate_best")
internal data class GateBestEntity(
    @PrimaryKey val gateId: String,
    val best: Int,
    val achievedAtEpochMillis: Long,
)
