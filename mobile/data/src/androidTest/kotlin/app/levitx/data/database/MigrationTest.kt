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

    @Test
    fun version2To3KeepsSetsAndDefaultsTheirLoad() {
        helper.createDatabase(name, 2).use { db ->
            db.execSQL(
                "INSERT INTO training_session (id, gateId, startedAtEpochMillis, finishedAtEpochMillis) " +
                    "VALUES ('s1', 'g', 0, 1)",
            )
            db.execSQL(
                "INSERT INTO logged_set (sessionId, position, value, repsInReserve, isBestEffort) " +
                    "VALUES ('s1', 0, 5, 2, 0)",
            )
        }
        helper.runMigrationsAndValidate(name, 3, true).use { db ->
            db.query("SELECT value, addedKg, assistanceKg FROM logged_set WHERE sessionId = 's1'").use { cursor ->
                check(cursor.moveToFirst()) { "set was lost in the migration" }
                check(cursor.getInt(0) == 5 && cursor.getDouble(1) == 0.0 && cursor.getDouble(2) == 0.0) {
                    "migrated set has the wrong values"
                }
            }
        }
    }

    @Test
    fun version3To4KeepsSessionsAndStartsWithNothingQueued() {
        helper.createDatabase(name, 3).use { db ->
            db.execSQL(
                "INSERT INTO training_session (id, gateId, startedAtEpochMillis, finishedAtEpochMillis) " +
                    "VALUES ('s1', 'g', 0, 1)",
            )
        }
        helper.runMigrationsAndValidate(name, 4, true).use { db ->
            db.query("SELECT COUNT(*) FROM training_session").use { cursor ->
                check(cursor.moveToFirst() && cursor.getInt(0) == 1) { "session was lost in the migration" }
            }
            db.query("SELECT COUNT(*) FROM session_outbox").use { cursor ->
                check(cursor.moveToFirst() && cursor.getInt(0) == 0) { "old sessions must not be queued for sync" }
            }
            db.query("SELECT COUNT(*) FROM session_draft").use { cursor ->
                check(cursor.moveToFirst() && cursor.getInt(0) == 0) { "there should be no session in progress" }
            }
        }
    }

    @Test
    fun version4To5KeepsSessionsAndStartsWithNoCoach() {
        helper.createDatabase(name, 4).use { db ->
            db.execSQL(
                "INSERT INTO training_session (id, gateId, startedAtEpochMillis, finishedAtEpochMillis) " +
                    "VALUES ('s1', 'g', 0, 1)",
            )
        }
        helper.runMigrationsAndValidate(name, 5, true).use { db ->
            db.query("SELECT workoutId FROM training_session WHERE id = 's1'").use { cursor ->
                check(cursor.moveToFirst() && cursor.isNull(0)) { "an old session should belong to no workout" }
            }
            db.query("SELECT COUNT(*) FROM coach_state").use { cursor ->
                check(cursor.moveToFirst() && cursor.getInt(0) == 0) { "the coach should start unset" }
            }
        }
    }

    @Test
    fun version5To6KeepsTheCoachAndStartsTheWatchTablesEmpty() {
        helper.createDatabase(name, 5).use { db ->
            db.execSQL("INSERT INTO coach_state (id, json, updatedAtEpochMillis) VALUES (0, '{}', 1)")
        }
        helper.runMigrationsAndValidate(name, 6, true).use { db ->
            db.query("SELECT COUNT(*) FROM coach_state").use { cursor ->
                check(cursor.moveToFirst() && cursor.getInt(0) == 1) { "the coach should be kept" }
            }
            db.query("SELECT COUNT(*) FROM next_session").use { cursor ->
                check(cursor.moveToFirst() && cursor.getInt(0) == 0) { "there should be no next session yet" }
            }
            db.query("SELECT COUNT(*) FROM workout_outbox").use { cursor ->
                check(cursor.moveToFirst() && cursor.getInt(0) == 0) { "nothing should be queued yet" }
            }
        }
    }
}
