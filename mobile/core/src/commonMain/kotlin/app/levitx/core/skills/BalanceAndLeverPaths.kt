package app.levitx.core.skills

/*
 * Programs for balancing on the hands and for the straight-arm holds. The
 * straight-arm gates move at tendon pace: tendons take months to catch up with
 * muscle, so their holds are short and their weeks are long. Like the rest of
 * the catalogue, they need a qualified coach's review before release.
 */

private const val HANDSTAND = "handstand"

/** From a wall plank to a 30 second freestanding handstand. */
internal val handstand = SkillPath(
    id = HANDSTAND,
    name = "Handstand foundations",
    level = SkillLevel.INTERMEDIATE,
    weeks = 10,
    about =
        "Balancing on your hands, built up against a wall first. A few minutes most days helps " +
            "more than one long session.",
    gates = listOf(
        hold(
            "$HANDSTAND.wall_plank",
            "30 s wall plank",
            30,
            "Feet on the wall, hands on the floor: your first time upside down.",
        ),
        hold("$HANDSTAND.crow", "20 s crow pose", 20, "Balancing on your hands, close to the floor."),
        hold(
            "$HANDSTAND.chest_to_wall",
            "60 s chest-to-wall handstand",
            60,
            "The straight line, and the shoulder strength to hold it.",
        ),
        hold(
            "$HANDSTAND.toes_on_wall",
            "30 s handstand, toes on the wall",
            30,
            "Balancing with less and less help from the wall.",
        ),
        hold("$HANDSTAND.freestanding", "10 s freestanding handstand", 10, "Nothing to lean on."),
        hold("$HANDSTAND.thirty", "30 s freestanding handstand", 30, THE_GOAL),
    ),
)

private const val HSPU = "handstand_push_up"

/** From pike push-ups to five wall handstand push-ups. */
internal val handstandPushUp = SkillPath(
    id = HSPU,
    name = "Handstand push-up",
    level = SkillLevel.ADVANCED,
    weeks = 16,
    about =
        "Pressing your whole body overhead, against a wall. A steady chest-to-wall handstand " +
            "comes first.",
    gates = listOf(
        reps("$HSPU.pike", "10 pike push-ups, feet raised", 10, "Pressing overhead with most of your weight."),
        hold("$HSPU.wall_hold", "60 s chest-to-wall handstand", 60, "Being comfortable upside down for a whole set."),
        reps("$HSPU.negatives", "5 wall handstand push-up negatives", 5, "Lowering to your head under control."),
        reps("$HSPU.one", "Wall handstand push-up", 1, "Pressing back up for the first time."),
        reps("$HSPU.five", "5 wall handstand push-ups", 5, THE_GOAL),
    ),
)

private const val BACK_LEVER = "back_lever"

/** From a German hang to a 5 second back lever. */
internal val backLever = SkillPath(
    id = BACK_LEVER,
    name = "Back lever",
    level = SkillLevel.ADVANCED,
    weeks = 26,
    about =
        "Hanging face down, body level behind the bar. Straight-arm strength in the chest, " +
            "biceps and shoulders. Take it slowly.",
    gates = listOf(
        hold(
            "$BACK_LEVER.german_hang",
            "20 s German hang",
            20,
            "Opening the shoulders into the back lever's position.",
        ),
        reps("$BACK_LEVER.skin_the_cat", "5 skin the cats", 5, "Turning through the back lever under control."),
        hold("$BACK_LEVER.tuck", "15 s tuck back lever", 15, "The position, with your knees tucked."),
        hold("$BACK_LEVER.advanced_tuck", "15 s advanced tuck back lever", 15, "Hips open, back flat."),
        hold("$BACK_LEVER.straddle", "10 s straddle back lever", 10, "Legs straight and wide."),
        hold("$BACK_LEVER.full", "5 s back lever", 5, THE_GOAL),
    ),
)

private const val FRONT_LEVER = "front_lever"

/** From a tuck front lever to a 5 second full front lever. */
internal val frontLever = SkillPath(
    id = FRONT_LEVER,
    name = "Front lever",
    level = SkillLevel.ADVANCED,
    weeks = 52,
    about =
        "Hanging face up, body level with the floor, arms straight. Most people take a year or " +
            "more. Strong pull-ups and rows come first.",
    gates = listOf(
        hold("$FRONT_LEVER.tuck", "15 s tuck front lever", 15, "Back level, knees tucked to your chest."),
        hold("$FRONT_LEVER.advanced_tuck", "15 s advanced tuck front lever", 15, "Back flat, hips open."),
        hold("$FRONT_LEVER.one_leg", "10 s one-leg front lever", 10, "One leg out straight."),
        hold("$FRONT_LEVER.straddle", "10 s straddle front lever", 10, "Both legs straight and wide."),
        hold("$FRONT_LEVER.half_lay", "8 s half lay front lever", 8, "Legs together, knees bent."),
        hold("$FRONT_LEVER.full", "5 s front lever", 5, THE_GOAL),
    ),
)

private const val TUCK_PLANCHE = "tuck_planche"

/** From a planche lean to a 15 second tuck planche. */
internal val tuckPlanche = SkillPath(
    id = TUCK_PLANCHE,
    name = "Tuck planche",
    level = SkillLevel.ADVANCED,
    weeks = 26,
    about =
        "Your body held up on straight arms with knees tucked, feet off the floor. The first " +
            "real planche. Warm up your wrists every time.",
    gates = listOf(
        hold("$TUCK_PLANCHE.lean", "30 s planche lean", 30, "Shoulders well past your hands, arms straight."),
        hold("$TUCK_PLANCHE.frog", "30 s frog stand", 30, "Balancing forwards on your hands, knees on your elbows."),
        reps("$TUCK_PLANCHE.pseudo", "10 pseudo-planche push-ups", 10, "Pressing strength in the leaning position."),
        hold("$TUCK_PLANCHE.five", "5 s tuck planche", 5, "Lifting off with straight arms."),
        hold("$TUCK_PLANCHE.fifteen", "15 s tuck planche", 15, THE_GOAL),
    ),
)
