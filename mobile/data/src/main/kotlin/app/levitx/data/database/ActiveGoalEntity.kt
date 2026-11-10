package app.levitx.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The table only ever holds this one row: there is one active goal at a time. */
internal const val ACTIVE_GOAL_ROW_ID = 0

/**
 * The goal the person is working toward.
 *
 * @property id Always [ACTIVE_GOAL_ROW_ID], so saving a new goal replaces the old one.
 * @property pathId A `SkillPath.id` from `:core`.
 * @property chosenAtEpochMillis When the goal was chosen, in UTC milliseconds.
 */
@Entity(tableName = "active_goal")
internal data class ActiveGoalEntity(
    @PrimaryKey val id: Int = ACTIVE_GOAL_ROW_ID,
    val pathId: String,
    val chosenAtEpochMillis: Long,
)
