package app.levitx.core.load

import kotlin.test.Test
import kotlin.test.assertEquals

class KgFormatTest {
    @Test
    fun wholeNumbersHaveNoDecimal() {
        assertEquals("70", formatKg(70.0))
        assertEquals("0", formatKg(0.0))
    }

    @Test
    fun oneDecimalPlaceAtMost() {
        assertEquals("72.5", formatKg(72.5))
        assertEquals("72.5", formatKg(72.46))
        assertEquals("73", formatKg(72.96))
    }
}
