package app.levitx.core.skills

import app.levitx.core.load.LoadCatalogue

/*
 * Programs for the big strength skills: the muscle-up and the one-arm
 * push-up and pull-up. Like the rest of the catalogue, they need a qualified
 * coach's review before release.
 */

private const val MUSCLE_UP = "muscle_up"

/** From first hangs to a strict bar muscle-up. */
internal val muscleUp = SkillPath(
    id = MUSCLE_UP,
    name = "Muscle-up",
    level = SkillLevel.ADVANCED,
    weeks = 20,
    about =
        "Pulling over the bar and pressing to the top in one movement. Strong pull-ups and " +
            "dips come first.",
    gates = listOf(
        hold("$MUSCLE_UP.dead_hang", "30 s dead hang", 30, "Grip and shoulder tolerance for everything that follows."),
        reps(
            "$MUSCLE_UP.dips",
            "10 straight-bar dips",
            10,
            "Pressing strength for the top half of the muscle-up.",
            LoadCatalogue.dip,
        ),
        reps("$MUSCLE_UP.pull_ups", "10 pull-ups", 10, "Pulling strength for the bottom half.", LoadCatalogue.pullUp),
        reps(
            "$MUSCLE_UP.chest_to_bar",
            "5 chest-to-bar pull-ups",
            5,
            "Pulling high enough to get over the bar.",
            LoadCatalogue.pullUp,
        ),
        hold(
            "$MUSCLE_UP.false_grip_hang",
            "20 s false-grip hang",
            20,
            "The wrist position that makes the transition possible.",
        ),
        reps("$MUSCLE_UP.muscle_up", "Muscle-up", 1, THE_GOAL),
    ),
)

private const val ONE_ARM_PUSH_UP = "one_arm_push_up"

/** From twenty push-ups to three one-arm push-ups each side. */
internal val oneArmPushUp = SkillPath(
    id = ONE_ARM_PUSH_UP,
    name = "One-arm push-up",
    level = SkillLevel.ADVANCED,
    weeks = 26,
    about =
        "All your pressing on one arm, feet wide. Hard on the core and the obliques as well as " +
            "the chest.",
    gates = listOf(
        reps("$ONE_ARM_PUSH_UP.twenty", "20 push-ups", 20, "A big base of two-arm strength.", LoadCatalogue.pushUp),
        reps("$ONE_ARM_PUSH_UP.archer", "8 archer push-ups each side", 8, "Most of your weight on one arm."),
        reps("$ONE_ARM_PUSH_UP.negatives", "5 one-arm negatives each side", 5, "Lowering on one arm without twisting."),
        reps("$ONE_ARM_PUSH_UP.one", "One-arm push-up each side", 1, "Pressing back up on one arm."),
        reps("$ONE_ARM_PUSH_UP.three", "3 one-arm push-ups each side", 3, THE_GOAL),
    ),
)

private const val ONE_ARM_PULL_UP = "one_arm_pull_up"

/** From fifteen pull-ups to a one-arm pull-up each side. */
internal val oneArmPullUp = SkillPath(
    id = ONE_ARM_PULL_UP,
    name = "One-arm pull-up",
    level = SkillLevel.ADVANCED,
    weeks = 78,
    about =
        "Your whole body pulled up by one arm. Expect well over a year, and stop at any elbow " +
            "pain: the tendons set the pace.",
    gates = listOf(
        reps("$ONE_ARM_PULL_UP.fifteen", "15 pull-ups", 15, "A big base of two-arm pulling.", LoadCatalogue.pullUp),
        reps("$ONE_ARM_PULL_UP.archer", "8 archer pull-ups each side", 8, "Most of your weight on one arm."),
        hold(
            "$ONE_ARM_PULL_UP.one_arm_hang",
            "20 s one-arm hang each side",
            20,
            "Grip and shoulder control on one arm.",
        ),
        reps(
            "$ONE_ARM_PULL_UP.negatives",
            "3 one-arm negatives each side",
            3,
            "Lowering on one arm as slowly as you can.",
        ),
        reps("$ONE_ARM_PULL_UP.one", "One-arm pull-up each side", 1, THE_GOAL),
    ),
)
