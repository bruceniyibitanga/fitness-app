package app.levitx.core.intervals

import app.levitx.core.coach.Exercise
import app.levitx.core.coach.ExerciseGuide

/**
 * The built-in cardio and mobility workouts. Sources are in COACH-ENGINE.md:
 * Tabata et al. 1996 for 20 seconds on and 10 off, Little et al. 2010 for ten
 * one-minute intervals, and ACSM's 2011 position stand for holding each
 * stretch around 30 seconds to a minute in all.
 */
public object IntervalCatalogue {
    private val bodyweightIntervals = IntervalWorkout(
        id = "bodyweight_intervals",
        name = "Bodyweight intervals",
        kind = IntervalKind.CARDIO,
        about = "Thirty seconds hard, thirty easy, through four moves. No kit, and quiet enough for a flat if you " +
            "step instead of jump.",
        blocks = listOf(
            IntervalBlock(jumpingJacks, workSeconds = 40, restSeconds = 20, rounds = 3),
            IntervalBlock(burpees, workSeconds = 30, restSeconds = 30, rounds = 4),
            IntervalBlock(mountainClimbers, workSeconds = 30, restSeconds = 30, rounds = 4),
            IntervalBlock(squatJumps, workSeconds = 30, restSeconds = 30, rounds = 4),
            IntervalBlock(highKnees, workSeconds = 30, restSeconds = 30, rounds = 4),
        ),
    )

    private val tabata = IntervalWorkout(
        id = "tabata",
        name = "Tabata",
        kind = IntervalKind.CARDIO,
        about =
            "Twenty seconds flat out, ten seconds rest, eight times, for three moves. The timing comes from Tabata's " +
                "1996 study, which used a bike at a far higher intensity than bodyweight moves reach.",
        blocks = listOf(
            IntervalBlock(jumpingJacks, workSeconds = 40, restSeconds = 20, rounds = 2, restAfterSeconds = 30),
            IntervalBlock(burpees, workSeconds = 20, restSeconds = 10, rounds = 8, restAfterSeconds = 60),
            IntervalBlock(highKnees, workSeconds = 20, restSeconds = 10, rounds = 8, restAfterSeconds = 60),
            IntervalBlock(mountainClimbers, workSeconds = 20, restSeconds = 10, rounds = 8),
        ),
    )

    private val oneMinuteIntervals = IntervalWorkout(
        id = "one_minute_intervals",
        name = "Ten one-minute intervals",
        kind = IntervalKind.CARDIO,
        about = "A minute hard, then 75 seconds easy, ten times. The format of Little and colleagues' 2010 study, " +
            "where six of these sessions over two weeks raised the muscles' capacity for endurance work.",
        blocks = listOf(
            IntervalBlock(marching, workSeconds = 60, restSeconds = 0, rounds = 3),
            IntervalBlock(hardMinute, workSeconds = 60, restSeconds = 75, rounds = 10),
        ),
    )

    private val ropeIntervals = IntervalWorkout(
        id = "rope_intervals",
        name = "Rope intervals",
        kind = IntervalKind.CARDIO,
        about = "Easy skipping to warm up, then thirty seconds fast and thirty easy, ten times.",
        blocks = listOf(
            IntervalBlock(ropeEasy, workSeconds = 60, restSeconds = 30, rounds = 3),
            IntervalBlock(ropeFast, workSeconds = 30, restSeconds = 30, rounds = 10),
            IntervalBlock(ropeEasy, workSeconds = 60, restSeconds = 0, rounds = 2),
        ),
    )

    private val morningMobility = IntervalWorkout(
        id = "morning_mobility",
        name = "Morning mobility",
        kind = IntervalKind.MOBILITY,
        about = "Spine, hips and upper back, gently. Good on rest days or before training.",
        blocks = listOf(
            IntervalBlock(catCow, workSeconds = 45, restSeconds = 10, rounds = 2),
            IntervalBlock(worldsGreatest, workSeconds = 30, restSeconds = 10, rounds = 2),
            IntervalBlock(openBook, workSeconds = 30, restSeconds = 10, rounds = 2),
            IntervalBlock(hipFlexor, workSeconds = 30, restSeconds = 10, rounds = 2),
            IntervalBlock(deepSquat, workSeconds = 60, restSeconds = 0, rounds = 1),
        ),
    )

