package app.levitx.data.intervals

import app.levitx.core.intervals.IntervalCatalogue
import app.levitx.core.session.CompletedSession
import app.levitx.core.session.LoggedSet
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Format of a stored interval workout. Bump it for any change an older app couldn't read. */
internal const val INTERVAL_DOCUMENT_VERSION = 1

/** Most moves one workout can log. The longest built-in workout has seven. */
private const val MAX_MOVES = 30

/** Most rounds one move can log, both sides counted. Blocks have at most twenty. */
private const val MAX_ROUNDS_PER_MOVE = 100

/** Most seconds one round can log. Steps are at most ten minutes. */
private const val MAX_ROUND_SECONDS = 600

/**
 * A stored interval workout: what was done of each move. Moves are kept as
 * ids, so the catalogue stays the one place they are described.
 *
 * @property v [INTERVAL_DOCUMENT_VERSION] when written.
 * @property moves The moves, in the order they were done.
 */
@Serializable
internal data class IntervalDocument(val v: Int, val moves: List<StoredMove>)

/**
 * One move of a stored interval workout.
 *
 * @property id The move's id in `IntervalCatalogue`.
 * @property startedAt When the workout started, in UTC milliseconds.
 * @property finishedAt When it finished, in UTC milliseconds.
 * @property seconds Seconds worked in each round, in order.
 */
@Serializable
internal data class StoredMove(val id: String, val startedAt: Long, val finishedAt: Long, val seconds: List<Int>)

/**
 * Turns the work done in an interval workout into text and back. The watch
 * stores it in this format and sync carries it the same way, so there is one
 * format, not two.
 *
 * Reading never throws and never quotes the text, which is training history.
 * Anything that can't be read whole reads as null: a move this version
 * doesn't know, too many rounds, impossible times.
 */
object IntervalCodec {
    // Unknown keys are ignored so a document written by a newer app still opens.
    private val json = Json { ignoreUnknownKeys = true }

    /** The text for [sessions]. */
    fun encode(sessions: List<CompletedSession>): String = json.encodeToString(
        IntervalDocument.serializer(),
        IntervalDocument(
            v = INTERVAL_DOCUMENT_VERSION,
            moves = sessions.map { session ->
                StoredMove(
                    id = session.gateId,
                    startedAt = session.startedAtEpochMillis,
                    finishedAt = session.finishedAtEpochMillis,
                    seconds = session.sets.map { it.value },
                )
            },
        ),
    )

    /** The work [text] describes, or null when it isn't something this version can read. */
    fun decodeOrNull(text: String): List<CompletedSession>? =
        parseOrNull(text)?.takeIf { it.isReadable() }?.moves?.map { it.toSession() }

    /** The document, or null when the text isn't one. The error is dropped, text and all. */
    private fun parseOrNull(text: String): IntervalDocument? = try {
        json.decodeFromString(IntervalDocument.serializer(), text)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun IntervalDocument.isReadable(): Boolean =
        v in 1..INTERVAL_DOCUMENT_VERSION && moves.size in 1..MAX_MOVES && moves.all { it.isValid() }

    private fun StoredMove.isValid(): Boolean = IntervalCatalogue.moveById(id) != null &&
        startedAt in 0..finishedAt &&
        seconds.size in 1..MAX_ROUNDS_PER_MOVE &&
        seconds.all { it in 1..MAX_ROUND_SECONDS }

    // Only called once isValid() holds, so CompletedSession's own checks pass.
    private fun StoredMove.toSession() = CompletedSession(
        gateId = id,
        startedAtEpochMillis = startedAt,
        finishedAtEpochMillis = finishedAt,
        sets = seconds.map { LoggedSet(value = it, repsInReserve = null, isBestEffort = false) },
    )
}
