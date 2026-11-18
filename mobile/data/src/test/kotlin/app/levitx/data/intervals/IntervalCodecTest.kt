package app.levitx.data.intervals

import app.levitx.core.session.CompletedSession
import app.levitx.core.session.LoggedSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IntervalCodecTest {
    private fun move(id: String, vararg seconds: Int) = CompletedSession(
        gateId = id,
        startedAtEpochMillis = 1_000,
        finishedAtEpochMillis = 900_000,
        sets = seconds.map { LoggedSet(value = it, repsInReserve = null, isBestEffort = false) },
    )

    @Test
    fun theWorkDoneSurvivesTheRoundTrip() {
        val sessions = listOf(move("cardio.burpees", 20, 20, 12), move("mobility.pigeon", 30, 30))

        assertEquals(sessions, IntervalCodec.decodeOrNull(IntervalCodec.encode(sessions)))
    }

    @Test
    fun aMoveThisVersionDoesntKnowMakesTheWholeWorkoutUnreadable() {
        val text = IntervalCodec.encode(listOf(move("cardio.burpees", 20), move("cardio.unknown", 20)))

        assertNull(IntervalCodec.decodeOrNull(text))
    }

    @Test
    fun impossibleValuesAreUnreadable() {
        val noTime = IntervalCodec.encode(listOf(move("cardio.burpees", 20))).replace("[20]", "[0]")
        val tooLong = IntervalCodec.encode(listOf(move("cardio.burpees", 20))).replace("[20]", "[601]")
        val backwards = """{"v":1,"moves":[{"id":"cardio.burpees","startedAt":5,"finishedAt":4,"seconds":[20]}]}"""
        val empty = """{"v":1,"moves":[]}"""

        listOf(noTime, tooLong, backwards, empty).forEach { assertNull(it, IntervalCodec.decodeOrNull(it)) }
    }

    @Test
    fun textThatIsntAWorkoutOrIsFromANewerVersionIsUnreadable() {
        val newer = IntervalCodec.encode(listOf(move("cardio.burpees", 20))).replace("\"v\":1", "\"v\":2")

        listOf("not json", "{}", newer).forEach { assertNull(it, IntervalCodec.decodeOrNull(it)) }
    }
}
