package app.levitx.core.coach

import app.levitx.core.pain.PainAdvice
import app.levitx.core.pain.PainAssessment

/**
 * Something that changed for an exercise last time, which the coach explains
 * next time it comes up. Listed from the smallest change to the biggest: when a
 * session has several, the biggest is the one explained.
 */
public enum class ProgressNote {
    /** The target went up. */
    TARGET_UP,

    /** Reps topped out with nothing harder to move to: the lowering slows down instead. */
    SLOWER,

    /** Weight was added: the reps start again from the bottom of the range. */
    WEIGHT_UP,

    /** The target, the weight or the exercise was eased back after two missed sessions. */
    EASED_BACK,

    /** A new exercise for this block, in place of one that trains the same thing. */
    NEW_THIS_BLOCK,

    /** The person moved up to this exercise from an easier one. */
    MOVED_UP,
}

/**
 * How one hard session of an exercise went, kept so trends can be spotted.
 *
 * @property atEpochMillis When, in UTC milliseconds.
 * @property top The best work set, in reps or seconds. Best-effort sets are left out so
 *   sessions with and without one can be compared.
 * @property effort The hardest rating given to a work set, or null when none was given.
 * @property hitTarget True when every work set reached its target.
 */
public data class ExerciseMark(val atEpochMillis: Long, val top: Int, val effort: Effort?, val hitTarget: Boolean)

/**
 * What the coach knows about the person on one exercise.
 *
 * @property target The reps or seconds work sets aim for now.
 * @property best The best unassisted set so far.
 * @property timesTrained Hard sessions done. The coach trusts its numbers more as this grows.
 * @property marks The last few hard sessions, oldest first.
 * @property note A change to explain the next time this exercise is planned.
 * @property addedKg Weight the work sets are done with: a vest, a belt or
 *   plates. 0 for bodyweight. The coach adds it once there is nothing harder to
 *   move to, and takes it off first when easing back.
 * @property slowLowering True once reps topped out with no weight to add, so
 *   each rep is lowered over [CoachTuning.SLOW_LOWER_SECONDS].
 */
public data class ExerciseState(
    val target: Int,
    val best: Int = 0,
    val timesTrained: Int = 0,
    val marks: List<ExerciseMark> = emptyList(),
    val note: ProgressNote? = null,
    val addedKg: Double = 0.0,
    val slowLowering: Boolean = false,
) {
    /** True while the coach is still finding the person's level on this exercise. */
    val isCalibrating: Boolean
        get() = timesTrained < CoachTuning.CALIBRATION_SESSIONS

    /** How sure the coach is of [target], from 0 to 1. */
    val confidence: Double
        get() = (timesTrained.toDouble() / CoachTuning.CALIBRATION_SESSIONS).coerceAtMost(1.0)
}

/**
 * A finished session, boiled down to what planning needs.
 *
 * @property finishedAtEpochMillis When it finished, in UTC milliseconds.
 * @property directSets Hard sets done of each pattern's own exercises. Safety limits count these.
 * @property hardSets Hard sets done per pattern with indirect sets counted as a share.
 *   Weekly targets count these.
 * @property exerciseIds The exercises done, in order.
 * @property minutes How long it took.
 */
public data class SessionRecord(
    val finishedAtEpochMillis: Long,
    val directSets: Map<MovementPattern, Int>,
    val hardSets: Map<MovementPattern, Double>,
    val exerciseIds: List<String>,
    val minutes: Int,
)

/**
 * Where the person is in the current training block.
 *
 * A block is counted in sessions, not calendar weeks, so someone who trains
 * less often than planned isn't sent into a lighter week they haven't earned.
 *
 * @property index Which block this is, from 0. Accessory exercises rotate with it.
 * @property sessionsDone Normal sessions done in this block.
 * @property lighterSessionsLeft Sessions left in the block's lighter week. 0 outside it.
 */
public data class TrainingBlock(val index: Int = 0, val sessionsDone: Int = 0, val lighterSessionsLeft: Int = 0) {
    /** True during the lighter week that ends a block. */
    val isLighterWeek: Boolean
        get() = lighterSessionsLeft > 0

    /** The next block, started fresh. */
    public fun next(): TrainingBlock = TrainingBlock(index = index + 1)
}

