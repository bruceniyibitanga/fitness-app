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
     * The plan is worked out when the state changes, not when the watch
     * starts it, and that is usually straight after a workout, when
     * everything just trained can only be done light. So it carries two
     * sessions: the one for now, and the one for once the rest is over
     * (`Coach.planAhead`). The watch picks by its own clock when the person
     * starts, and gets the session the phone would have given at that time.
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
                next = coachState?.let { coach.planAhead(it.model, clock.currentTimeMillis()) },
                recentWorkoutIds = recent,
            )
        }
    }.distinctUntilChanged()
}
