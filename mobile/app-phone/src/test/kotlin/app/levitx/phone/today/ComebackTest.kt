package app.levitx.phone.today

import app.levitx.core.coach.Coach
import app.levitx.core.coach.ReadinessSignal
import app.levitx.core.coach.Suggestion
import app.levitx.core.coach.SuggestionChoice
import app.levitx.testing.newcomer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ComebackTest {
    private val session = checkNotNull(Coach().recommend(newcomer(), 0).session)

    private fun suggestion(vararg signals: ReadinessSignal) =
        Suggestion(signals.toList(), SuggestionChoice.LIGHTER, session.lightened())

    @Test
    fun aLongGapIsAComebackWithBothLengths() {
        val comeback = comebackOf(suggestion(ReadinessSignal.LongGap(days = 23)), session)
        assertEquals(Comeback(23, session.lightened().estimatedMinutes, session.estimatedMinutes), comeback)
    }

    @Test
    fun anyOtherSuggestionKeepsTheUsualCard() {
        assertNull(comebackOf(suggestion(ReadinessSignal.LighterWeek), session))
    }
}
