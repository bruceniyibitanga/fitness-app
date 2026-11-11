package app.levitx.data.progress

import kotlinx.coroutines.flow.Flow

/**
 * The person's best result for each gate.
 *
 * This is the only way the UI reads or changes bests, so how they are stored
 * can change without touching any screen. Progress along a path and today's
 * plan are worked out from these bests by `:core` (`SkillPath.trainingDay`).
 */
interface ProgressRepository {
    /**
     * Emits the best result per gate id straight away, then again whenever
     * one changes. Gates without a result are missing from the map.
     */
    fun observeBests(): Flow<Map<String, Int>>

    /**
     * Records [best] reps or seconds for [gateId]. Keeps the stored value if
     * it is already higher, so a weaker attempt never lowers a best.
     */
    suspend fun recordBest(gateId: String, best: Int)
}
