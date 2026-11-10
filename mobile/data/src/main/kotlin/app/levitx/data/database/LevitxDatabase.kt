package app.levitx.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The app's local database. Encrypted with SQLCipher; see [LevitxDatabaseProvider].
 *
 * When the schema changes, bump [version], add a migration and commit the new
 * schema file Room writes to `data/schemas/`. CI fails if that file is missing.
 */
@Database(
    entities = [
        GateBestEntity::class,
        ActiveGoalEntity::class,
        TrainingSessionEntity::class,
        LoggedSetEntity::class,
        BodyweightEntity::class,
        BandEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        // Version 2 only adds tables, which Room can migrate by itself.
        AutoMigration(from = 1, to = 2),
        // Version 3 adds tables and columns with defaults, which Room can also migrate by itself.
        AutoMigration(from = 2, to = 3),
    ],
)
internal abstract class LevitxDatabase : RoomDatabase() {
    /** Access to personal bests per gate. */
    abstract fun gateBestDao(): GateBestDao

    /** Access to the active goal. */
    abstract fun goalDao(): GoalDao

    /** Access to finished sessions and their sets. */
    abstract fun sessionDao(): SessionDao

    /** Access to bodyweight and bands. */
    abstract fun equipmentDao(): EquipmentDao
}
