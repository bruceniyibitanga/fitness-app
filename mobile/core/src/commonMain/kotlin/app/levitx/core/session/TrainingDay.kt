package app.levitx.core.session

import app.levitx.core.skills.Gate
import app.levitx.core.skills.PathProgress
import app.levitx.core.skills.SkillPath

/**
 * Where the person is on their path and what to train today.
 *
 * Both apps build their home screens from this, so they always agree.
 *
 * @property path The goal.
 * @property progress Progress along [path].
 * @property currentGate The gate to work on, or null once the path is complete.
 * @property currentBest The person's best at [currentGate], 0 if none yet.
 * @property plan Today's session, or null once the path is complete.
 */
public data class TrainingDay(
    val path: SkillPath,
    val progress: PathProgress,
    val currentGate: Gate?,
    val currentBest: Int,
    val plan: SessionPlan?,
) {
    /** 1-based number of the current gate, or the last gate once complete. For display. */
    val gateNumber: Int
        get() = (progress.currentIndex ?: path.gates.lastIndex) + 1
}

/** Works out today's training on this path from the person's best result per gate id. */
public fun SkillPath.trainingDay(bestByGateId: Map<String, Int>): TrainingDay {
    val progress = progressFor(bestByGateId)
    val gate = progress.currentIndex?.let { gates[it] }
    val best = gate?.let { (bestByGateId[it.id] ?: 0).coerceAtLeast(0) } ?: 0
    return TrainingDay(
        path = this,
        progress = progress,
        currentGate = gate,
        currentBest = best,
        plan = gate?.let { SessionPlanner.planFor(it, best) },
    )
}
