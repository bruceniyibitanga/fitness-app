package app.levitx.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One bodyweight reading. Kept as a log rather than a single value, so the
 * progress screens in Phase 4 can chart it.
 *
 * @property id Row id, assigned by the database.
 * @property kg Bodyweight in kilograms.
 * @property recordedAtEpochMillis When it was recorded, in UTC milliseconds.
 */
@Entity(tableName = "bodyweight_entry")
internal data class BodyweightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kg: Double,
    val recordedAtEpochMillis: Long,
)

/**
 * A resistance band the person owns, with the help it gives.
 *
 * @property id Random UUID.
 * @property name The person's name for it, usually its colour.
 * @property assistanceKg Help the band gives, from the in-app calibration.
 * @property createdAtEpochMillis When it was added, in UTC milliseconds. Bands are listed in this order.
 */
@Entity(tableName = "band")
internal data class BandEntity(
    @PrimaryKey val id: String,
    val name: String,
    val assistanceKg: Double,
    val createdAtEpochMillis: Long,
)
