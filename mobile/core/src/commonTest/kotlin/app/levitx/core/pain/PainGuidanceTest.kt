package app.levitx.core.pain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PainGuidanceTest {
    @Test
    fun painUpToFiveThatSettlesByMorningIsWithinTheGuide() {
        val result = assessPain(PainCheckIn(duringExercise = 5, morningBefore = 2, morningAfter = 2))
        assertEquals(PainAssessment(PainAdvice.CONTINUE_AS_PLANNED, PainReason.WITHIN_GUIDE), result)
    }

    @Test
    fun painAboveFiveDuringExerciseMeansEaseOff() {
        val result = assessPain(PainCheckIn(duringExercise = 6))
        assertEquals(PainAssessment(PainAdvice.EASE_OFF, PainReason.TOO_HIGH_DURING_EXERCISE), result)
    }

    @Test
    fun worseNextMorningMeansEaseOff() {
        val result = assessPain(PainCheckIn(duringExercise = 3, morningBefore = 1, morningAfter = 3))
        assertEquals(PainAssessment(PainAdvice.EASE_OFF, PainReason.WORSE_NEXT_MORNING), result)
    }

    @Test
    fun redFlagAlwaysMeansSeeAProfessional() {
        val result = assessPain(PainCheckIn(duringExercise = 0, redFlags = setOf(RedFlag.NUMBNESS_OR_TINGLING)))
        assertEquals(PainAssessment(PainAdvice.SEE_PROFESSIONAL, PainReason.RED_FLAG), result)
    }

    @Test
    fun missingMorningRatingsDoNotBlockAdvice() {
        assertEquals(PainAdvice.CONTINUE_AS_PLANNED, assessPain(PainCheckIn(duringExercise = 4)).advice)
    }

    @Test
    fun rejectsRatingsOutsideTheScale() {
        assertFailsWith<IllegalArgumentException> { PainCheckIn(duringExercise = 11) }
        assertFailsWith<IllegalArgumentException> { PainCheckIn(duringExercise = 2, morningAfter = -1) }
    }
}
