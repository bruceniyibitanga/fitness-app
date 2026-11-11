package app.levitx.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** Reads and writes [GateBestEntity] rows. Room generates the implementation. */
@Dao
internal abstract class GateBestDao {
    /** Every stored best. Emits again whenever the table changes. */
    @Query("SELECT * FROM gate_best")
    abstract fun observeAll(): Flow<List<GateBestEntity>>

    /** Every stored best now. */
    @Query("SELECT * FROM gate_best")
    abstract suspend fun all(): List<GateBestEntity>

    /** The stored best for [gateId], or null if there isn't one. */
    @Query("SELECT best FROM gate_best WHERE gateId = :gateId")
    abstract suspend fun best(gateId: String): Int?

    // Not @Upsert: Room's upsert catches the platform's constraint exception,
    // but SQLCipher throws its own type (sqlcipher-android issue #23). A
    // replace is equivalent here because the table has no foreign keys.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrReplace(entity: GateBestEntity)

    /**
     * Stores [best] for [gateId] only if it beats the current best. Runs in one
     * transaction, so two results recorded at once can't overwrite each other.
     */
    @Transaction
    open suspend fun recordBest(gateId: String, best: Int, achievedAtEpochMillis: Long) {
        val current = best(gateId)
        if (current == null || best > current) {
            insertOrReplace(GateBestEntity(gateId, best, achievedAtEpochMillis))
        }
    }
}
