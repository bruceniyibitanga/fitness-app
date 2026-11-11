package app.levitx.phone.skills

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Exercise
import app.levitx.core.coach.ExerciseCatalogue
import app.levitx.core.coach.Ladder
import app.levitx.core.coach.MovementPattern
import app.levitx.core.coach.currentExercise
import app.levitx.core.coach.currentGatePattern

/**
 * Where the person stands on one movement's ladder.
 *
 * @property pattern The movement.
 * @property rung 0-based rung the person trains at, after allowing for their kit.
 * @property rungCount Rungs on the ladder.
 * @property exercise The exercise trained now, or null without the kit for any of it.
 * @property target Reps or seconds its work sets aim for, or null until the coach has a figure.
 * @property next The next exercise up the ladder the person has the kit for, or null at the top.
 * @property isTrainedByGate True when sessions train this movement through the
 *   current skill gate rather than the ladder.
 */
data class LadderStanding(
    val pattern: MovementPattern,
    val rung: Int,
    val rungCount: Int,
    val exercise: Exercise?,
    val target: Int?,
    val next: Exercise?,
    val isTrainedByGate: Boolean,
)

/** Where [model] stands on every ladder in the catalogue. */
internal fun standings(model: AthleteModel): List<LadderStanding> {
    val gatePattern = model.currentGatePattern()
    return ExerciseCatalogue.ladders.map { ladder -> model.standingOn(ladder, gatePattern) }
}

private fun AthleteModel.standingOn(ladder: Ladder, gatePattern: MovementPattern?): LadderStanding {
    val owned = profile.equipment
    val pattern = ladder.pattern
    // Without the kit for the rung the person reached, they train the highest one they can.
    val rung = ladder.usableRungAtOrBelow(rung(pattern), owned) ?: rung(pattern).coerceIn(0, ladder.rungs.lastIndex)
    val exercise = currentExercise(pattern)
    return LadderStanding(
        pattern = pattern,
        rung = rung,
        rungCount = ladder.rungs.size,
        exercise = exercise,
        target = exercise?.let { exercises[it.id]?.target },
        next = exercise?.let { ladder.usableRungAbove(rung, owned)?.let { above -> ladder.exerciseAt(above, owned) } },
        isTrainedByGate = pattern == gatePattern,
    )
}
