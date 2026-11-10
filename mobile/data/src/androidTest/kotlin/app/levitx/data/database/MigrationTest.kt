package app.levitx.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Checks every schema version upgrades to the latest one with nothing lost.
 * Room checks the migrated tables match the committed schema files exactly.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val name = "migration_test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LevitxDatabase::class.java,
    )

    @Test
    fun version1To2KeepsBests() {
        helper.createDatabase(name, 1).use { db ->
            db.execSQL(
                "INSERT INTO gate_best (gateId, best, achievedAtEpochMillis) VALUES ('muscle_up.dips', 7, 0)",
            )
        }
        helper.runMigrationsAndValidate(name, 2, true).use { db ->
            db.query("SELECT best FROM gate_best WHERE gateId = 'muscle_up.dips'").use { cursor ->
                check(cursor.moveToFirst() && cursor.getInt(0) == 7) { "best was lost in the migration" }
            }
        }
    }
}
