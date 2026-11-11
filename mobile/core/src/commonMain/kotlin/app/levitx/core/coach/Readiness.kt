package app.levitx.core.coach

private const val MARKS_FOR_A_TREND = 3

/** Something the coach noticed that may mean today should be easier. */
public sealed interface ReadinessSignal {
    /**
     * Results on an exercise fell and stayed down for two sessions.
     *
     * @property exerciseName The exercise.
     * @property recentTops The best set of each of those sessions, oldest first.
     */
    public data class PerformanceDrop(val exerciseName: String, val recentTops: List<Int>) : ReadinessSignal

    /**
     * The last two sessions of an exercise were both rated hard.
     *
     * @property exerciseName The exercise.
     */
    public data class FeltHard(val exerciseName: String) : ReadinessSignal

    /**
     * Bodyweight is falling fast.
     *
     * @property weeklyChange Change per week as a fraction of bodyweight. Negative.
     */
    public data class FastWeightLoss(val weeklyChange: Double) : ReadinessSignal

    /**
     * A long time since the last session.
     *
     * @property days Whole days since then.
     */
    public data class LongGap(val days: Int) : ReadinessSignal

    /** The block's planned lighter week. */
    public data object LighterWeek : ReadinessSignal
}

/**
 * An offer to ease off, with what the coach noticed. The person always
 * decides: nothing is skipped or lightened without them choosing it.
 *
 * @property signals What was noticed, most important first.
 * @property recommended The choice the coach would make.
 * @property lighter The session to do if they pick [SuggestionChoice.LIGHTER].
 */
public data class Suggestion(
    val signals: List<ReadinessSignal>,
    val recommended: SuggestionChoice,
    val lighter: PlannedSession,
)

/**
 * Reads trends out of an exercise's recent results. Readiness and block timing
 * both use it.
 *
 * A trend only counts when its newest result came after [since], the last time
 * the person took an easier day. That day was the answer to what came before
 * it, so it takes a fresh result that is still down to raise the matter again.
 */
internal class PerformanceTrend(private val since: Long?) {
    /**
     * Results fell and stayed down: the last two sessions were both below the
     * one before them, the latest by a real margin. One bad day isn't a trend,
     * and neither is a rep's wobble.
     */
    fun isDropping(state: ExerciseState): Boolean {
        val tops = recentTops(state)
        return isFresh(state) &&
            tops.size == MARKS_FOR_A_TREND &&
            tops[1] < tops[0] &&
            tops[2] <= tops[1] &&
            tops[2] <= tops[0] * (1 - CoachTuning.PERFORMANCE_DROP_FRACTION)
    }

    /** The last two sessions were both rated hard. */
    fun feltHard(state: ExerciseState): Boolean {
        val recent = state.marks.takeLast(2)
        return isFresh(state) && recent.size == 2 && recent.all { it.effort == Effort.HARD }
    }

    /** The best work set of each of the last three sessions, oldest first. */
    fun recentTops(state: ExerciseState): List<Int> = state.marks.takeLast(MARKS_FOR_A_TREND).map { it.top }

    private fun isFresh(state: ExerciseState): Boolean {
        val newest = state.marks.lastOrNull()?.atEpochMillis
        return newest != null && (since == null || newest > since)
    }
}

/**
 * Decides whether to offer an easier day.
 *
 * A long gap or a planned lighter week is enough alone. Otherwise it takes
 * [CoachTuning.SIGNALS_FOR_SUGGESTION] different kinds of signal pointing the
 * same way, because any single one is too noisy to act on. Pain isn't a signal
 * here: it works through the hard rules and needs nobody's agreement.
 */
internal object ReadinessAssessor {
    /** The suggestion to show before [session], or null when there is nothing to suggest. */
    fun assess(session: PlannedSession, context: CoachContext): Suggestion? {
        val strong = strongSignal(context)
        val signals = strong?.let(::listOf) ?: softSignals(session, context)
        val needed = if (strong != null) 1 else signalsNeeded(context.model)
        val enough = !session.isLight && signals.map { it::class }.distinct().size >= needed
        return if (enough) Suggestion(signals, recommended(context), session.lightened()) else null
    }

    private fun strongSignal(context: CoachContext): ReadinessSignal? {
        val daysAway = context.hoursSinceLastSession()?.let { (it / HOURS_PER_DAY).toInt() } ?: 0
        return when {
            daysAway >= CoachTuning.COMEBACK_AFTER_DAYS -> ReadinessSignal.LongGap(daysAway)
            context.model.block.isLighterWeek -> ReadinessSignal.LighterWeek
            else -> null
        }
    }

    private fun softSignals(session: PlannedSession, context: CoachContext): List<ReadinessSignal> {
        val trend = context.trend
        val trained = session.plans.filter { it.isHard }.mapNotNull { plan ->
            context.model.exercises[plan.exercise.id]?.let { plan.exercise.name to it }
        }
        val drops = trained.filter { trend.isDropping(it.second) }
            .map { ReadinessSignal.PerformanceDrop(it.first, trend.recentTops(it.second)) }
        val hard = trained.filter { trend.feltHard(it.second) }.map { ReadinessSignal.FeltHard(it.first) }
        val weight = context.weightTrend?.takeIf { it.isFastLoss }?.weeklyChange?.let(ReadinessSignal::FastWeightLoss)
        return drops + hard + listOfNotNull(weight)
    }

    /**
     * Kinds of signal needed. One more than usual for someone who keeps
     * choosing to carry on after a suggestion and then does well.
     */
    private fun signalsNeeded(model: AthleteModel): Int {
        val recent = model.suggestionOutcomes.takeLast(CoachTuning.KEPT_SUGGESTIONS_TO_RAISE_BAR)
        val shruggedOff = recent.size == CoachTuning.KEPT_SUGGESTIONS_TO_RAISE_BAR &&
            recent.all { it.choice == SuggestionChoice.KEEP && it.wentWell }
        return CoachTuning.SIGNALS_FOR_SUGGESTION + if (shruggedOff) 1 else 0
    }

    private fun recommended(context: CoachContext): SuggestionChoice {
        val hours = context.hoursSinceLastSession()
        val trainedVeryRecently = hours != null && hours < CoachTuning.REST_SUGGESTED_WITHIN_HOURS
        return if (trainedVeryRecently) SuggestionChoice.REST else SuggestionChoice.LIGHTER
    }
}

private const val HOURS_PER_DAY = 24
