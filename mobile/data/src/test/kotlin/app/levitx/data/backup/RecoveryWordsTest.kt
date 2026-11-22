package app.levitx.data.backup

import java.security.SecureRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryWordsTest {
    @Test
    fun theListIsTheEffLargeList() {
        val list = RecoveryWords.list
        assertEquals(RecoveryWords.LIST_SIZE, list.size)
        assertEquals("abacus", list.first())
        assertEquals("abstract", list[22])
        assertEquals("zoom", list.last())
        assertEquals(list.sorted(), list)
    }

    @Test
    fun aNewSetIsSixWordsFromTheList() {
        repeat(50) {
            val words = RecoveryWords.generate(SecureRandom())
            assertEquals(RecoveryWords.COUNT, words.size)
            assertTrue(words.all(RecoveryWords::isWord))
        }
    }

    @Test
    fun typedWordsAreReadTheWayTheyWereWrittenDown() {
        val expected = listOf("abacus", "zoom", "t-shirt", "abstract", "yo-yo", "felt-tip")
        assertEquals(expected, RecoveryWords.parse("abacus zoom t-shirt abstract yo-yo felt-tip"))
        assertEquals(expected, RecoveryWords.parse("  Abacus, ZOOM\nt-shirt\tabstract,yo-yo   felt-tip "))
    }

    @Test
    fun theWrongNumberOfWordsOrAnUnknownWordIsRefused() {
        assertNull(RecoveryWords.parse("abacus zoom t-shirt abstract yo-yo"))
        assertNull(RecoveryWords.parse("abacus zoom t-shirt abstract yo-yo felt-tip zoom"))
        assertNull(RecoveryWords.parse("abacus zoom t-shirt abstract yo-yo levitx"))
        assertNull(RecoveryWords.parse(""))
        assertFalse(RecoveryWords.isWord("levitx"))
        assertTrue(RecoveryWords.isWord(" Zoom "))
    }
}
