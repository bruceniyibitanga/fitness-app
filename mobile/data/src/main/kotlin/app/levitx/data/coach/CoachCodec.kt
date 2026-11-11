package app.levitx.data.coach

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Turns the coach's documents into JSON and back.
 *
 * Reading never throws a message that quotes the document: the documents hold
 * health data (pain, training history), and exception messages can end up in
 * crash reports.
 */
internal object CoachCodec {
    // Unknown keys are ignored so a document written by a newer app still opens.
    private val json = Json { ignoreUnknownKeys = true }

    fun encode(document: CoachDocument): String = json.encodeToString(CoachDocument.serializer(), document)

    /**
     * Reads a coach document.
     *
     * @throws UnreadableCoachStateException when it isn't a document this version can read.
     */
    fun decodeCoach(text: String): CoachDocument =
        decodeOrNull(text, CoachDocument.serializer())?.takeIf { it.v <= COACH_DOCUMENT_VERSION }
            ?: throw UnreadableCoachStateException()

    fun encode(document: WorkoutDocument): String = json.encodeToString(WorkoutDocument.serializer(), document)

    /** Reads a workout document, or null when it can't be read. A lost draft only loses the workout in progress. */
    fun decodeWorkoutOrNull(text: String): WorkoutDocument? =
        decodeOrNull(text, WorkoutDocument.serializer())?.takeIf { it.v <= WORKOUT_DOCUMENT_VERSION }

    fun encode(document: PlanDocument): String = json.encodeToString(PlanDocument.serializer(), document)

    /** Reads a plan document, or null when it can't be read. */
    fun decodePlanOrNull(text: String): PlanDocument? =
        decodeOrNull(text, PlanDocument.serializer())?.takeIf { it.v <= PLAN_DOCUMENT_VERSION }

    /**
     * The decoded value, or null for anything that isn't valid JSON of the right
     * shape. The error is dropped, text and all.
     */
    private fun <T> decodeOrNull(text: String, serializer: KSerializer<T>): T? = try {
        json.decodeFromString(serializer, text)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

/**
 * The coach's stored state is from a newer version of the app, or damaged.
 * Nothing is overwritten: the person can update the app, and nothing they've
 * done is lost.
 */
class UnreadableCoachStateException : IllegalStateException("The coach's stored state can't be read by this version")
