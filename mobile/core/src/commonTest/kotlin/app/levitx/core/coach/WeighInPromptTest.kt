package app.levitx.core.coach

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeighInPromptTest {
    private val day = 24L * 60 * 60 * 1_000
    private val now = 100 * day

    @Test
    fun aReadingFromThisWeekMeansNoAsk() {
        assertFalse(WeighInPrompt.isDue(listOf(WeighIn(now - 6 * day, 74.0)), null, now))
    }

    @Test
    fun noReadingThisWeekAsks() {
        assertTrue(WeighInPrompt.isDue(listOf(WeighIn(now - 8 * day, 74.0)), null, now))
        assertTrue(WeighInPrompt.isDue(emptyList(), null, now))
    }

    @Test
    fun askingOnceCoversAWeek() {
        val old = listOf(WeighIn(now - 30 * day, 74.0))
        assertFalse(WeighInPrompt.isDue(old, lastAskedAtEpochMillis = now - 6 * day, nowEpochMillis = now))
        assertTrue(WeighInPrompt.isDue(old, lastAskedAtEpochMillis = now - 7 * day, nowEpochMillis = now))
    }

    @Test
    fun timesInTheFutureDontCount() {
        // The clock went back after these were saved.
        assertTrue(
            WeighInPrompt.isDue(
                listOf(WeighIn(now + day, 74.0)),
                lastAskedAtEpochMillis = now + day,
                nowEpochMillis = now,
            ),
        )
    }
}
