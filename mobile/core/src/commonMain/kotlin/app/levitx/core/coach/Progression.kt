package app.levitx.core.coach

import app.levitx.core.skills.SkillCatalogue

private const val MILLIS_PER_MINUTE = 60_000L

/**
 * How one exercise of a session went, in the terms progression decides on.
 *
 * @property top The best unassisted set of any kind.
 * @property workTop The best work set, which is what sessions are compared on.
 * @property hitTarget True when every planned work set was done and reached its target.
 * @property effort The hardest rating given to a work set, or null when none was given.
 * @property didBestEffort True when a best-effort set was done.
 */
internal data class Outcome(
    val top: Int,
    val workTop: Int,
    val hitTarget: Boolean,
    val effort: Effort?,
    val didBestEffort: Boolean,
) {
    companion object {
        /** Reads the outcome off [result] by lining the sets done up with the sets planned. */
        fun of(result: ExerciseResult): Outcome {
            val done = result.plan.sets.zip(result.sets)
            val work = done.filterNot { it.first.isBestEffort }
            val planned = result.plan.sets.count { !it.isBestEffort }
            return Outcome(
                top = result.gateTop,
                workTop = work.maxOfOrNull { it.second.value } ?: 0,
                hitTarget = work.size == planned && work.all { (set, logged) -> logged.value >= set.target },
                effort = Effort.of(work.mapNotNull { it.second.repsInReserve }.minOrNull()),
                didBestEffort = done.any { it.first.isBestEffort },
            )
        }
    }
}

/**
 * Updates the model from a finished session. This is the only way the model
 * learns from training.
 *
 * It keeps the session's record and hands each exercise to the part that owns
 * its rules: [LadderProgression] for ladder exercises, gate bests for skill
 * work, [BlockKeeper] for the block. Light work never changes a target.
 */
internal object Progression {
    /** The model after [result]. [context] is the decision context from when the session started. */
    fun apply(model: AthleteModel, result: SessionResult, context: CoachContext): AthleteModel {
        val trained = result.results.fold(model) { current, exercise -> learn(current, exercise, result, context) }
        val hard = result.results.filter { it.plan.isHard && !it.skipped }
        val record = SessionRecord(
            finishedAtEpochMillis = result.finishedAtEpochMillis,
            directSets = hard.groupingBy { it.plan.exercise.pattern }.fold(0) { sets, done -> sets + setsDone(done) },
            hardSets = hard.map { it.plan.exercise.setsByPattern(setsDone(it)) }.sumByPattern(),
            exerciseIds = result.results.filterNot { it.skipped }.map { it.plan.exercise.id },
            minutes = ((result.finishedAtEpochMillis - result.startedAtEpochMillis) / MILLIS_PER_MINUTE).toInt(),
        )
        val oldest = result.finishedAtEpochMillis - CoachTuning.HISTORY_DAYS * MILLIS_PER_DAY
        val recorded = trained.copy(
            history = (trained.history + record).filter { it.finishedAtEpochMillis >= oldest },
            suggestionOutcomes = outcomes(model, result),
            // The notice is said once per spell of fast loss, so it resets when the spell ends.
            weightNoticeShown = model.weightNoticeShown && context.holdProgress,
            lastEasedAtEpochMillis = result.finishedAtEpochMillis
                .takeIf { result.choice == SuggestionChoice.LIGHTER } ?: model.lastEasedAtEpochMillis,
        )
        return BlockKeeper.advance(recorded, context)
    }

    private fun learn(
        model: AthleteModel,
        result: ExerciseResult,
        session: SessionResult,
        context: CoachContext,
    ): AthleteModel {
        val id = result.plan.exercise.id
        val gateId = result.plan.gateId
        val at = session.finishedAtEpochMillis
        return when {
            result.skipped -> model.copy(skips = model.skips + (id to (model.skips[id] ?: 0) + 1))
            !result.plan.isHard || result.sets.isEmpty() -> model
            gateId != null -> learnGate(model, result, gateId, context).marking(result, at)
            else -> LadderProgression.learn(model.marking(result, at), result, context)
        }
    }

    /** Skill work: the gate's best goes up, and passing it flags the next gate's exercise. */
    private fun learnGate(
        model: AthleteModel,
        result: ExerciseResult,
        gateId: String,
        context: CoachContext,
    ): AthleteModel {
        val before = model.gateBests[gateId] ?: 0
        val best = maxOf(before, Outcome.of(result).top)
        val target = SkillCatalogue.gateById(gateId)?.target ?: Int.MAX_VALUE
        val next = context.path?.gates?.let { gates -> gates.getOrNull(gates.indexOfFirst { it.id == gateId } + 1) }
        val flagged = next?.takeIf { before < target && best >= target }?.let { gate ->
            gate.id to (model.exercises[gate.id] ?: ExerciseState(target = 1)).copy(note = ProgressNote.MOVED_UP)
        }
        return model.copy(
            gateBests = model.gateBests + (gateId to best),
            exercises = model.exercises + listOfNotNull(flagged),
        )
    }

    /**
     * Adds this session to the exercise's recent results and clears its note,
     * which has now been explained. The target stays as it was: gate targets
     * come from the gate's best, and ladder targets are [LadderProgression]'s to move.
     */
    private fun AthleteModel.marking(result: ExerciseResult, at: Long): AthleteModel {
        val outcome = Outcome.of(result)
        val id = result.plan.exercise.id
        val before = exercises[id] ?: ExerciseState(target = result.plan.workTarget)
        val mark = ExerciseMark(at, outcome.workTop, outcome.effort, outcome.hitTarget)
        val after = before.copy(
            best = maxOf(before.best, outcome.top),
            timesTrained = before.timesTrained + 1,
            marks = (before.marks + mark).takeLast(CoachTuning.MARKS_KEPT),
            note = null,
        )
        return copy(exercises = exercises + (id to after))
    }

    /** Sets actually done, which is what counts toward the week, not what was planned. */
    private fun setsDone(result: ExerciseResult): Int = result.sets.count { it.value > 0 }

    private fun outcomes(model: AthleteModel, result: SessionResult): List<SuggestionOutcome> {
        val wentWell = result.results.filter { it.plan.isHard && !it.skipped }.all { Outcome.of(it).hitTarget }
        val outcome = result.choice?.let { SuggestionOutcome(it, wentWell) }
        return (model.suggestionOutcomes + listOfNotNull(outcome)).takeLast(CoachTuning.OUTCOMES_KEPT)
    }
}
