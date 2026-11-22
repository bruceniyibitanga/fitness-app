package app.levitx.phone.exercise

import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Exercise
import app.levitx.core.coach.ExerciseCatalogue
import app.levitx.core.coach.ExerciseGuide
import app.levitx.core.coach.ExerciseGuides
import app.levitx.core.coach.ExerciseLookup
import app.levitx.core.coach.bodyweightTrend
import app.levitx.core.coach.currentExercise
import app.levitx.core.coach.swapOptions
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
     * @property ladder Its ladder, easiest first, or null for a skill gate's exercise or a cardio or mobility move.
     * @property swap Whether it can be swapped in or out at the person's level.
     * @property alternatives With [SwapState.CURRENT], the other exercises at the same level they could train.
     */
    data class Ready(
        val exercise: Exercise,
        val guide: ExerciseGuide?,
        val target: Int?,
        val best: Int?,
        val bodyweightKg: Double?,
        val ladder: List<LadderRung>?,
        val swap: SwapState = SwapState.NONE,
        val alternatives: List<Exercise> = emptyList(),
    ) : ExerciseUiState
}

/** Where an exercise stands for swapping, as on the "Exercise detail: swap" artboard. */
enum class SwapState {
    /** Not at the person's level, a skill gate, or the only option there: nothing to swap. */
    NONE,

    /** The one they train for its movement, with others at the same level to swap to. */
    CURRENT,

    /** At their level and within their kit, but not the one they train: it can be swapped in. */
    AVAILABLE,
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
        // Gates and cardio or mobility moves sit on no ladder.
        ladder = if (isGate || !exercise.pattern.isStrength) null else ladderFor(model, exercise),
    ).withSwap(model, isGate)
}

/** Adds whether [ExerciseUiState.Ready.exercise] can be swapped, and what to. Gates never can. */
private fun ExerciseUiState.Ready.withSwap(model: AthleteModel, isGate: Boolean): ExerciseUiState.Ready {
    val options = if (isGate) emptyList() else model.swapOptions(exercise.pattern)
    val swap = when {
        options.size < 2 || options.none { it.id == exercise.id } -> SwapState.NONE
        model.currentExercise(exercise.pattern)?.id == exercise.id -> SwapState.CURRENT
        else -> SwapState.AVAILABLE
    }
    return copy(
        swap = swap,
        alternatives = if (swap ==
            SwapState.CURRENT
        ) {
            options.filter { it.id != exercise.id }
        } else {
            emptyList()
        },
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
