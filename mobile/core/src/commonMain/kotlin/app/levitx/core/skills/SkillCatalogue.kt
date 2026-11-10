package app.levitx.core.skills

import app.levitx.core.load.LoadCatalogue

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
                loadProfile = LoadCatalogue.dip,
            ),
            Gate(
                id = "muscle_up.pull_ups",
                title = "10 pull-ups",
                measure = GateMeasure.REPS,
                target = 10,
                trains = "Pulling strength for the bottom half.",
                loadProfile = LoadCatalogue.pullUp,
            ),
            Gate(
                id = "muscle_up.chest_to_bar",
                title = "5 chest-to-bar pull-ups",
                measure = GateMeasure.REPS,
                target = 5,
                trains = "Pulling high enough to get over the bar.",
                loadProfile = LoadCatalogue.pullUp,
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

    /** Path from no pull-ups to a first strict pull-up. */
    public val firstPullUp: SkillPath = SkillPath(
        id = "first_pull_up",
        name = "First pull-up",
        gates = listOf(
            Gate(
                id = "first_pull_up.dead_hang",
                title = "30 s dead hang",
                measure = GateMeasure.HOLD_SECONDS,
                target = 30,
                trains = "Grip strength and comfortable shoulders under load.",
            ),
            Gate(
                id = "first_pull_up.scapular_pulls",
                title = "10 scapular pull-ups",
                measure = GateMeasure.REPS,
                target = 10,
                trains = "Starting the pull with your shoulder blades, not your arms.",
            ),
            Gate(
                id = "first_pull_up.flexed_arm_hang",
                title = "15 s chin-over-bar hold",
                measure = GateMeasure.HOLD_SECONDS,
                target = 15,
                trains = "Strength at the top, where most first attempts stall.",
            ),
            Gate(
                id = "first_pull_up.negatives",
                title = "5 slow negatives",
                measure = GateMeasure.REPS,
                target = 5,
                trains = "Lowering under control for 3 to 5 seconds builds the pulling strength.",
                loadProfile = LoadCatalogue.pullUp,
            ),
            Gate(
                id = "first_pull_up.pull_up",
                title = "Pull-up",
                measure = GateMeasure.REPS,
                target = 1,
                trains = "The goal.",
                loadProfile = LoadCatalogue.pullUp,
            ),
        ),
    )

    /** Every built-in path, easiest goal first. */
    public val all: List<SkillPath> = listOf(firstPullUp, muscleUp)

    private val pathsById: Map<String, SkillPath> = all.associateBy { it.id }

    /** Finds a path by its [SkillPath.id], or returns null. */
    public fun byId(id: String): SkillPath? = pathsById[id]
}
