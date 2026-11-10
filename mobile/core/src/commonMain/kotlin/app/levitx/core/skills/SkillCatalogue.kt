package app.levitx.core.skills

/**
 * Built-in skill paths.
 *
 * The gates follow common coaching progressions. They are a starting point and
 * must be reviewed by a qualified coach before release.
 */
public object SkillCatalogue {
    /** Path from first hangs to a strict bar muscle-up. */
    public val muscleUp: SkillPath = SkillPath(
        id = "muscle_up",
        name = "Muscle-up",
        gates = listOf(
            Gate(
                id = "muscle_up.dead_hang",
                title = "30 s dead hang",
                measure = GateMeasure.HOLD_SECONDS,
                target = 30,
                trains = "Grip and shoulder tolerance for everything that follows.",
            ),
            Gate(
                id = "muscle_up.dips",
                title = "10 straight-bar dips",
                measure = GateMeasure.REPS,
                target = 10,
                trains = "Pressing strength for the top half of the muscle-up.",
            ),
            Gate(
                id = "muscle_up.pull_ups",
                title = "10 pull-ups",
                measure = GateMeasure.REPS,
                target = 10,
                trains = "Pulling strength for the bottom half.",
            ),
            Gate(
                id = "muscle_up.chest_to_bar",
                title = "5 chest-to-bar pull-ups",
                measure = GateMeasure.REPS,
                target = 5,
                trains = "Pulling high enough to get over the bar.",
            ),
            Gate(
                id = "muscle_up.false_grip_hang",
                title = "20 s false-grip hang",
                measure = GateMeasure.HOLD_SECONDS,
                target = 20,
                trains = "The wrist position that makes the transition possible.",
            ),
            Gate(
                id = "muscle_up.muscle_up",
                title = "Muscle-up",
                measure = GateMeasure.REPS,
                target = 1,
                trains = "The goal.",
            ),
        ),
    )

    /** Every built-in path. */
    public val all: List<SkillPath> = listOf(muscleUp)
}
