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
