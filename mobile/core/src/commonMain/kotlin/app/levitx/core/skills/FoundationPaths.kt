package app.levitx.core.skills

import app.levitx.core.load.LoadCatalogue

/*
 * Programs for the first strict rep of each basic movement, and the first
 * solid sets after it. The gates follow common coaching progressions; like
 * the rest of the catalogue they need a qualified coach's review before release.
 */

private const val FIRST_PUSH_UP = "first_push_up"

/** From wall push-ups to a first push-up from the floor. */
internal val firstPushUp = SkillPath(
    id = FIRST_PUSH_UP,
    name = "First push-up",
    level = SkillLevel.BEGINNER,
    weeks = 6,
    about =
        "For anyone who can't do a push-up from the floor yet. Each gate puts a little more of " +
            "your weight on your hands.",
    gates = listOf(
        reps(
            "$FIRST_PUSH_UP.wall",
            "20 wall push-ups",
            20,
            "The shape of a push-up: body straight, elbows back at about 45 degrees.",
        ),
        hold(
            "$FIRST_PUSH_UP.plank",
            "30 s plank",
            30,
            "Holding your body in one line, which a push-up needs the whole way down.",
        ),
        reps(
            "$FIRST_PUSH_UP.knees",
            "12 knee push-ups",
            12,
            "Most of the pressing, with less of your weight.",
            LoadCatalogue.pushUpKnees,
        ),
        reps(
            "$FIRST_PUSH_UP.negatives",
            "5 slow push-up negatives",
            5,
            "Lowering for 3 seconds builds the strength to push back up.",
            LoadCatalogue.pushUp,
        ),
        reps("$FIRST_PUSH_UP.push_up", "Push-up", 1, THE_GOAL, LoadCatalogue.pushUp),
    ),
)

private const val TWENTY_PUSH_UPS = "twenty_push_ups"

/** From a few push-ups to twenty in a row. */
internal val twentyPushUps = SkillPath(
    id = TWENTY_PUSH_UPS,
    name = "Twenty push-ups",
    level = SkillLevel.BEGINNER,
    weeks = 8,
    about =
        "For when one push-up is done. Sets build to twenty clean reps in a row, every one to " +
            "a fist from the floor.For when one push-up is done. Sets build to twenty clean reps " +
            "in a row, every one to a fist from the floor.",
    gates = listOf(
        reps("$TWENTY_PUSH_UPS.five", "5 push-ups", 5, "A solid set of five, full range.", LoadCatalogue.pushUp),
        reps(
            "$TWENTY_PUSH_UPS.ten",
            "10 push-ups",
            10,
            "Ten in a row: strength turns into endurance.",
            LoadCatalogue.pushUp,
        ),
        reps(
            "$TWENTY_PUSH_UPS.fifteen",
            "15 push-ups",
            15,
            "Your shoulders and triceps keeping up with your chest.",
            LoadCatalogue.pushUp,
        ),
        reps("$TWENTY_PUSH_UPS.twenty", "20 push-ups", 20, THE_GOAL, LoadCatalogue.pushUp),
    ),
)

private const val FIRST_PULL_UP = "first_pull_up"

/** From no pull-ups to a first strict pull-up. */
internal val firstPullUp = SkillPath(
    id = FIRST_PULL_UP,
    name = "First pull-up",
    level = SkillLevel.BEGINNER,
    weeks = 8,
    about =
        "Built for people at zero. You move up when you pass a gate, so the weeks are an " +
            "estimate, not a deadline.",
    gates = listOf(
        hold("$FIRST_PULL_UP.dead_hang", "30 s dead hang", 30, "Grip strength and comfortable shoulders under load."),
        reps(
            "$FIRST_PULL_UP.scapular_pulls",
            "10 scapular pull-ups",
            10,
            "Starting the pull with your shoulder blades, not your arms.",
        ),
        hold(
            "$FIRST_PULL_UP.flexed_arm_hang",
            "15 s chin-over-bar hold",
            15,
            "Strength at the top, where most first attempts stall.",
        ),
        reps(
            "$FIRST_PULL_UP.negatives",
            "5 slow negatives",
            5,
            "Lowering under control for 3 to 5 seconds builds the pulling strength.",
            LoadCatalogue.pullUp,
        ),
        reps("$FIRST_PULL_UP.pull_up", "Pull-up", 1, THE_GOAL, LoadCatalogue.pullUp),
    ),
)

private const val TEN_PULL_UPS = "ten_pull_ups"

/** From a first pull-up to ten strict in a row. */
internal val tenPullUps = SkillPath(
    id = TEN_PULL_UPS,
    name = "Ten pull-ups",
    level = SkillLevel.INTERMEDIATE,
    weeks = 12,
    about =
        "From your first pull-up to ten strict in a row. Chin-ups along the way give your " +
            "biceps a bigger share.",
    gates = listOf(
        reps("$TEN_PULL_UPS.three", "3 pull-ups", 3, "Your first real set.", LoadCatalogue.pullUp),
        reps("$TEN_PULL_UPS.five", "5 pull-ups", 5, "Five, each from a dead hang.", LoadCatalogue.pullUp),
        reps("$TEN_PULL_UPS.chin_ups", "8 chin-ups", 8, "More reps with palms facing you, to build volume."),
        reps(
            "$TEN_PULL_UPS.seven",
            "7 pull-ups",
            7,
            "Back to the overhand grip, with the extra strength.",
            LoadCatalogue.pullUp,
        ),
        reps("$TEN_PULL_UPS.ten", "10 pull-ups", 10, THE_GOAL, LoadCatalogue.pullUp),
    ),
)

private const val FIRST_DIP = "first_dip"

/** From holding yourself up on bars to a first full dip. */
internal val firstDip = SkillPath(
    id = FIRST_DIP,
    name = "First dip",
    level = SkillLevel.BEGINNER,
    weeks = 6,
    about =
        "From holding yourself up on the bars to your first full dip. Needs parallel bars, or " +
            "two sturdy chairs for the early gates.",
    gates = listOf(
        hold(
            "$FIRST_DIP.support",
            "30 s support hold",
            30,
            "Locked arms and pressed-down shoulders: the top of every dip.",
        ),
        reps("$FIRST_DIP.bench_dips", "10 bench dips", 10, "Bending the elbows under part of your weight."),
        reps(
            "$FIRST_DIP.negatives",
            "5 slow negative dips",
            5,
            "Lowering through the whole dip under control.",
            LoadCatalogue.dip,
        ),
        reps("$FIRST_DIP.dip", "Dip", 1, THE_GOAL, LoadCatalogue.dip),
    ),
)
