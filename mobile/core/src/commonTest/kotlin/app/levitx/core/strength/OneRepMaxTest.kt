package app.levitx.core.strength

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OneRepMaxTest {
    @Test
    fun usesTheEpleyFormula() {
        // 100 kg for 5 reps: 100 × (1 + 5/30) = 116.67
        assertEquals(116.667, assertNotNull(estimateOneRepMax(100.0, 5)), absoluteTolerance = 0.001)
    }

    @Test
    fun addsRepsInReserveToRepsPerformed() {
        assertEquals(estimateOneRepMax(100.0, 7), estimateOneRepMax(100.0, 5, repsInReserve = 2))
    }

    @Test
    fun singleRepToFailureIsTheMax() {
        assertEquals(90.0, estimateOneRepMax(90.0, 1))
    }

    @Test
    fun returnsNullAboveTheTrustedRepRange() {
        assertNull(estimateOneRepMax(70.0, 11))
        assertNull(estimateOneRepMax(70.0, 8, repsInReserve = 3))
    }

    @Test
    fun rejectsInvalidInput() {
        assertFailsWith<IllegalArgumentException> { estimateOneRepMax(70.0, 0) }
        assertFailsWith<IllegalArgumentException> { estimateOneRepMax(-1.0, 5) }
        assertFailsWith<IllegalArgumentException> { estimateOneRepMax(70.0, 5, repsInReserve = -1) }
    }
}
