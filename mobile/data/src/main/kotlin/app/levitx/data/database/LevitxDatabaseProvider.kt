package app.levitx.data.database

import android.content.Context
import androidx.room.Room
import app.levitx.data.DataUnavailableException
import app.levitx.data.IoDispatcher
import app.levitx.data.security.DatabaseKeyStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Inject
import javax.inject.Singleton

/** File name of the database inside the app's private databases folder. */
internal const val DATABASE_NAME = "levitx.db"

/**
 * Opens the encrypted database the first time it is needed, then hands out
 * the same instance.
 *
 * Opening means unlocking the key and loading SQLCipher's native library, so
 * it runs on the I/O dispatcher and only once. Callers just `suspend`.
 *
 * Fails closed: if the database file exists but its key doesn't, it throws
 * [DataUnavailableException] instead of creating a new key, which would make
 * the existing data unreadable for good.
 */
@Singleton
internal class LevitxDatabaseProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val keyStore: DatabaseKeyStore,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val mutex = Mutex()

    @Volatile
    private var database: LevitxDatabase? = null

    /** The open database. Opens it on first call. */
    suspend fun database(): LevitxDatabase = database ?: mutex.withLock {
        database ?: open().also { database = it }
    }

    private suspend fun open(): LevitxDatabase = withContext(ioDispatcher) {
        val key = keyStore.read() ?: run {
            if (context.getDatabasePath(DATABASE_NAME).exists()) {
                throw DataUnavailableException("The database exists but its key is missing")
            }
            keyStore.create()
        }
        System.loadLibrary("sqlcipher")
        Room.databaseBuilder(context, LevitxDatabase::class.java, DATABASE_NAME)
            .openHelperFactory(SupportOpenHelperFactory(key.toSqlCipherRawKey()))
            .build()
    }
}

/**
 * Formats a 256-bit key as SQLCipher's raw key string, `x'<64 hex digits>'`.
 *
 * A raw key is used as the encryption key directly. SQLCipher skips its
 * password stretching (PBKDF2), which exists to slow down guessing of human
 * passwords and adds nothing for 32 random bytes except a slower start.
 */
internal fun ByteArray.toSqlCipherRawKey(): ByteArray {
    val hex = joinToString(separator = "") { byte ->
        val value = byte.toInt() and BYTE_MASK
        "${HEX_DIGITS[value shr NIBBLE_BITS]}${HEX_DIGITS[value and NIBBLE_MASK]}"
    }
    return "x'$hex'".toByteArray(Charsets.US_ASCII)
}

private const val HEX_DIGITS = "0123456789abcdef"
private const val BYTE_MASK = 0xFF
private const val NIBBLE_BITS = 4
private const val NIBBLE_MASK = 0x0F
