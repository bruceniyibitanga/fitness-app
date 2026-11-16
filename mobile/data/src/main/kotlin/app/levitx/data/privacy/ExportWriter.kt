package app.levitx.data.privacy

import app.levitx.core.history.WorkoutRecord
import java.io.OutputStream
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/** Version of the export's layout. Raised if a field changes meaning. */
internal const val EXPORT_FORMAT = 1

/**
 * Everything to export, read from the database.
 *
 * @property coach The coach's stored state as JSON (plan, setup answers and
 *   what it has learned), or null before setup.
 * @property workouts Every finished workout.
 * @property weighIns Every weigh-in.
 * @property bands The person's bands.
 * @property gateBests Best per skill gate.
 */
internal data class ExportSnapshot(
    val coach: JsonElement?,
    val workouts: List<WorkoutRecord>,
    val weighIns: List<ExportWeighIn>,
    val bands: List<ExportBand>,
    val gateBests: Map<String, Int>,
)

/** A weigh-in. */
@Serializable
internal data class ExportWeighIn(val recordedAt: String, val kg: Double)

/** A resistance band. */
@Serializable
internal data class ExportBand(val name: String, val assistanceKg: Double)

@Serializable
private data class ExportSet(
    val value: Int,
    val repsInReserve: Int?,
    val isBestEffort: Boolean,
    val addedKg: Double,
    val assistanceKg: Double,
)

@Serializable
private data class ExportExercise(
    val exerciseId: String,
    val name: String?,
    val bodyweightKg: Double?,
    val sets: List<ExportSet>,
)

@Serializable
private data class ExportWorkout(
    val id: String,
    val startedAt: String,
    val finishedAt: String,
    val exercises: List<ExportExercise>,
)

@Serializable
private data class ExportDocument(
    val format: Int,
    val exportedAt: String,
    val coach: JsonElement?,
    val gateBests: Map<String, Int>,
    val bands: List<ExportBand>,
    val weighIns: List<ExportWeighIn>,
    val workouts: List<ExportWorkout>,
)

// Pretty, so the person can read it. Nulls are written out, which is the default.
private val exportJson = Json { prettyPrint = true }

/** Writes [snapshot] to [out] as a zip of JSON and CSV, stamped [nowEpochMillis]. Doesn't close [out]. */
internal fun writeExport(snapshot: ExportSnapshot, nowEpochMillis: Long, out: OutputStream) {
    val zip = ZipOutputStream(out)
    zip.entry("levitx.json", exportJson.encodeToString(ExportDocument.serializer(), document(snapshot, nowEpochMillis)))
    zip.entry("workouts.csv", workoutsCsv(snapshot.workouts))
    zip.entry(
        "weigh-ins.csv",
        csv(
            listOf("recorded_at", "kg"),
            snapshot.weighIns.map {
                listOf(it.recordedAt, it.kg.toString())
            },
        ),
    )
    // Finishes the zip without closing the stream, which belongs to the caller.
    zip.finish()
}

/** An ISO 8601 time in UTC, such as 2026-11-16T03:42:00Z. */
internal fun isoTime(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis).toString()

private fun document(snapshot: ExportSnapshot, nowEpochMillis: Long) = ExportDocument(
    format = EXPORT_FORMAT,
    exportedAt = isoTime(nowEpochMillis),
    coach = snapshot.coach,
    gateBests = snapshot.gateBests,
    bands = snapshot.bands,
    weighIns = snapshot.weighIns,
    workouts = snapshot.workouts.sortedBy { it.finishedAtEpochMillis }.map { workout ->
        ExportWorkout(
            id = workout.id,
            startedAt = isoTime(workout.startedAtEpochMillis),
            finishedAt = isoTime(workout.finishedAtEpochMillis),
            exercises = workout.exercises.map { record ->
                ExportExercise(
                    exerciseId = record.exerciseId,
                    name = record.exercise?.name,
                    bodyweightKg = record.bodyweightKg,
                    sets = record.sets.map {
                        ExportSet(it.value, it.repsInReserve, it.isBestEffort, it.addedKg, it.assistanceKg)
                    },
                )
            },
        )
    },
)

/** One row per set, oldest workout first. */
private fun workoutsCsv(workouts: List<WorkoutRecord>): String = csv(
    header = listOf(
        "workout_id", "finished_at", "exercise_id", "exercise", "set", "value", "measure",
        "reps_left", "added_kg", "band_kg", "bodyweight_kg",
    ),
    rows = workouts.sortedBy { it.finishedAtEpochMillis }.flatMap { workout ->
        workout.exercises.flatMap { record ->
            record.sets.mapIndexed { index, set ->
                listOf(
                    workout.id,
                    isoTime(workout.finishedAtEpochMillis),
                    record.exerciseId,
                    record.exercise?.name.orEmpty(),
                    (index + 1).toString(),
                    set.value.toString(),
                    record.exercise?.measure?.name?.lowercase().orEmpty(),
                    set.repsInReserve?.toString().orEmpty(),
                    set.addedKg.toString(),
                    set.assistanceKg.toString(),
                    record.bodyweightKg?.toString().orEmpty(),
                )
            }
        }
    },
)

/** RFC 4180 CSV: a field with a comma, quote or line break is quoted, quotes doubled. */
internal fun csv(header: List<String>, rows: List<List<String>>): String =
    (listOf(header) + rows).joinToString(separator = "\r\n", postfix = "\r\n") { row ->
        row.joinToString(",") { it.csvField() }
    }

/** Characters that make a CSV field need quotes. */
private val CSV_SPECIAL = setOf(',', '"', '\n', '\r')

private fun String.csvField(): String = if (any { it in CSV_SPECIAL }) "\"" + replace("\"", "\"\"") + "\"" else this

private fun ZipOutputStream.entry(name: String, text: String) {
    putNextEntry(ZipEntry(name))
    write(text.toByteArray(Charsets.UTF_8))
    closeEntry()
}
