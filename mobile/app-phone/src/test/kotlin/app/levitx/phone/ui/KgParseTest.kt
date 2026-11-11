package app.levitx.phone.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KgParseTest {
    @Test
    fun parsesDotsAndCommas() {
        assertEquals(72.5, parseKg("72.5"))
        assertEquals(72.5, parseKg("72,5"))
        assertNull(parseKg("abc"))
        assertNull(parseKg("-3"))
    }
}
