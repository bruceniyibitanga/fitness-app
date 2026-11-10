package app.levitx.data.database

import org.junit.Assert.assertEquals
import org.junit.Test

class RawKeyTest {
    @Test
    fun formatsBytesAsSqlCipherRawKey() {
        val key = byteArrayOf(0x00, 0x0f, 0x10, 0xff.toByte())
        assertEquals("x'000f10ff'", String(key.toSqlCipherRawKey(), Charsets.US_ASCII))
    }

    @Test
    fun aFullKeyIs64HexDigitsInsideTheQuotes() {
        val raw = String(ByteArray(32).toSqlCipherRawKey(), Charsets.US_ASCII)
        assertEquals(64 + "x''".length, raw.length)
    }
}
