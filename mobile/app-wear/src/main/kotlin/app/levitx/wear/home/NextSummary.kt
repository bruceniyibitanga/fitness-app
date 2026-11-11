package app.levitx.wear.home

import app.levitx.core.coach.PlannedSession

/**
 * The next session in a glance, as the home screen and the Tile show it.
 *
 * @property exerciseCount Exercises in the session.
 * @property minutes Roughly how long it takes, warm-up included.
 * @property firstExercise The exercise it starts with.
 */
data class NextSummary(val exerciseCount: Int, val minutes: Int, val firstExercise: String)

/** This session as a [NextSummary]. One function, so the home screen and the Tile always agree. */
fun PlannedSession.summary(): NextSummary = NextSummary(
    exerciseCount = plans.size,
    minutes = estimatedMinutes,
    firstExercise = plans.first().exercise.name,
)
