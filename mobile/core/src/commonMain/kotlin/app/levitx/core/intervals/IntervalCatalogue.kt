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
    private val quietCardio = IntervalWorkout(
        id = "quiet_cardio",
        name = "Quiet cardio",
        kind = IntervalKind.CARDIO,
        about = "Nothing that jumps. Kind to knees and neighbours.",
        blocks = listOf(
            IntervalBlock(marching, workSeconds = 60, restSeconds = 0, rounds = 2),
            IntervalBlock(stepJacks, workSeconds = 40, restSeconds = 20, rounds = 4),
            IntervalBlock(shadowBoxing, workSeconds = 40, restSeconds = 20, rounds = 4),
            IntervalBlock(mountainClimbers, workSeconds = 30, restSeconds = 30, rounds = 4),
            IntervalBlock(marching, workSeconds = 60, restSeconds = 0, rounds = 1),
        ),
        forBeginners = true,
    )

    private val bodyweightIntervals = IntervalWorkout(
        id = "bodyweight_intervals",
        name = "Bodyweight intervals",
        kind = IntervalKind.CARDIO,
        about = "Thirty hard, thirty easy. Three moves jump.",
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
        // The 20/10 x 8 timing is from Tabata et al. 1996, which used a bike at a far higher
        // intensity than bodyweight moves reach.
        about = "Twenty seconds flat out, ten off, eight times.",
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
        // The format of Little et al. 2010: six of these sessions over two weeks raised the
        // muscles' capacity for endurance work.
        about = "A minute hard, 75 seconds easy, ten times.",
        blocks = listOf(
            IntervalBlock(marching, workSeconds = 60, restSeconds = 0, rounds = 3),
            IntervalBlock(hardMinute, workSeconds = 60, restSeconds = 75, rounds = 10),
        ),
    )

    private val ropeIntervals = IntervalWorkout(
        id = "rope_intervals",
        name = "Rope intervals",
        kind = IntervalKind.CARDIO,
        about = "Easy warm-up, then thirty fast, thirty easy.",
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
        forBeginners = true,
        about = "Spine, hips and upper back, gently.",
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
        forBeginners = true,
        about = "The range pistols, shrimps and L-sits need.",
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
        forBeginners = true,
        about = "Before handstands or hard pushing. Bring a towel or band.",
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
        forBeginners = true,
        about = "Everything, slowly, after a hard week.",
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
        quietCardio,
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

    /** Every move interval workouts are built from. */
    public val moves: List<Exercise> = intervalMoves

    private val movesById: Map<String, Exercise> = intervalMoves.associateBy { it.id }

    /** Finds a workout by its [IntervalWorkout.id], or returns null. */
    public fun byId(id: String): IntervalWorkout? = byId[id]

    /** Finds an interval move by its id, or returns null. */
    public fun moveById(id: String): Exercise? = movesById[id]

    /** The form cues for the interval move with [id], or null. */
    public fun guideFor(id: String): ExerciseGuide? = intervalGuides[id]
}
