package app.levitx.core.coach

/** Milliseconds in an hour. Times in the coach are UTC milliseconds, like the rest of the core. */
internal const val MILLIS_PER_HOUR: Long = 3_600_000L

/** Milliseconds in a day. */
internal const val MILLIS_PER_DAY: Long = 24 * MILLIS_PER_HOUR

/** Days in a week, for rolling weekly windows. */
internal const val DAYS_PER_WEEK: Int = 7

/**
 * Every number the coach decides with, in one place.
 *
 * Some come from the research cited in docs/app/COACH-ENGINE.md. The rest are
 * our own conservative choices. All of them must be reviewed by a qualified
 * coach before release, and keeping them together is what makes that review
 * possible: nothing in the engine hides a threshold of its own.
 */
public object CoachTuning {
    // Recovery

    /**
     * Shortest gap between two hard sessions for the same movement pattern.
     * 36 hours works out as a rest day in between on any normal schedule
     * (Monday evening, then Wednesday morning), where a strict 48 would refuse
     * the Wednesday session.
     */
    public const val MIN_HOURS_BETWEEN_HARD_SESSIONS: Int = 36

    /** After this long a pattern counts as fully rested when sessions are compared. */
    public const val FULL_RECOVERY_HOURS: Int = 72

    /** Hard sets of a pattern's own exercises in one session from which it counts as trained hard that day. */
    public const val HARD_SESSION_MIN_SETS: Int = 2

    // Weekly volume. Targets count a set in full for its own pattern and as a
    // share for patterns it trains indirectly. The safety limits count only a
    // pattern's own sets, so the plan's own targets always fit under them.

    /** A set that trains a pattern indirectly counts this much toward its weekly target (Pelland et al. 2026). */
    public const val INDIRECT_SET_SHARE: Double = 0.5

    /**
     * Hours that count as "this week". It is half a day short of seven days so
     * that the session on the same weekday last week never counts as this
     * week's, whichever of the two was later in the day.
     */
    public const val WEEK_WINDOW_HOURS: Int = 156

    /** Most hard sets of one pattern's own exercises in a week. */
    public const val MAX_WEEKLY_HARD_SETS: Double = 20.0

    /**
     * Hard sets of a pattern's own exercises anyone may do in a week, whatever
     * the weeks before looked like. It equals the biggest target a plan sets,
     * so a new plan fits but nothing can be piled on top in the first weeks.
     */
    public const val STARTING_WEEKLY_HARD_SETS: Double = 15.0

    /** Most a pattern's weekly hard sets may grow over its busiest recent week, as a fraction. */
    public const val MAX_WEEKLY_INCREASE: Double = 0.2

    /** Weeks of load history the growth limit looks back over. */
    public const val LOAD_HISTORY_WEEKS: Int = 4

    /** Weekly hard sets aimed for on the patterns the goal skill uses. */
    public const val GOAL_PATTERN_WEEKLY_SETS: Double = 12.0

    /** Weekly hard sets aimed for on the goal patterns in a focused plan. */
    public const val FOCUSED_GOAL_WEEKLY_SETS: Double = 15.0

    /** Work sets a focused plan adds to each session's skill work. */
    public const val FOCUSED_EXTRA_SKILL_SETS: Int = 1

    /** Weekly hard sets aimed for on every other pattern in an all-round plan. */
    public const val ALL_ROUND_WEEKLY_SETS: Double = 6.0

    /** Weekly hard sets that hold a pattern steady in a focused plan. */
    public const val MAINTENANCE_WEEKLY_SETS: Double = 3.0

    // Session shape

    /** Sets per exercise in a normal session. */
    public const val SETS_PER_EXERCISE: Int = 3

    /** Fewest sets an exercise is cut to when time is short. */
    public const val MIN_SETS_PER_EXERCISE: Int = 2

    /** Sets per exercise in a lighter session. */
    public const val LIGHT_SETS_PER_EXERCISE: Int = 2

    /** A lighter session aims for this share of the normal target. */
    public const val LIGHT_TARGET_FRACTION: Double = 0.8

    /** Reps left in the tank on lighter sets. Far enough from failure not to count as hard. */
    public const val LIGHT_REPS_IN_RESERVE: Int = 4

    /** Rest between lighter sets, in seconds. */
    public const val LIGHT_REST_SECONDS: Int = 60