/**
 * How a suggestion to ease off turned out.
 *
 * @property choice What the person picked.
 * @property wentWell True when they then hit their targets.
 */
public data class SuggestionOutcome(val choice: SuggestionChoice, val wentWell: Boolean)

/**
 * Everything the coach knows about one person. It is plain data, so it can be
 * stored in the encrypted database, inspected and replayed in tests.
 *
 * It is immutable. [Coach.record] and the `with` functions return the next
 * model. It holds health data (pain and bodyweight), so it stays on the phone.
 *
 * @property profile What they told us at setup.
 * @property rungs The rung they train at on each pattern's ladder. Missing means the first.
 * @property exercises What we know per exercise, by [Exercise.id].
 * @property gateBests Their best result per skill gate id.
 * @property history Recent sessions, oldest first by when they finished.
 * @property pain The latest advice per sore body area. Areas that are fine aren't listed.
 * @property weighIns Recent bodyweight readings, oldest first.
 * @property block Where they are in the current block.
 * @property suggestionOutcomes How the last few suggestions turned out, oldest first.
 * @property skips Times each exercise was skipped or swapped out, by [Exercise.id].
 * @property preferred The exercise the person chose for a movement, by [Exercise.id], used
 *   while it is an option on the rung they train at. See [withPreferred].
 * @property weightNoticeShown True once the fast weight loss notice has been shown for the current spell.
 * @property weighInAskedAtEpochMillis When the quick weigh-in last showed, saved or skipped,
 *   or null if never. See [WeighInPrompt].
 * @property weekReviewedAtEpochMillis When the person last opened a week review, or null if never.
 * @property lastEasedAtEpochMillis When they last took a lighter session or a rest the coach
 *   offered. Results from before then no longer count as a trend: the easier day was the answer to them.
 */
