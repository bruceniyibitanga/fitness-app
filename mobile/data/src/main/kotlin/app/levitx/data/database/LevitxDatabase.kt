package app.levitx.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The app's local database. Encrypted with SQLCipher; see [LevitxDatabaseProvider].
 *
 * When the schema changes, bump [version], add a migration and commit the new
 * schema file Room writes to `data/schemas/`. CI fails if that file is missing.
 */
@Database(entities = [GateBestEntity::class], version = 1, exportSchema = true)
internal abstract class LevitxDatabase : RoomDatabase() {
    /** Access to personal bests per gate. */
    abstract fun gateBestDao(): GateBestDao
}
