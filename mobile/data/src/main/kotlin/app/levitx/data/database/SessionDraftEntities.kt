package app.levitx.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The only row id [SessionDraftEntity] ever uses: there is one session in progress at most. */
internal const val SESSION_DRAFT_ROW_ID = 0

/**
 * The session in progress, saved after every set so it survives the app being
 * killed or crashing (added in version 4).
 *
 * @property id Always [SESSION_DRAFT_ROW_ID].
 * @property sessionId The id the finished session will be stored under.
 * @property gateId The gate being trained, a `Gate.id` from `:core`.
 * @property previousBest The person's best at the gate when the session started.
 * @property startedAtEpochMillis When the session started, in UTC milliseconds.
 * @property restSeconds Rest between sets in this session's plan.
 * @property repsInReserve Reps to leave in reserve on work sets in this session's plan.
 * @property restEndsAtElapsedMillis When the current rest ends on the elapsed clock, or null when not resting.
 * @property addedKg Vest or belt weight the next set will be logged with.
 * @property assistanceKg Band help the next set will be logged with.
 */
@Entity(tableName = "session_draft")
internal data class SessionDraftEntity(
    @PrimaryKey val id: Int = SESSION_DRAFT_ROW_ID,
    val sessionId: String,
    val gateId: String,
    val previousBest: Int,
    val startedAtEpochMillis: Long,
    val restSeconds: Int,
    val repsInReserve: Int,
    val restEndsAtElapsedMillis: Long?,
    val addedKg: Double,
    val assistanceKg: Double,
)

/**
 * One planned set of the session in progress, with what was done once it is logged.
 *
 * @property position 0-based order within the session.
 * @property target Reps or seconds to aim for.
 * @property isBestEffort True for the session's best-effort set.
 * @property value Reps or seconds done, or null while the set is still to do.
 * @property loggedRepsInReserve Reps the person felt they had left, or null.
 * @property addedKg Vest or belt weight the set was done with. 0 until logged.
 * @property assistanceKg Band help the set was done with. 0 until logged.
 */
@Entity(tableName = "session_draft_set")
internal data class SessionDraftSetEntity(
    @PrimaryKey val position: Int,
    val target: Int,
    val isBestEffort: Boolean,
    val value: Int?,
    val loggedRepsInReserve: Int?,
    val addedKg: Double,
    val assistanceKg: Double,
)

/**
 * The draft rows, read together.
 *
 * @property draft The session in progress.
 * @property sets Its planned sets, in order.
 */
internal data class SessionDraftRows(val draft: SessionDraftEntity, val sets: List<SessionDraftSetEntity>)
