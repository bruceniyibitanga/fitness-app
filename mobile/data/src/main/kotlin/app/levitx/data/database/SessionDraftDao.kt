package app.levitx.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

/** Reads and writes the session in progress. */
@Dao
internal abstract class SessionDraftDao {
    /** The draft row, or null when no session is in progress. */
    @Query("SELECT * FROM session_draft LIMIT 1")
    abstract suspend fun draft(): SessionDraftEntity?

    /** The draft's planned sets, in order. */
    @Query("SELECT * FROM session_draft_set ORDER BY position")
    abstract suspend fun sets(): List<SessionDraftSetEntity>

    /** Inserts the draft row. */
    @Insert
    abstract suspend fun insertDraft(draft: SessionDraftEntity)

    /** Inserts the draft's sets. */
    @Insert
    abstract suspend fun insertSets(sets: List<SessionDraftSetEntity>)

    /** Deletes the draft row. */
    @Query("DELETE FROM session_draft")
    abstract suspend fun deleteDraft()

    /** Deletes the draft's sets. */
    @Query("DELETE FROM session_draft_set")
    abstract suspend fun deleteSets()

    /** The session in progress with its sets, or null. Read in one transaction so they always match. */
    @Transaction
    open suspend fun load(): SessionDraftRows? = draft()?.let { SessionDraftRows(it, sets()) }

    /** Replaces the session in progress. One transaction: the old draft is never half overwritten. */
    @Transaction
    open suspend fun replace(rows: SessionDraftRows) {
        deleteSets()
        deleteDraft()
        insertDraft(rows.draft)
        insertSets(rows.sets)
    }

    /** Removes the session in progress. */
    @Transaction
    open suspend fun clear() {
        deleteSets()
        deleteDraft()
    }
}