    private val hipsAndHamstrings = IntervalWorkout(
        id = "hips_and_hamstrings",
        name = "Hips and hamstrings",
        kind = IntervalKind.MOBILITY,
        about = "The range pistols, shrimps and L-sits need. Each stretch adds up to a minute a side.",
        blocks = listOf(
            IntervalBlock(hipFlexor, workSeconds = 30, restSeconds = 10, rounds = 2),
            IntervalBlock(pigeon, workSeconds = 30, restSeconds = 10, rounds = 2),
            IntervalBlock(hamstring, workSeconds = 30, restSeconds = 10, rounds = 2),
            IntervalBlock(frog, workSeconds = 45, restSeconds = 10, rounds = 2),
            IntervalBlock(deepSquat, workSeconds = 60, restSeconds = 0, rounds = 1),
        ),
    )

    private val shouldersAndWrists = IntervalWorkout(
        id = "shoulders_and_wrists",
        name = "Shoulders and wrists",
        kind = IntervalKind.MOBILITY,
        about = "Before handstands, planche work or a hard pushing day. You'll want a towel or a band.",
        blocks = listOf(
            IntervalBlock(wrists, workSeconds = 30, restSeconds = 10, rounds = 2),
            IntervalBlock(passThrough, workSeconds = 40, restSeconds = 10, rounds = 2),
            IntervalBlock(doorwayChest, workSeconds = 30, restSeconds = 10, rounds = 2),
            IntervalBlock(threadNeedle, workSeconds = 30, restSeconds = 10, rounds = 2),
            IntervalBlock(childsPose, workSeconds = 45, restSeconds = 0, rounds = 1),
        ),
    )

    private val fullBodyStretch = IntervalWorkout(
        id = "full_body_stretch",
        name = "Full body stretch",
        kind = IntervalKind.MOBILITY,
        about = "Everything, slowly, after a hard week. About a minute per stretch in all.",
        blocks = listOf(
            IntervalBlock(catCow, workSeconds = 45, restSeconds = 10, rounds = 1),
            IntervalBlock(childsPose, workSeconds = 45, restSeconds = 10, rounds = 1),
            IntervalBlock(couchStretch, workSeconds = 30, restSeconds = 10, rounds = 2),
            IntervalBlock(pigeon, workSeconds = 30, restSeconds = 10, rounds = 2),
            IntervalBlock(hamstring, workSeconds = 30, restSeconds = 10, rounds = 2),
            IntervalBlock(threadNeedle, workSeconds = 30, restSeconds = 10, rounds = 2),
            IntervalBlock(doorwayChest, workSeconds = 30, restSeconds = 10, rounds = 2),
            IntervalBlock(deepSquat, workSeconds = 60, restSeconds = 0, rounds = 1),
        ),
    )

    /** Every built-in interval workout, cardio first. */
    public val all: List<IntervalWorkout> = listOf(
        bodyweightIntervals,
        tabata,
        oneMinuteIntervals,
        ropeIntervals,
        morningMobility,
        hipsAndHamstrings,
        shouldersAndWrists,
        fullBodyStretch,
    )

    private val byId: Map<String, IntervalWorkout> = all.associateBy { it.id }

    private val movesById: Map<String, Exercise> = intervalMoves.associateBy { it.id }

    /** Finds a workout by its [IntervalWorkout.id], or returns null. */
    public fun byId(id: String): IntervalWorkout? = byId[id]

    /** Finds an interval move by its id, or returns null. */
    public fun moveById(id: String): Exercise? = movesById[id]

    /** The form cues for the interval move with [id], or null. */
    public fun guideFor(id: String): ExerciseGuide? = intervalGuides[id]
}