public data class AthleteModel(
    val profile: CoachProfile,
    val rungs: Map<MovementPattern, Int> = emptyMap(),
    val exercises: Map<String, ExerciseState> = emptyMap(),
    val gateBests: Map<String, Int> = emptyMap(),
    val history: List<SessionRecord> = emptyList(),
    val pain: Map<BodyArea, PainAdvice> = emptyMap(),
    val weighIns: List<WeighIn> = emptyList(),
    val block: TrainingBlock = TrainingBlock(),
    val suggestionOutcomes: List<SuggestionOutcome> = emptyList(),
    val skips: Map<String, Int> = emptyMap(),
    val preferred: Map<MovementPattern, String> = emptyMap(),
    val weightNoticeShown: Boolean = false,
    val lastEasedAtEpochMillis: Long? = null,
    val weighInAskedAtEpochMillis: Long? = null,
    val weekReviewedAtEpochMillis: Long? = null,
) {
    /** The rung trained at for [pattern]. */
    public fun rung(pattern: MovementPattern): Int = rungs[pattern] ?: 0

    /**
     * Hard sets counted toward [pattern]'s weekly target after [fromEpochMillis]
     * and up to [toEpochMillis], indirect sets included as a share.
     */
    public fun hardSets(pattern: MovementPattern, fromEpochMillis: Long, toEpochMillis: Long): Double =
        between(fromEpochMillis, toEpochMillis).sumOf { it.hardSets[pattern] ?: 0.0 }

    /** Hard sets of [pattern]'s own exercises after [fromEpochMillis] and up to [toEpochMillis]. */
    public fun directSets(pattern: MovementPattern, fromEpochMillis: Long, toEpochMillis: Long): Int =
        between(fromEpochMillis, toEpochMillis).sumOf { it.directSets[pattern] ?: 0 }

    /**
     * Records the result of a pain check-in for [area]. Advice to carry on
     * clears the area; anything else replaces what was there.
     */
    public fun withPain(area: BodyArea, assessment: PainAssessment): AthleteModel {
        val settled = assessment.advice == PainAdvice.CONTINUE_AS_PLANNED
        return copy(pain = if (settled) pain - area else pain + (area to assessment.advice))
    }

    /**
     * The areas on light work that [result] loaded, in [BodyArea] order: the
     * ones worth asking about after the session, because the answer can put
     * them back to normal. Areas taken out of the plan aren't listed, as no
     * planned session loads them. Skipped exercises load nothing.
     */
    public fun easedAreasLoadedBy(result: SessionResult): List<BodyArea> {
        val loaded = result.results
            .filter { it.sets.isNotEmpty() }
            .flatMapTo(mutableSetOf()) { it.plan.exercise.pattern.loads }
        return BodyArea.entries.filter { it in loaded && pain[it] == PainAdvice.EASE_OFF }
    }

    /** Adds a bodyweight reading, dropping ones too old for the trend. */
    public fun withWeighIn(weighIn: WeighIn): AthleteModel {
        val oldest = weighIn.atEpochMillis - CoachTuning.HISTORY_DAYS * MILLIS_PER_DAY
        return copy(weighIns = (weighIns + weighIn).filter { it.atEpochMillis >= oldest }.sortedBy { it.atEpochMillis })
    }

    /** Marks the fast weight loss notice as shown, so it is said once and not repeated. */
    public fun withWeightNoticeShown(): AthleteModel = copy(weightNoticeShown = true)

    /** Notes that the quick weigh-in showed at [atEpochMillis], whether it was saved or skipped. */
    public fun withWeighInAsked(atEpochMillis: Long): AthleteModel = copy(weighInAskedAtEpochMillis = atEpochMillis)

    /** Notes that the person opened a week review at [atEpochMillis]. */
    public fun withWeekReviewed(atEpochMillis: Long): AthleteModel = copy(weekReviewedAtEpochMillis = atEpochMillis)

    private fun between(fromEpochMillis: Long, toEpochMillis: Long): List<SessionRecord> =
        history.filter { it.finishedAtEpochMillis > fromEpochMillis && it.finishedAtEpochMillis <= toEpochMillis }

    /** Starting points. */
    public companion object {
        /**
         * The model for someone who has just finished setup. Every ladder
         * starts at its easiest rung and the first sessions find the real
         * level, except pulling, where a "yes" to [pullUp] starts at pull-ups,
         * and any movement in the optional quick test, which [tested] places.
         * A pull-up count in [tested] wins over the [pullUp] answer.
         *
         * @param tested Quick test results, as reps per test. Empty when skipped.
         * @throws IllegalArgumentException if a result is out of its test's range.
         */
        public fun starting(
            profile: CoachProfile,
            pullUp: PullUpAnswer,
            tested: Map<QuickTest, Int> = emptyMap(),
        ): AthleteModel {
            val pullUpRung = ExerciseCatalogue.ladder(MovementPattern.VERTICAL_PULL).rungs
                .indexOfFirst { rung -> rung.options.any { it.id == PULL_UP_ID } }
            val canPullUp = pullUp == PullUpAnswer.YES
            val rungs = if (canPullUp) mapOf(MovementPattern.VERTICAL_PULL to pullUpRung) else emptyMap()
            return tested.entries.fold(AthleteModel(profile = profile, rungs = rungs)) { model, (test, reps) ->
                QuickTestPlacement.place(model, test, reps)
            }
        }
    }
}

/**
 * The last session finished by [asOfEpochMillis], or null when there is none.
 * Sessions dated after it are left out: they can only come from a clock that
 * was ahead, or be ones the caller is looking back from, and neither has
 * happened yet as far as a decision at that time goes.
 */
public fun AthleteModel.lastSession(asOfEpochMillis: Long): SessionRecord? =
    upTo(asOfEpochMillis).maxByOrNull { it.finishedAtEpochMillis }

/**
 * When [pattern] was last trained hard by [asOfEpochMillis], or null if it
 * hasn't been in the history kept. Like [lastSession], it ignores sessions
 * dated later, so one wrong date can't keep a movement "resting".
 */
public fun AthleteModel.lastHardAt(pattern: MovementPattern, asOfEpochMillis: Long): Long? = upTo(asOfEpochMillis)
    .filter { (it.directSets[pattern] ?: 0) >= CoachTuning.HARD_SESSION_MIN_SETS }
    .maxOfOrNull { it.finishedAtEpochMillis }

private fun AthleteModel.upTo(epochMillis: Long): List<SessionRecord> =
    history.filter { it.finishedAtEpochMillis <= epochMillis }

/** The pull-up on the vertical pull ladder, where a "yes" at setup starts. */
private const val PULL_UP_ID = "vertical_pull.pull_up"
