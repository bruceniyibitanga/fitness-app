package app.levitx.data.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.levitx.data.DataUnavailableException
import java.io.File
import java.security.KeyStore
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Runs on a device or emulator, because the Android Keystore only exists there. */
@RunWith(AndroidJUnit4::class)
class DatabaseKeyStoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val alias = "test.database_key_wrapping"
    private val keyFile = File(context.cacheDir, "test_database_key.bin")

    private fun store(dispatcher: TestDispatcher) =
        DatabaseKeyStore(keyFile, dispatcher, useStrongBox = false, keystoreAlias = alias)

    @After
    fun cleanUp() {
        keyFile.delete()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias)
    }

    @Test
    fun readReturnsNullBeforeAKeyExists() = runTest {
        assertNull(store(StandardTestDispatcher(testScheduler)).read())
    }

    @Test
    fun createdKeyReadsBackTheSame() = runTest {
        val store = store(StandardTestDispatcher(testScheduler))
        val created = store.create()
        assertEquals(DATABASE_KEY_BYTES, created.size)
        assertArrayEquals(created, store.read())
    }

    @Test
    fun keyIsNotStoredInPlainForm() = runTest {
        val created = store(StandardTestDispatcher(testScheduler)).create()
        val onDisk = keyFile.readBytes()
        assertTrue(onDisk.toList().windowed(created.size).none { it == created.toList() })
    }

    @Test
    fun tamperedFileFailsClosed() = runTest {
        val store = store(StandardTestDispatcher(testScheduler))
        store.create()
        val bytes = keyFile.readBytes()
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        keyFile.writeBytes(bytes)
        // GCM's authentication tag no longer matches, so unlocking must fail.
        val error = runCatching { store.read() }.exceptionOrNull()
        assertTrue("A tampered key file must not unlock", error is DataUnavailableException)
    }

    @Test(expected = IllegalStateException::class)
    fun creatingASecondKeyIsRefused() = runTest {
        val store = store(StandardTestDispatcher(testScheduler))
        store.create()
        store.create()
    }
}
