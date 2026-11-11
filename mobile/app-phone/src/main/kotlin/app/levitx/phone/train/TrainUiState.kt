package app.levitx.phone.train

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.PlannedSession
import app.levitx.core.coach.WorkoutFocus
import app.levitx.core.skills.SkillCatalogue
import app.levitx.core.skills.SkillPath

/** What the Train tab shows. */
sealed interface TrainUiState {
    /** Loading. */
    data object Loading : TrainUiState

    /** The data couldn't be read on this device. */
    data object Unavailable : TrainUiState

    /**
     * The programs and workouts to pick from.
     *
     * @property programs Every skill program.
     * @property workouts One workout per focus, in [WorkoutFocus] order.
     */
    data class Ready(val programs: List<ProgramSummary>, val workouts: List<WorkoutSummary>) : TrainUiState
}

/**
 * A skill program and how far the person is through it.
 *
 * @property path The program.
 * @property passed Gates already passed.
 * @property isCurrent True when it is the person's goal now.
 */
data class ProgramSummary(val path: SkillPath, val passed: Int, val isCurrent: Boolean)

/**
 * A workout the person could start now.
 *
 * @property focus What it works.
 * @property session What the coach would have them do, or null when none of it is safe today.
 */
data class WorkoutSummary(val focus: WorkoutFocus, val session: PlannedSession?)

/** Every program, with the person's progress through it. */
internal fun programsFor(model: AthleteModel): List<ProgramSummary> = SkillCatalogue.all.map { path ->
    val remaining = path.progressFor(model.gateBests).gatesRemaining
    ProgramSummary(path, passed = path.gates.size - remaining, isCurrent = model.profile.goalPathId == path.id)
}

/** The workout for every focus as of [nowEpochMillis]. */
internal fun workoutsFor(model: AthleteModel, coach: Coach, nowEpochMillis: Long): List<WorkoutSummary> =
    WorkoutFocus.entries.map { WorkoutSummary(it, coach.workout(model, it, nowEpochMillis)) }
