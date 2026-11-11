package app.levitx.data.coach

import app.levitx.core.coach.WeighIn
import app.levitx.core.load.LoadLimits
import app.levitx.data.database.BodyweightEntity
import app.levitx.data.database.GateBestEntity

/**
 * Builds the [CoachState] from the coach's document and the tables that own
 * bests and bodyweight.
 *
 * Bests come from both, highest wins: a session recorded on the watch raises
 * the table's best without going through the coach, and the coach must still
 * see it. Bodyweight readings come only from their table, which the weigh-in,
 * the bodyweight screen and Health Connect all write to.
 */
internal fun assembleCoachState(
    document: CoachDocument,
    bests: List<GateBestEntity>,
    bodyweights: List<BodyweightEntity>,
): CoachState {
    val model = document.toModel()
    val mergedBests = (model.gateBests.keys + bests.map { it.gateId }).associateWith { gateId ->
        maxOf(model.gateBests[gateId] ?: 0, bests.firstOrNull { it.gateId == gateId }?.best ?: 0)
    }
    val weighIns = bodyweights
        .filter { it.kg in LoadLimits.MIN_BODYWEIGHT_KG..LoadLimits.MAX_BODYWEIGHT_KG }
        .map { WeighIn(it.recordedAtEpochMillis, it.kg) }
    return CoachState(model = model.copy(gateBests = mergedBests, weighIns = weighIns), trainingDays = document.days())
}
