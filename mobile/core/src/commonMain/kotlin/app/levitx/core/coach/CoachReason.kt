package app.levitx.core.coach

/**
 * Why the coach chose today's session, as data. [CoachPhrases] turns it into
 * a sentence. Keeping the two apart means the reason is always what the engine
 * actually did, and the wording can change or be translated without touching it.
 */
public sealed interface CoachReason {
    /** Something changed since last time. */
    public sealed interface Progress : CoachReason

    /** What set this session apart from the others the coach could have picked. */
    public sealed interface Choice : CoachReason

    /** A safety rule shaped the session. */
    public sealed interface Safety : CoachReason

    /** The first sessions: targets are on the easy side while the coach finds the person's level. */
    public data object StillLearning : Progress

    /**
     * A target went up after the last session.
     *
     * @property exerciseName The exercise.
     */
    public data class TargetRaised(val exerciseName: String) : Progress

    /**
     * The person moved up to a harder exercise, or on to the next gate.
     *
     * @property exerciseName The new exercise.
     */
    public data class MovedUp(val exerciseName: String) : Progress

    /**
     * Something was eased back after two sessions short of target.
     *
     * @property exerciseName The exercise.
     */
    public data class EasedBack(val exerciseName: String) : Progress

    /**
     * A new exercise for the new block.
     *
     * @property exerciseName The exercise.
     */
    public data class NewThisBlock(val exerciseName: String) : Progress

    /**
     * The session is built around the goal.
     *
     * @property skillName The goal skill.
     */
    public data class GoalFirst(val skillName: String) : Choice

    /**
     * The session catches up a pattern that is behind this week.
     *
     * @property pattern The pattern.
     * @property setsShort Hard sets it was short of its weekly target, rounded.
     */
    public data class ClosingGap(val pattern: MovementPattern, val setsShort: Int) : Choice

    /**
     * The session leads with what is most rested.
     *
     * @property pattern The most rested pattern in it.
     */
    public data class FreshestFirst(val pattern: MovementPattern) : Choice

    /**
     * The session was the best fit for the time available.
     *
     * @property minutes Its estimated length.
     */
    public data class FitsYourTime(val minutes: Int) : Choice

    /** The session leans on exercises the person doesn't skip. */
    public data object YourKindOfSession : Choice

    /** The session is a different mix from last time. */
    public data object Variety : Choice

    /**
     * The goal's skill work isn't in the session because it needs kit the
     * person hasn't said they have.
     *
     * @property skillName The goal skill.
     * @property equipment The kit it needs.
     */
    public data class NeedsKit(val skillName: String, val equipment: Set<Equipment>) : Choice

    /**
     * A pattern is light or left out because it was trained hard too recently.
     *
     * @property pattern The pattern.
     */
    public data class Recovering(val pattern: MovementPattern) : Safety

    /**
     * A pattern is light or left out because it is at its weekly limit.
     *
     * @property pattern The pattern.
     */
    public data class VolumeCapped(val pattern: MovementPattern) : Safety

    /**
     * The session works around a sore area.
     *
     * @property area The area.
     */
    public data class WorkingAround(val area: BodyArea) : Safety

    /** Targets are held where they are while bodyweight is falling fast. */
    public data object HoldingSteady : Safety

    /** Nothing can be trained safely today. */
    public data object RestDay : Safety
}

/** Works out the one reason to show for a choice. */
internal object ReasonPicker {
    /**
     * The reason for choosing [best]. In order: a safety rule that [relaxed]
     * something in it, a change since last time, still learning, holding for weight,
     * then whichever scoring term set [best] apart from [runnerUp].
     */
    fun pick(best: ScoredSession, runnerUp: ScoredSession?, relaxed: Blocked?, context: CoachContext): CoachReason =
        relaxed?.let(::fromRule)
            ?: needsKit(context)
            ?: fromNote(best.session, context)
            ?: CoachReason.StillLearning.takeIf { context.model.history.size < CoachTuning.CALIBRATION_SESSIONS }
            ?: CoachReason.HoldingSteady.takeIf { context.holdProgress }
            ?: fromTerm(decidingTerm(best, runnerUp), best.session, context)

    private fun needsKit(context: CoachContext): CoachReason? {
        val missing = context.model.kitMissingForGoal()
        val skill = context.path?.name
        return if (missing.isEmpty() || skill == null) null else CoachReason.NeedsKit(skill, missing)
    }

    private fun fromRule(blocked: Blocked): CoachReason? = when (blocked) {
        is Blocked.Pain -> CoachReason.WorkingAround(blocked.area)
        is Blocked.Recovering -> CoachReason.Recovering(blocked.pattern)
        is Blocked.VolumeCapped -> CoachReason.VolumeCapped(blocked.pattern)
        is Blocked.GateOutOfOrder -> null
    }

    /**
     * The biggest change among the session's exercises. [ProgressNote] lists
     * them smallest first, so a new exercise is explained ahead of one more rep.
     */
    private fun fromNote(session: PlannedSession, context: CoachContext): CoachReason? = session.plans
        .mapNotNull { plan -> context.model.exercises[plan.exercise.id]?.note?.let { plan.exercise.name to it } }
        .maxByOrNull { it.second }
        ?.let { (name, note) ->
            when (note) {
                ProgressNote.TARGET_UP -> CoachReason.TargetRaised(name)
                ProgressNote.EASED_BACK -> CoachReason.EasedBack(name)
                ProgressNote.NEW_THIS_BLOCK -> CoachReason.NewThisBlock(name)
                ProgressNote.MOVED_UP -> CoachReason.MovedUp(name)
            }
        }

    /** The term where [best] beat [runnerUp] by most, or its strongest term when there is no runner-up. */
    private fun decidingTerm(best: ScoredSession, runnerUp: ScoredSession?): Term {
        val lead = best.parts.mapValues { (term, part) -> part - (runnerUp?.parts?.get(term) ?: 0.0) }
        return lead.maxBy { it.value }.key
    }

    private fun fromTerm(term: Term, session: PlannedSession, context: CoachContext): CoachReason {
        val skill = context.path?.name
        val furthestBehind = session.hardSets.keys.maxByOrNull { VolumeTerm.gap(it, context) }
        return when {
            term == Term.GOAL && skill != null -> CoachReason.GoalFirst(skill)

            term == Term.RECOVERY -> CoachReason.FreshestFirst(freshest(session, context))

            term == Term.TIME -> CoachReason.FitsYourTime(session.estimatedMinutes)

            term == Term.PREFERENCE -> CoachReason.YourKindOfSession

            term == Term.NOVELTY -> CoachReason.Variety

            furthestBehind != null ->
                CoachReason.ClosingGap(furthestBehind, VolumeTerm.gap(furthestBehind, context).toInt())

            else -> CoachReason.FitsYourTime(session.estimatedMinutes)
        }
    }

    private fun freshest(session: PlannedSession, context: CoachContext): MovementPattern =
        session.plans.map { it.exercise.pattern }.maxBy { RecoveryTerm.rested(it, context) }
}
