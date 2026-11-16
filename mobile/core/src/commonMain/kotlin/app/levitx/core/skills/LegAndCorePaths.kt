package app.levitx.core.skills

/*
 * Programs for one-leg squats, the hamstrings and the core. The gates follow
 * the r/bodyweightfitness progressions and common coaching practice; like the
 * rest of the catalogue they need a qualified coach's review before release.
 */

private const val PISTOL = "pistol_squat"

/** From split squats to a full pistol squat. */
internal val pistolSquat = SkillPath(
    id = PISTOL,
    name = "Pistol squat",
    level = SkillLevel.INTERMEDIATE,
    weeks = 10,
    about =
        "One leg, all the way down and back up. Builds strength, balance and ankle range " +
            "together.",
    gates = listOf(
        hold("$PISTOL.deep_squat", "60 s deep squat hold", 60, "The ankle and hip range the bottom of a pistol needs."),
        reps(
            "$PISTOL.bulgarian",
            "10 Bulgarian split squats each leg",
            10,
            "One-leg strength with the other foot helping.",
        ),
        reps(
            "$PISTOL.to_chair",
            "8 pistols to a chair each leg",
            8,
            "The whole movement, with the hardest part cut off.",
        ),
        reps(
            "$PISTOL.assisted",
            "5 pistols holding a door frame each leg",
            5,
            "Full depth, with your hands taking a little weight.",
        ),
        reps("$PISTOL.pistol", "Pistol squat each leg", 1, THE_GOAL),
    ),
)

private const val SHRIMP = "shrimp_squat"

/** From lunges to a shrimp squat holding the back foot. */
internal val shrimpSquat = SkillPath(
    id = SHRIMP,
    name = "Shrimp squat",
    level = SkillLevel.INTERMEDIATE,
    weeks = 12,
    about =
        "The other one-leg squat: the back knee bends to the floor instead of the front leg " +
            "reaching out. Kinder to tight hamstrings than a pistol.",
    gates = listOf(
        reps("$SHRIMP.lunges", "12 reverse lunges each leg", 12, "Control on one leg with the other one close by."),
        reps("$SHRIMP.bulgarian", "10 Bulgarian split squats each leg", 10, "Most of your weight on the front leg."),
        reps(
            "$SHRIMP.hands_forward",
            "8 shrimp squats, hands forward, each leg",
            8,
            "The shrimp, with your arms as a counterweight.",
        ),
        reps("$SHRIMP.knee_down", "5 shrimp squats, knee to the floor, each leg", 5, "Only the knee touches down."),
        reps("$SHRIMP.holding_foot", "3 shrimp squats holding your foot, each leg", 3, THE_GOAL),
    ),
)

private const val NORDIC = "nordic_curl"

/** From bridges to strict Nordic curls. */
internal val nordicCurl = SkillPath(
    id = NORDIC,
    name = "Nordic curl",
    level = SkillLevel.INTERMEDIATE,
    weeks = 16,
    about =
        "Hamstring strength that guards the knees and helps sprinting. You need something to " +
            "hook your heels under.Hamstring strength that guards the knees and helps sprinting. " +
            "You need something to hook your heels under.",
    gates = listOf(
        reps(
            "$NORDIC.single_leg_bridges",
            "15 single-leg bridges each leg",
            15,
            "Hamstrings and glutes working one side at a time.",
        ),
        reps(
            "$NORDIC.sliding_curls",
            "10 sliding leg curls",
            10,
            "Bending the knee under load, the job a Nordic curl does.",
        ),
        reps(
            "$NORDIC.negatives",
            "5 Nordic curl negatives",
            5,
            "Lowering as slowly as you can: the part that builds the most.",
        ),
        reps("$NORDIC.nordic", "Nordic curl", 1, "Pulling yourself back up for the first time."),
        reps("$NORDIC.three", "3 Nordic curls", 3, THE_GOAL),
    ),
)

private const val L_SIT = "l_sit"

/** From a hollow hold to a 20 second L-sit. */
internal val lSit = SkillPath(
    id = L_SIT,
    name = "L-sit",
    level = SkillLevel.INTERMEDIATE,
    weeks = 10,
    about =
        "Straight legs held level with your hips, on straight arms. Parallettes, dip bars or " +
            "two chairs make it easier to start.",
    gates = listOf(
        hold("$L_SIT.hollow", "30 s hollow hold", 30, "The braced core every L-sit sits on."),
        hold("$L_SIT.tuck", "20 s tuck L-sit", 20, "Pushing down hard with your knees pulled in."),
        hold("$L_SIT.one_leg", "10 s one-leg L-sit", 10, "One leg out straight at a time."),
        hold("$L_SIT.l_sit", "10 s L-sit", 10, "Both legs out."),
        hold("$L_SIT.twenty", "20 s L-sit", 20, THE_GOAL),
    ),
)

private const val DRAGON = "dragon_flag"

/** From a hollow hold to three dragon flags. */
internal val dragonFlag = SkillPath(
    id = DRAGON,
    name = "Dragon flag",
    level = SkillLevel.ADVANCED,
    weeks = 16,
    about =
        "Your whole body held straight, pivoting from your shoulders. Needs a bench or post " +
            "solid enough to hold behind your head.",
    gates = listOf(
        hold("$DRAGON.hollow", "45 s hollow hold", 45, "Keeping your lower back down while your body is long."),
        reps("$DRAGON.leg_raises", "15 lying leg raises", 15, "Lifting your legs without your back arching."),
        reps("$DRAGON.tuck_negatives", "5 tuck dragon flag negatives", 5, "The lowering, with your knees in."),
        reps("$DRAGON.negatives", "5 dragon flag negatives", 5, "The lowering with straight legs."),
        reps("$DRAGON.dragon_flag", "3 dragon flags", 3, THE_GOAL),
    ),
)
