package app.levitx.phone.exercise

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Exercise
import app.levitx.core.coach.ExerciseCatalogue
import app.levitx.core.coach.ExerciseGuide
import app.levitx.core.coach.ExerciseGuides
import app.levitx.core.coach.ExerciseLookup
import app.levitx.core.coach.bodyweightTrend
import app.levitx.core.skills.SkillCatalogue

/** What an exercise's screen shows. */
sealed interface ExerciseUiState {
    /** Loading. */
    data object Loading : ExerciseUiState

    /** The data couldn't be read on this device. */
    data object Unavailable : ExerciseUiState

    /** No exercise has this id (a back stack saved by an older version). */
    data object NotFound : ExerciseUiState

    /**
     * The exercise and the person's numbers on it.
     *
     * @property exercise The exercise.
     * @property guide How to do it, or null when there's no guide.
     * @property target Reps or seconds the person's work sets aim for, or null until the coach has a figure.
     * @property best Their best set, or null before they have one.
     * @property bodyweightKg Bodyweight for kilograms per rep, or null when none is recorded.
     * @property ladder Its ladder, easiest first, or null for a skill gate's exercise.
     */
    data class Ready(
        val exercise: Exercise,
        val guide: ExerciseGuide?,
        val target: Int?,
        val best: Int?,
        val bodyweightKg: Double?,
        val ladder: List<LadderRung>?,
    ) : ExerciseUiState
}

/** Where a rung sits for the person. */
enum class RungStatus {
    /** Below the rung they train at. */
    DONE,

    /** The rung they train at. */
    CURRENT,

    /** Above it. */
    LATER,
}

/**
 * One rung of a ladder.
 *
 * @property names The exercises on it.
 * @property status Where it sits for the person.
 * @property holdsShown True when it holds the exercise on screen.
 */
data class LadderRung(val names: List<String>, val status: RungStatus, val holdsShown: Boolean)

/** The screen for [exerciseId] given [model] at [nowEpochMillis]. */
fun exerciseState(model: AthleteModel, exerciseId: String, nowEpochMillis: Long): ExerciseUiState {
    val exercise = ExerciseLookup.byId(exerciseId) ?: return ExerciseUiState.NotFound
    val isGate = SkillCatalogue.gateById(exerciseId) != null
    val state = model.exercises[exerciseId]
    val best = if (isGate) model.gateBests[exerciseId] else state?.best
    return ExerciseUiState.Ready(
        exercise = exercise,
        guide = ExerciseGuides.forExercise(exerciseId),
        target = state?.target.takeUnless { isGate },
        best = best?.takeIf { it > 0 },
        bodyweightKg = bodyweightTrend(model.weighIns, nowEpochMillis)?.currentKg,
        ladder = if (isGate) null else ladderFor(model, exercise),
    )
}

private fun ladderFor(model: AthleteModel, exercise: Exercise): List<LadderRung> {
    val ladder = ExerciseCatalogue.ladder(exercise.pattern)
    val owned = model.profile.equipment
    // Without the kit for the rung they reached, people train the highest one they can.
    val current = ladder.usableRungAtOrBelow(model.rung(exercise.pattern), owned) ?: model.rung(exercise.pattern)
    return ladder.rungs.mapIndexed { index, rung ->
        LadderRung(
            names = rung.options.map { it.name },
            status = when {
                index < current -> RungStatus.DONE
                index == current -> RungStatus.CURRENT
                else -> RungStatus.LATER
            },
            holdsShown = rung.options.any { it.id == exercise.id },
        )
    }
}
