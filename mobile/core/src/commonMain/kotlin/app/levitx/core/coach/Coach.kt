package app.levitx.core.coach

/** Something to tell the person once, outside the reason for today's session. */
public enum class CoachNotice {
    /**
     * Bodyweight is falling faster than is good for keeping strength. Said
     * plainly and without judgement, with a suggestion to see a GP if the loss
     * isn't intended. The apps call [AthleteModel.withWeightNoticeShown] once shown.
     */
    FAST_WEIGHT_LOSS,
}

/**
 * What the coach proposes for the next session.
 *
 * @property session The session to do, or null when nothing can be trained safely today.
 * @property reason Why this session.
 * @property suggestion An offer to ease off, when the coach noticed something. Null otherwise.
 * @property notices Things to tell the person once.
 */
public data class Recommendation(
    val session: PlannedSession?,
    val reason: CoachReason,
    val suggestion: Suggestion? = null,
    val notices: Set<CoachNotice> = emptySet(),
)

/**
 * The coach: plans the next session for a person and learns from what they did.
 *
 * It works in three layers (docs/app/adr/0007-adaptive-coach.md):
 * 1. hard rules that are never traded off,
 * 2. a model of the person, updated after every session,
 * 3. scoring that picks the best of the sessions the rules allow.
 *
 * It is deterministic and holds no state of its own. The same model at the
 * same time always gives the same recommendation, so a choice can be tested
 * and a bug report replayed.
 */
public class Coach internal constructor(
    private val builder: SessionBuilder,
    private val rules: List<HardRule>,
    private val scorer: SessionScorer,
) {
    /** A coach with the standard templates, rules and scoring terms. */
    public constructor() : this(SessionBuilder(), HardRules.all, SessionScorer())

    /**
     * The next session for [model] as of [nowEpochMillis].
     *
     * The plan is a rolling queue, not a calendar: this is always "what comes
     * next", whichever day it is asked on.
     */
    public fun recommend(model: AthleteModel, nowEpochMillis: Long): Recommendation {
        val context = CoachContext(model, nowEpochMillis)
        val safe = builder.candidates(context)
            .mapNotNull { HardRules.repair(it, context, rules) }
            .distinctBy { it.session }
        val ranked = safe.map { scorer.score(it.session, context) }.sortedByDescending { it.total }
        val best = ranked.firstOrNull()
        return if (best == null) {
            Recommendation(session = null, reason = CoachReason.RestDay)
        } else {
            val relaxed = safe.first { it.session == best.session }.relaxed.firstOrNull()
            Recommendation(
                session = best.session,
                reason = ReasonPicker.pick(best, ranked.getOrNull(1), relaxed, context),
                suggestion = ReadinessAssessor.assess(best.session, context),
                notices = notices(context),
            )
        }
    }

    /**
     * A workout the person picked for [focus] from the Train tab, as of
     * [nowEpochMillis], or null when nothing in it can be trained safely.
     *
     * It uses the same exercises, targets and safety rules as [recommend], so a
     * movement trained too recently comes back light and a sore area is worked
     * around. It doesn't change the plan: the next recommended session is
     * still whatever comes next once this one is recorded.
     */
    public fun workout(model: AthleteModel, focus: WorkoutFocus, nowEpochMillis: Long): PlannedSession? {
        val context = CoachContext(model, nowEpochMillis)
        return builder.focused(focus, context)?.let { HardRules.repair(it, context, rules)?.session }
    }

    /**
     * The model after the person finished [result]. Call it for every session,
     * including ones started from the Train tab, so the coach knows what was
     * actually done.
     */
    public fun record(model: AthleteModel, result: SessionResult): AthleteModel =
        Progression.apply(model, result, CoachContext(model, result.startedAtEpochMillis))

    /**
     * The model after the person chose to rest at [nowEpochMillis] when the
     * coach suggested easing off. The session they skipped stays next in line.
     */
    public fun recordRest(model: AthleteModel, nowEpochMillis: Long): AthleteModel {
        val outcome = SuggestionOutcome(SuggestionChoice.REST, wentWell = false)
        return model.copy(
            suggestionOutcomes = (model.suggestionOutcomes + outcome).takeLast(CoachTuning.OUTCOMES_KEPT),
            lastEasedAtEpochMillis = nowEpochMillis,
        )
    }

    private fun notices(context: CoachContext): Set<CoachNotice> {
        val tell = context.holdProgress && !context.model.weightNoticeShown
        return if (tell) setOf(CoachNotice.FAST_WEIGHT_LOSS) else emptySet()
    }
}
