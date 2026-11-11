package app.levitx.data.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Checks the database is really encrypted on disk, and that bests only go up. */
@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "test_encrypted.db"
    private lateinit var database: LevitxDatabase

    @Before
    fun open() {
        System.loadLibrary("sqlcipher")
        val key = ByteArray(32) { it.toByte() }
        database = Room.databaseBuilder(context, LevitxDatabase::class.java, name)
            .openHelperFactory(SupportOpenHelperFactory(key.toSqlCipherRawKey()))
            .build()
    }

    @After
    fun close() {
        database.close()
        context.deleteDatabase(name)
    }

    @Test
    fun fileOnDiskIsNotPlainSqlite() = runTest {
        database.gateBestDao().recordBest("muscle_up.dead_hang", 30, 0)
        // Every unencrypted SQLite file starts with this header.
        val header = "SQLite format 3".toByteArray()
        val start = context.getDatabasePath(name).readBytes().copyOf(header.size)
        assertFalse(start.contentEquals(header))
    }

    @Test
    fun weakerResultDoesNotReplaceBest() = runTest {
        val dao = database.gateBestDao()
        dao.recordBest("muscle_up.pull_ups", 8, 0)
        dao.recordBest("muscle_up.pull_ups", 5, 1)
        assertEquals(8, dao.observeAll().first().single().best)
    }
}
