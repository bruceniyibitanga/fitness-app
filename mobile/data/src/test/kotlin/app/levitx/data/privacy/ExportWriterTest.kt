package app.levitx.data.privacy

import app.levitx.core.history.ExerciseRecord
import app.levitx.core.history.WorkoutRecord
import app.levitx.core.session.LoggedSet
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportWriterTest {
    private val workout = WorkoutRecord(
        id = "w1",
        startedAtEpochMillis = 0,
        finishedAtEpochMillis = 60_000,
        exercises = listOf(
            ExerciseRecord(
                "horizontal_push.feet_30cm",
                listOf(LoggedSet(10, 2, false), LoggedSet(8, null, true, addedKg = 5.0)),
                80.5,
            ),
        ),
    )

    private val snapshot = ExportSnapshot(
        coach = Json.parseToJsonElement("""{"version":3}"""),
        workouts = listOf(workout),
        weighIns = listOf(ExportWeighIn(isoTime(0), 80.5)),
        bands = listOf(ExportBand("Red", 15.0)),
        gateBests = mapOf("first_pull_up.dead_hang" to 30),
    )

    private fun entries(): Map<String, String> {
        val out = ByteArrayOutputStream()
        writeExport(snapshot, nowEpochMillis = 1_000, out)
        val zip = ZipInputStream(ByteArrayInputStream(out.toByteArray()))
        return generateSequence { zip.nextEntry }.associate { it.name to zip.readBytes().toString(Charsets.UTF_8) }
    }

    @Test
    fun theZipHoldsTheJsonAndBothSpreadsheets() {
        assertEquals(setOf("levitx.json", "workouts.csv", "weigh-ins.csv"), entries().keys)
    }

    @Test
    fun theJsonHasEverythingWithReadableTimes() {
        val json = Json.parseToJsonElement(entries().getValue("levitx.json")).jsonObject
        assertEquals(EXPORT_FORMAT.toString(), json.getValue("format").jsonPrimitive.content)
        assertEquals(
            "1970-01-01T00:01:00Z",
            json.getValue("workouts").jsonArray[0].jsonObject.getValue("finishedAt").jsonPrimitive.content,
        )
        assertEquals("3", json.getValue("coach").jsonObject.getValue("version").jsonPrimitive.content)
        assertEquals("Red", json.getValue("bands").jsonArray[0].jsonObject.getValue("name").jsonPrimitive.content)
    }

    @Test
    fun theWorkoutsSheetHasARowPerSetAndQuotesNamesWithCommas() {
        val lines = entries().getValue("workouts.csv").trimEnd().split("\r\n")
        assertEquals(3, lines.size)
        assertTrue(lines[1].contains("\"Push-up, feet on 30 cm box\""))
        assertTrue(lines[2].endsWith(",8,reps,,5.0,0.0,80.5"))
    }

    @Test
    fun aQuoteInsideAFieldIsDoubled() {
        assertEquals("a,b\r\n\"say \"\"hi\"\"\",x\r\n", csv(listOf("a", "b"), listOf(listOf("say \"hi\"", "x"))))
    }
}
