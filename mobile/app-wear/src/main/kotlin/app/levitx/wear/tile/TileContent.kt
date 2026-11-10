package app.levitx.wear.tile

import app.levitx.core.session.TrainingDay
import app.levitx.core.skills.GateMeasure

/**
 * What the Tile shows, worked out from the same [TrainingDay] the home screen
 * uses, so the two always agree.
 */
sealed interface TileContent {
    /** The database couldn't be read. The Tile points to the app, which explains why. */
    data object Unavailable : TileContent

    /** No goal on the watch yet. */
    data object NoGoal : TileContent

    /**
     * Every gate is passed.
     *
     * @property skillName The goal that was reached.
     */
    data class PathComplete(val skillName: String) : TileContent

    /**
     * The next session.
     *
     * @property gateNumber 1-based position of the current gate.
     * @property gateCount Gates on the path.
     * @property gateTitle The gate being trained, for example "10 pull-ups".
     * @property workSets Work sets before the best-effort set.
     * @property workTarget Reps or seconds per work set.
     * @property measure Whether sets count reps or seconds.
     */
    data class NextSession(
        val gateNumber: Int,
        val gateCount: Int,
        val gateTitle: String,
        val workSets: Int,
        val workTarget: Int,
        val measure: GateMeasure,
    ) : TileContent
}

/** The Tile's content for [day], or [TileContent.NoGoal] when there is no goal. */
fun tileContentFor(day: TrainingDay?): TileContent {
    val gate = day?.currentGate
    val plan = day?.plan
    return when {
        day == null -> TileContent.NoGoal

        gate == null || plan == null -> TileContent.PathComplete(day.path.name)

        else -> {
            val workSets = plan.sets.filterNot { it.isBestEffort }
            TileContent.NextSession(
                gateNumber = day.gateNumber,
                gateCount = day.path.gates.size,
                gateTitle = gate.title,
                workSets = workSets.size,
                workTarget = workSets.firstOrNull()?.target ?: 0,
                measure = gate.measure,
            )
        }
    }
}
