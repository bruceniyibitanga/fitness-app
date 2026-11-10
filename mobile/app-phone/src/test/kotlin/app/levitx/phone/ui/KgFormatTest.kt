package app.levitx.phone.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KgFormatTest {
    @Test
    fun formatsWholeAndHalfKilograms() {
        assertEquals("70", formatKg(70.0))
        assertEquals("72.5", formatKg(72.5))
        assertEquals("72.5", formatKg(72.46))
    }

    @Test
    fun parsesDotsAndCommas() {
        assertEquals(72.5, parseKg("72.5"))
        assertEquals(72.5, parseKg("72.5"))
        assertNull(parseKg("abc"))
        assertNull(parseKg("-3"))
    }
}
