package app.levitx.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Test

class BackupCipherTest {
    private val words = listOf("abacus", "zoom", "t-shirt", "abstract", "yo-yo", "felt-tip")
    private val other = listOf("abacus", "zoom", "t-shirt", "abstract", "yo-yo", "abdomen")
    private val plain = "a training history".toByteArray()

    /** The problem decrypting [file] with [with] reports. Fails the test if it opens. */
    private fun problemOf(file: ByteArray, with: List<String> = words): BackupProblem = try {
        BackupCipher.decrypt(file, with)
        fail("the file opened")
        error("unreachable")
    } catch (error: BackupException) {
        error.problem
    }

    @Test
    fun theRightWordsOpenWhatTheyLocked() {
        assertArrayEquals(plain, BackupCipher.decrypt(BackupCipher.encrypt(plain, words), words))
    }

    @Test
    fun wordsAreCaseBlind() {
        val file = BackupCipher.encrypt(plain, words.map { it.uppercase() })
        assertArrayEquals(plain, BackupCipher.decrypt(file, words))
    }

    @Test
    fun oneWrongWordOpensNothing() {
        assertEquals(BackupProblem.WRONG_WORDS_OR_DAMAGED, problemOf(BackupCipher.encrypt(plain, words), other))
    }

    @Test
    fun everyBackupHasItsOwnSaltAndNonce() {
        val first = BackupCipher.encrypt(plain, words)
        val second = BackupCipher.encrypt(plain, words)
        assertFalse(first.copyOfRange(0, HEADER_BYTES).contentEquals(second.copyOfRange(0, HEADER_BYTES)))
    }

    @Test
    fun changingAnyPartOfTheFileIsCaught() {
        val file = BackupCipher.encrypt(plain, words)
        // Salt, nonce, ciphertext and tag: each fails GCM's check.
        listOf(10, 26, HEADER_BYTES + 1, file.size - 1).forEach { index ->
            val changed = file.copyOf().also { it[index] = (it[index].toInt() xor 1).toByte() }
            assertEquals("byte $index", BackupProblem.WRONG_WORDS_OR_DAMAGED, problemOf(changed))
        }
    }

    @Test
    fun weakerOrHeavierKeySettingsAreRefusedBeforeAnyKeyIsMade() {
        val file = BackupCipher.encrypt(plain, words)
        val fewerRounds = file.copyOf().also {
            it[6] = 0
            it[7] = 0
            it[8] = 0
            it[9] = 1
        }
        assertEquals(BackupProblem.WRONG_WORDS_OR_DAMAGED, problemOf(fewerRounds))
        val otherFunction = file.copyOf().also { it[5] = 2 }
        assertEquals(BackupProblem.WRONG_WORDS_OR_DAMAGED, problemOf(otherFunction))
    }

    @Test
    fun aNewerFormatAsksForAnUpdate() {
        val file = BackupCipher.encrypt(plain, words).also { it[4] = 2 }
        assertEquals(BackupProblem.NEWER_VERSION, problemOf(file))
    }

    @Test
    fun otherFilesAndCutShortFilesAreNotBackups() {
        assertEquals(BackupProblem.NOT_A_BACKUP, problemOf("PK\u0003\u0004 a zip".toByteArray()))
        assertEquals(BackupProblem.NOT_A_BACKUP, problemOf(ByteArray(0)))
        val file = BackupCipher.encrypt(plain, words)
        assertEquals(BackupProblem.NOT_A_BACKUP, problemOf(file.copyOf(HEADER_BYTES + 3)))
    }

    @Test
    fun anOversizedFileIsRefusedUnread() {
        assertEquals(BackupProblem.TOO_LARGE, problemOf(ByteArray(MAX_FILE_BYTES + 1)))
    }
}