    /** Minutes of warm-up assumed at the start of every session. */
    public const val WARM_UP_MINUTES: Int = 5

    /** Seconds one rep is assumed to take, for estimating session length. */
    public const val SECONDS_PER_REP: Int = 4

    /** Seconds an average set plus its rest takes, for sizing weekly targets to the time available. */
    public const val AVERAGE_SET_SECONDS: Int = 150

    /** Hold targets move in steps of this many seconds. Rep targets move one rep at a time. */
    public const val HOLD_STEP_SECONDS: Int = 5

    // Learning the person

    /** Sessions on an exercise during which the coach is still finding the person's level. */
    public const val CALIBRATION_SESSIONS: Int = 2

    /** Most rungs a best-effort set can move someone up at once while calibrating. */
    public const val MAX_CALIBRATION_JUMP: Int = 2

    /** Recent results kept per exercise for spotting trends. */
    public const val MARKS_KEPT: Int = 4

    /** Days of session history the model keeps. */
    public const val HISTORY_DAYS: Int = 42

    /** Times an exercise is skipped before the coach treats it as disliked. */
    public const val SKIPS_BEFORE_DISLIKED: Int = 2

    // Readiness

    /** A fall of at least this fraction that lasts two sessions counts as performance dropping. */
    public const val PERFORMANCE_DROP_FRACTION: Double = 0.15

    /** Different kinds of signal needed before the coach suggests easing off. */
    public const val SIGNALS_FOR_SUGGESTION: Int = 2

    /** Suggestions in a row the person kept and then did well on, after which the bar goes up by one. */
    public const val KEPT_SUGGESTIONS_TO_RAISE_BAR: Int = 3

    /** Suggestion outcomes remembered. */
    public const val OUTCOMES_KEPT: Int = 6

    /** Days away after which the next session is a comeback session. */
    public const val COMEBACK_AFTER_DAYS: Int = 14

    /** A session within this many hours of the last one makes "rest today" the suggested choice. */
    public const val REST_SUGGESTED_WITHIN_HOURS: Int = 24

    // Blocks

    /** Weeks of normal training in a block before its lighter week. */
    public const val BLOCK_WEEKS: Int = 5

    /** Weeks into a block before stalled performance can bring the lighter week forward. */
    public const val WEEKS_BEFORE_EARLY_LIGHTER_WEEK: Int = 2

    /** Patterns that must be stalling at once to bring the lighter week forward. */
    public const val STALLED_PATTERNS_FOR_LIGHTER_WEEK: Int = 2

    // Bodyweight

    /** Days of weigh-ins the trend looks at. */
    public const val WEIGHT_TREND_WINDOW_DAYS: Int = 21

    /** Weigh-ins needed before there is a trend at all. */
    public const val WEIGHT_TREND_MIN_READINGS: Int = 3

    /** Days the weigh-ins must span before there is a trend at all. */
    public const val WEIGHT_TREND_MIN_SPAN_DAYS: Int = 10

    /** Days the weigh-ins must span before a loss counts as lasting two weeks. */
    public const val FAST_LOSS_MIN_SPAN_DAYS: Int = 14

    /**
     * Weekly loss, as a fraction of bodyweight, above which progression is held.
     * Guidance for keeping muscle while losing weight is 0.5 to 1% a week
     * (Ruiz-Castellano et al. 2021; Garthe et al. 2011).
     */
    public const val FAST_LOSS_WEEKLY_FRACTION: Double = 0.01

    // Scoring weights. They add up to 1, and novelty is the smallest on purpose.

    /** Weight of progress toward the goal. */
    public const val WEIGHT_GOAL: Double = 0.30

    /** Weight of closing the week's volume gaps. */
    public const val WEIGHT_VOLUME: Double = 0.30

    /** Weight of training rested patterns. */
    public const val WEIGHT_RECOVERY: Double = 0.15

    /** Weight of using the time available. */
    public const val WEIGHT_TIME: Double = 0.15

    /** Weight of avoiding exercises the person keeps skipping. */
    public const val WEIGHT_PREFERENCE: Double = 0.06

    /** Weight of being different from the last session. It only breaks ties. */
    public const val WEIGHT_NOVELTY: Double = 0.04

    /** Hard sets toward the goal in one session that earn the full goal score. */
    public const val GOAL_SETS_FOR_FULL_SCORE: Double = 6.0
}
