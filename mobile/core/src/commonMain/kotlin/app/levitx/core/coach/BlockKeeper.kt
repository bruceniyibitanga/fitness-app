package app.levitx.core.coach

/**
 * Moves the person through training blocks: so many normal sessions, then a
 * lighter week, then a new block with fresh accessory exercises.
 */
internal object BlockKeeper {
    /**
     * The model with its block moved on by the session just recorded.
     * [context] is from when that session started, so its gap is the time away before it.
     */
    fun advance(model: AthleteModel, context: CoachContext): AthleteModel {
        val perWeek = model.profile.sessionsPerWeek
        val block = model.block
        val backFromBreak = (context.hoursSinceLastSession() ?: 0.0) >= CoachTuning.COMEBACK_AFTER_DAYS * HOURS_PER_DAY
        val next = when {
            // Time away was the rest a lighter week is for.
            block.isLighterWeek && (backFromBreak || block.lighterSessionsLeft == 1) -> block.next()

            block.isLighterWeek -> block.copy(lighterSessionsLeft = block.lighterSessionsLeft - 1)

            lighterWeekIsDue(model, context) -> block.copy(lighterSessionsLeft = perWeek)

            else -> block.copy(sessionsDone = block.sessionsDone + 1)
        }
        return if (next.index == block.index) model.copy(block = next) else startBlock(model, next, context)
    }

    private fun lighterWeekIsDue(model: AthleteModel, context: CoachContext): Boolean {
        val perWeek = model.profile.sessionsPerWeek
        val done = model.block.sessionsDone + 1
        val stalled = MovementPattern.entries.count { pattern ->
            context.exerciseFor(pattern)?.let { model.exercises[it.id] }?.let(context.trend::isDropping) == true
        }
        val early = stalled >= CoachTuning.STALLED_PATTERNS_FOR_LIGHTER_WEEK &&
            done >= perWeek * CoachTuning.WEEKS_BEFORE_EARLY_LIGHTER_WEEK
        return early || done >= perWeek * CoachTuning.BLOCK_WEEKS
    }

    /** Starts [next], flagging every accessory exercise that changes so the coach can say so. */
    private fun startBlock(model: AthleteModel, next: TrainingBlock, context: CoachContext): AthleteModel {
        val goal = context.goalPatterns
        val fresh = MovementPattern.entries.mapNotNull { pattern ->
            val arriving = model.exerciseFor(pattern, goal, next.index)
            arriving?.takeIf { it != model.exerciseFor(pattern, goal) }
        }
        val flagged = fresh.associate { exercise ->
            val state = model.exercises[exercise.id] ?: ExerciseState(target = exercise.range.first)
            exercise.id to state.copy(note = ProgressNote.NEW_THIS_BLOCK)
        }
        return model.copy(block = next, exercises = model.exercises + flagged)
    }
}

private const val HOURS_PER_DAY = 24
