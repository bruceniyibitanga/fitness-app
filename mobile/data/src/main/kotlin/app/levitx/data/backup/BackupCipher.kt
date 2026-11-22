package app.levitx.data.backup

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** "LVXB": marks a Levitx backup, so another file is turned away before any work. */
private val MAGIC = byteArrayOf(0x4C, 0x56, 0x58, 0x42)

/** The only layout so far. A file with another number is from a newer Levitx. */
private const val FORMAT: Byte = 1

/** PBKDF2 with HMAC-SHA256: the only key function so far. */
private const val KDF_PBKDF2_SHA256: Byte = 1

/**
 * Rounds of PBKDF2, from OWASP's password storage guidance for HMAC-SHA256.
 * Fixed rather than read from the file, so a changed file can't make the
 * phone do less work (a weaker key) or far more (a frozen phone).
 */
internal const val PBKDF2_ROUNDS = 600_000

private const val KEY_BITS = 256
private const val SALT_BYTES = 16
private const val NONCE_BYTES = 12
private const val TAG_BITS = 128
private const val TAG_BYTES = TAG_BITS / Byte.SIZE_BITS
private const val INT_BYTES = Int.SIZE_BYTES

/** Magic, format, key function, rounds, salt and nonce. Authenticated with the contents. */
internal const val HEADER_BYTES = 4 + 1 + 1 + INT_BYTES + SALT_BYTES + NONCE_BYTES

/**
 * The largest backup file read. Years of training are a few megabytes at
 * most; anything far bigger is not a backup and isn't read into memory.
 */
internal const val MAX_FILE_BYTES = 32 * 1024 * 1024

private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val KDF = "PBKDF2WithHmacSHA256"

/** Why a backup couldn't be opened. Never says which word was wrong: it can't know. */
public enum class BackupProblem {
    /** Not a Levitx backup, or cut short. */
    NOT_A_BACKUP,

    /** Made by a newer Levitx: update the app first. */
    NEWER_VERSION,

    /** The words don't open it, or the file was changed. GCM can't tell these apart. */
    WRONG_WORDS_OR_DAMAGED,

    /** It opened, but what's inside breaks the app's limits, so none of it is used. */
    UNREADABLE_CONTENTS,

    /** Bigger than any real backup. */
    TOO_LARGE,
}

/**
 * A backup that couldn't be opened, and why.
 *
 * @property problem Why, for the screen to say.
 */
public class BackupException(public val problem: BackupProblem, cause: Throwable? = null) :
    Exception(problem.name, cause)

/**
 * Encrypts and decrypts backup files (ADR 0009).
 *
 * Layout: `LVXB`, format (1 byte), key function (1 byte), rounds (4 bytes,
 * big-endian), salt (16), nonce (12), then the AES-256-GCM ciphertext with
 * its 16-byte tag. The header is passed to GCM as additional data, so
 * changing any byte of it fails the tag like changing the contents does.
 *
 * Each backup gets a fresh salt and nonce, so each has its own key and a
 * nonce never repeats under one key. Only `javax.crypto` from the platform
 * is used. Key derivation is slow on purpose (about a second on a phone), so
 * callers run it off the main thread.
 */
internal object BackupCipher {
    /** [plain] locked with [words]. */
    fun encrypt(plain: ByteArray, words: List<String>, random: SecureRandom = SecureRandom()): ByteArray {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val header = ByteBuffer.allocate(HEADER_BYTES)
            .put(MAGIC).put(FORMAT).put(KDF_PBKDF2_SHA256).putInt(PBKDF2_ROUNDS).put(salt).put(nonce)
            .array()
        val key = deriveKey(words, salt)
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(header)
            return header + cipher.doFinal(plain)
        } finally {
            key.fill(0)
        }
    }

    /**
     * The contents of [file], unlocked with [words].
     *
     * @throws BackupException when the file isn't a backup this version can
     *   read, or the words don't open it.
     */
    fun decrypt(file: ByteArray, words: List<String>): ByteArray {
        if (file.size > MAX_FILE_BYTES) fail(BackupProblem.TOO_LARGE)
        if (file.size < HEADER_BYTES + TAG_BYTES || !file.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) {
            fail(BackupProblem.NOT_A_BACKUP)
        }
        val header = ByteBuffer.wrap(file, 0, HEADER_BYTES)
        header.position(MAGIC.size)
        val format = header.get()
        val kdf = header.get()
        val rounds = header.getInt()
        if (format > FORMAT) fail(BackupProblem.NEWER_VERSION)
        // Format 1 has one key function and one round count. Anything else was edited.
        if (format != FORMAT || kdf != KDF_PBKDF2_SHA256 || rounds != PBKDF2_ROUNDS) {
            fail(BackupProblem.WRONG_WORDS_OR_DAMAGED)
        }
        val salt = ByteArray(SALT_BYTES).also { header.get(it) }
        val nonce = ByteArray(NONCE_BYTES).also { header.get(it) }
        val key = deriveKey(words, salt)
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(file, 0, HEADER_BYTES)
            return cipher.doFinal(file, HEADER_BYTES, file.size - HEADER_BYTES)
        } catch (error: GeneralSecurityException) {
            // A wrong key and a changed file both fail the tag (AEADBadTagException).
            fail(BackupProblem.WRONG_WORDS_OR_DAMAGED, error)
        } finally {
            key.fill(0)
        }
    }

    /**
     * A 256-bit key from [words] and [salt]: the words joined by single
     * spaces, lowercase, as UTF-8 characters. The characters are wiped once
     * the key is made; the caller wipes the key.
     */
    private fun deriveKey(words: List<String>, salt: ByteArray): ByteArray {
        require(words.size == RecoveryWords.COUNT) { "a backup needs ${RecoveryWords.COUNT} words" }
        val secret = words.joinToString(" ") { it.lowercase() }.toCharArray()
        val spec = PBEKeySpec(secret, salt, PBKDF2_ROUNDS, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance(KDF).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            Arrays.fill(secret, '\u0000')
        }
    }
}

/** Stops reading a backup because of [problem]. */
internal fun fail(problem: BackupProblem, cause: Throwable? = null): Nothing = throw BackupException(problem, cause)
