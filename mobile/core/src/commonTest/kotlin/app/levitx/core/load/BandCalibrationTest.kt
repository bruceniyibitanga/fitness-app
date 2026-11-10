package app.levitx.core.load

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BandCalibrationTest {
    @Test
    fun assistanceIsBodyweightMinusTheReading() {
        assertEquals(22.5, bandAssistanceFromScale(bodyweightKg = 80.0, scaleReadingKg = 57.5))
    }

    @Test
    fun aReadingThatIsntLowerGivesNoBand() {
        assertNull(bandAssistanceFromScale(80.0, 80.0))
        assertNull(bandAssistanceFromScale(80.0, 85.0))
    }

    @Test
    fun rejectsNonsenseReadings() {
        assertNull(bandAssistanceFromScale(80.0, -1.0))
        assertNull(bandAssistanceFromScale(80.0, Double.NaN))
    }
}
