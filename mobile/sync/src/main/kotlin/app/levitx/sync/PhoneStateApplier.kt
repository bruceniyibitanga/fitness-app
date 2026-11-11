package app.levitx.sync

import app.levitx.data.coach.NextSessionRepository
import app.levitx.data.equipment.EquipmentRepository
import app.levitx.data.goal.GoalRepository
import app.levitx.data.progress.ProgressRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Copies the phone's state into the watch's own database, so the watch runs
 * the coach's session with the phone out of reach.
 *
 * Applying the same state twice, or an older one late, changes nothing:
 * - The goal and the plan belong to the phone. The watch only ever copies them.
 * - A best only ever goes up.
 * - A plan made before the phone recorded the watch's last workout is never
 *   taken, so the watch can't run the same session twice.
 */
internal class PhoneStateApplier @Inject constructor(
    private val goalRepository: GoalRepository,
    private val progressRepository: ProgressRepository,
    private val equipmentRepository: EquipmentRepository,
    private val nextSessionRepository: NextSessionRepository,
) {
    /** @return True when anything on the watch changed. */
    suspend fun applyState(state: PhoneState): Boolean {
        // Every step runs; none is skipped because an earlier one changed something.
        val changes = listOf(applyGoal(state), applyBests(state), applyBands(state), applyNext(state))
        return changes.any { it }
    }

    private suspend fun applyGoal(state: PhoneState): Boolean {
        if (goalRepository.observeActiveGoal().first() == state.path) return false
        if (state.path == null) goalRepository.clearActiveGoal() else goalRepository.setActiveGoal(state.path)
        return true
    }

    private suspend fun applyBests(state: PhoneState): Boolean {
        val bests = progressRepository.observeBests().first()
        val higher = state.bests.filter { (gateId, best) -> best > (bests[gateId] ?: 0) }
        // recordBest keeps the higher value, so a best set on the watch is never lowered.
        higher.forEach { (gateId, best) -> progressRepository.recordBest(gateId, best) }
        return higher.isNotEmpty()
    }

    private suspend fun applyBands(state: PhoneState): Boolean {
        if (equipmentRepository.observeBands().first() == state.bands) return false
        equipmentRepository.replaceBands(state.bands)
        return true
    }

    /**
     * Takes the phone's next session, unless the watch finished a workout the
     * phone hadn't recorded when it planned it. That plan is stale: it may be
     * the very session just done. The watch then keeps waiting, and the next
     * state the phone sends after recording the workout is taken.
     */
    private suspend fun applyNext(state: PhoneState): Boolean {
        val awaiting = nextSessionRepository.awaitingWorkoutId()
        val isStale = awaiting != null && awaiting !in state.recentWorkoutIds
        val isSame = awaiting == null && nextSessionRepository.observeNext().first() == state.next
        if (isStale || isSame) return false
        nextSessionRepository.replace(state.next)
        return true
    }
}
