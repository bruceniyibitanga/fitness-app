package app.levitx.phone.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackTest {
    @Test
    fun `an ordinary model is kept as it is`() {
        assertEquals("Pixel 8 Pro", deviceText("Pixel 8 Pro"))
    }

    @Test
    fun `a phone that reports nothing is unknown`() {
        assertEquals("unknown", deviceText(null))
        assertEquals("unknown", deviceText(""))
        assertEquals("unknown", deviceText("  \n\t "))
    }

    @Test
    fun `line breaks and control characters can't add lines to the draft`() {
        val cleaned = deviceText("SM-G991B\r\nBcc: someone@example.com\u0000\u001B")
        assertEquals("SM-G991B Bcc: someone@example.com", cleaned)
        assertFalse(cleaned.any { it.isISOControl() })
    }

    @Test
    fun `runs of spaces become one`() {
        assertEquals("moto g power", deviceText("  moto   g \t power  "))
    }

    @Test
    fun `a long value is cut, with no space left hanging`() {
        val cleaned = deviceText("a".repeat(MAX_DEVICE_TEXT - 1) + " " + "b".repeat(50))
        assertEquals("a".repeat(MAX_DEVICE_TEXT - 1), cleaned)
        assertTrue(deviceText("x".repeat(500)).length == MAX_DEVICE_TEXT)
    }

    @Test
    fun `the draft leaves room to write above the footer`() {
        val body = feedbackBody("Levitx 1.0 · Android 15 · Pixel 8")
        assertTrue(body.startsWith("\n\n"))
        assertEquals("Levitx 1.0 · Android 15 · Pixel 8", body.trim())
        assertEquals("Levitx 1.0 · Android 15 · Pixel 8", body.lines().last())
    }
}
