package app.levitx.sync

import app.levitx.core.coach.Coach
import app.levitx.data.coach.CoachRepository
import app.levitx.data.equipment.EquipmentRepository
import app.levitx.data.goal.GoalRepository
import app.levitx.data.progress.ProgressRepository
import app.levitx.data.time.TrainingClock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Puts together what the phone sends the watch: the goal, bests and bands,
 * and the next session the coach plans. The coach's model never leaves the
 * phone; only the plan it produces does.
 */
internal class PhoneStateSource @Inject constructor(
    private val goalRepository: GoalRepository,
    private val progressRepository: ProgressRepository,
    private val equipmentRepository: EquipmentRepository,
    private val coachRepository: CoachRepository,
    private val coach: Coach,
    private val clock: TrainingClock,
) {
    /**
     * What the watch needs, emitted again whenever any of it changes. Null
     * until the person has a goal or has finished setup.
     *
     * The next session is worked out when the state changes, not when the
     * watch starts it. That errs on the safe side: a movement that needed rest
     * then only gets more rested by the time the watch runs the session.
     */
    fun observe(): Flow<PhoneState?> = combine(
        goalRepository.observeActiveGoal(),
        progressRepository.observeBests(),
        equipmentRepository.observeBands(),
        coachRepository.observeState(),
        coachRepository.observeRecentWorkoutIds(),
    ) { path, bests, bands, coachState, recent ->
        if (path == null && coachState == null) {
            null
        } else {
            PhoneState(
                path = path,
                bests = bests,
                bands = bands,
                next = coachState?.let { coach.recommend(it.model, clock.currentTimeMillis()).session },
                recentWorkoutIds = recent,
            )
        }
    }.distinctUntilChanged()
}
