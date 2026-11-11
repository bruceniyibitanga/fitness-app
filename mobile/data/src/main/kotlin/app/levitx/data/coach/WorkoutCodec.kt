package app.levitx.data.coach

import app.levitx.core.coach.PlannedSession
import app.levitx.core.coach.WorkoutRun

/**
 * Turns planned sessions and workouts into text and back, in the same format
 * the database stores them in. Sync uses it to carry the next session to the
 * watch and finished workouts back, so there is one format, not two.
 *
 * Reading never throws and never quotes the text: a workout holds training
 * history. Anything that can't be read whole, including an exercise this
 * version doesn't know, reads as null.
 */
object WorkoutCodec {
    /** The text for [session]. */
    fun encodePlan(session: PlannedSession): String = CoachCodec.encode(planDocumentOf(session))

    /** The session [text] describes, or null when it isn't one this version can read. */
    fun decodePlanOrNull(text: String): PlannedSession? = CoachCodec.decodePlanOrNull(text)?.toSessionOrNull()

    /** The text for [run]. */
    fun encodeRun(run: WorkoutRun): String = CoachCodec.encode(workoutDocumentOf(run))

    /** The workout [text] describes, or null when it isn't one this version can read. */
    fun decodeRunOrNull(text: String): WorkoutRun? = CoachCodec.decodeWorkoutOrNull(text)?.toRunOrNull()
}
