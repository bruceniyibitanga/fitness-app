package app.levitx.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import app.levitx.data.DataUnavailableException
import java.io.File
import java.io.IOException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Name of the Android Keystore provider. */
private const val ANDROID_KEYSTORE = "AndroidKeyStore"

/** Keystore alias of the key that encrypts the database key. */
internal const val DATABASE_KEY_WRAPPING_ALIAS = "levitx.database_key_wrapping"

/** The database key is 256 bits, the strongest key SQLCipher uses. */
internal const val DATABASE_KEY_BYTES = 32

private const val WRAPPING_KEY_BITS = 256
private const val GCM_TAG_BITS = 128
private const val GCM_IV_BYTES = 12
private const val TRANSFORMATION = "AES/GCM/NoPadding"

/** First byte of the key file, so the format can change later without guessing. */
private const val FILE_FORMAT_VERSION: Byte = 1

/**
 * Creates and unlocks the key that encrypts the database.
 *
 * The database key is [DATABASE_KEY_BYTES] random bytes. It never touches
 * disk in plain form: it is encrypted with an AES-256-GCM key that lives in
 * the Android Keystore and can't be exported, then written to [keyFile].
 * GCM also authenticates the file, so a changed or corrupted file is
 * detected rather than producing a wrong key.
 *
 * Where the device has a StrongBox secure element, the Keystore key is kept
 * there; otherwise in the Trusted Execution Environment. Unlocking happens
 * once per app start, so StrongBox's slower operations don't matter here.
 *
 * Both functions move their work to [ioDispatcher], so they are safe to call
 * from the main thread.
 *
 * @param keyFile Where the encrypted key is stored. Use `noBackupFilesDir`.
 * @param useStrongBox True when the device has StrongBox
 *   (`PackageManager.FEATURE_STRONGBOX_KEYSTORE`).
 * @param keystoreAlias Keystore alias for the wrapping key. Tests use their own.
 */
internal class DatabaseKeyStore(
    private val keyFile: File,
    private val ioDispatcher: CoroutineDispatcher,
    private val useStrongBox: Boolean,
    private val keystoreAlias: String = DATABASE_KEY_WRAPPING_ALIAS,
) {
    /**
     * Returns the database key, or null if none has been created yet.
     *
     * @throws DataUnavailableException if a key file exists but can't be
     *   decrypted, for example because its Keystore key is gone.
     */
    suspend fun read(): ByteArray? = withContext(ioDispatcher) {
        if (!keyFile.exists()) return@withContext null
        keyOperation("The database key couldn't be unlocked") {
            val wrappingKey = existingWrappingKey()
                ?: throw GeneralSecurityException("The Keystore key that unlocks the database is missing")
            decrypt(AtomicFile(keyFile).readFully(), wrappingKey)
        }
    }

    /**
     * Makes a new random database key, stores it encrypted and returns it.
     *
     * @throws IllegalStateException if a key already exists. Replacing it
     *   would make the existing database unreadable, so that is always a bug.
     * @throws DataUnavailableException if the key can't be protected or saved.
     */
    suspend fun create(): ByteArray = withContext(ioDispatcher) {
        check(!keyFile.exists()) { "A database key already exists" }
        val databaseKey = ByteArray(DATABASE_KEY_BYTES).also { SecureRandom().nextBytes(it) }
        keyOperation("The database key couldn't be saved") {
            writeAtomically(encrypt(databaseKey, generateWrappingKey()))
        }
        databaseKey
    }

    private fun existingWrappingKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(keystoreAlias, null) as SecretKey?
    }

    private fun generateWrappingKey(): SecretKey {
        val spec = KeyGenParameterSpec.Builder(
            keystoreAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(WRAPPING_KEY_BITS)
            .setIsStrongBoxBacked(useStrongBox)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    /** File layout: format version (1 byte), IV length (1 byte), IV, ciphertext with GCM tag. */
    private fun encrypt(plain: ByteArray, wrappingKey: SecretKey): ByteArray {
        // The Keystore picks a fresh random IV for every encryption.
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, wrappingKey) }
        val iv = cipher.iv
        val sealed = cipher.doFinal(plain)
        return ByteBuffer.allocate(2 + iv.size + sealed.size)
            .put(FILE_FORMAT_VERSION)
            .put(iv.size.toByte())
            .put(iv)
            .put(sealed)
            .array()
    }

    private fun decrypt(file: ByteArray, wrappingKey: SecretKey): ByteArray {
        val buffer = ByteBuffer.wrap(file)
        val version = buffer.get()
        val ivLength = buffer.get().toInt()
        if (version != FILE_FORMAT_VERSION || ivLength != GCM_IV_BYTES) {
            throw GeneralSecurityException("The database key file has an unknown format")
        }
        val iv = ByteArray(ivLength).also { buffer.get(it) }
        val sealed = ByteArray(buffer.remaining()).also { buffer.get(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, wrappingKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        return cipher.doFinal(sealed)
    }

    /** Writes the whole file or nothing, so a crash mid-write can't leave half a key. */
    private fun writeAtomically(bytes: ByteArray) {
        val file = AtomicFile(keyFile)
        val stream = file.startWrite()
        try {
            stream.write(bytes)
            file.finishWrite(stream)
        } catch (e: IOException) {
            file.failWrite(stream)
            throw e
        }
    }

    /**
     * Runs a key operation and turns the ways it can fail (Keystore errors,
     * a bad tag, a short or unreadable file) into [DataUnavailableException].
     * Any other exception is a bug and passes through unchanged.
     */
    private inline fun <T> keyOperation(failureMessage: String, block: () -> T): T =
        runCatching(block).getOrElse { error ->
            throw if (error.isKeyFailure()) DataUnavailableException(failureMessage, error) else error
        }

    private fun Throwable.isKeyFailure(): Boolean =
        this is GeneralSecurityException || this is IOException || this is BufferUnderflowException
}
