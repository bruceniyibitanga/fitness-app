package app.levitx.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Reads and writes bodyweight and bands. */
@Dao
internal interface EquipmentDao {
    /** The most recent bodyweight, or null if none has been entered. */
    @Query("SELECT kg FROM bodyweight_entry ORDER BY recordedAtEpochMillis DESC, id DESC LIMIT 1")
    fun observeLatestBodyweight(): Flow<Double?>

    /** Every bodyweight reading since [sinceEpochMillis], oldest first. Emits again when one is added. */
    @Query(
        "SELECT * FROM bodyweight_entry WHERE recordedAtEpochMillis >= :sinceEpochMillis " +
            "ORDER BY recordedAtEpochMillis",
    )
    fun observeBodyweightSince(sinceEpochMillis: Long): Flow<List<BodyweightEntity>>

    /** Every bodyweight reading since [sinceEpochMillis] now, oldest first. */
    @Query(
        "SELECT * FROM bodyweight_entry WHERE recordedAtEpochMillis >= :sinceEpochMillis " +
            "ORDER BY recordedAtEpochMillis",
    )
    suspend fun bodyweightSince(sinceEpochMillis: Long): List<BodyweightEntity>

    /** Adds a bodyweight reading. */
    @Insert
    suspend fun insertBodyweight(entry: BodyweightEntity)

    /** Every band, oldest first. */
    @Query("SELECT * FROM band ORDER BY createdAtEpochMillis")
    fun observeBands(): Flow<List<BandEntity>>

    /** Adds a band. */
    @Insert
    suspend fun insertBand(band: BandEntity)

    /** Adds several bands. */
    @Insert
    suspend fun insertBands(bands: List<BandEntity>)

    /** Removes every band. */
    @Query("DELETE FROM band")
    suspend fun deleteAllBands()

    /** Removes a band. Sets already logged with it keep their assistance value. */
    @Query("DELETE FROM band WHERE id = :id")
    suspend fun deleteBand(id: String)
}
