package app.levitx.core.skills

/**
 * Built-in skill paths: the programs on the Train tab.
 *
 * The paths themselves live in the files beside this one, grouped by kind.
 * The gates follow common coaching progressions. They are a starting point and
 * must be reviewed by a qualified coach before release.
 */
public object SkillCatalogue {
    /** Path from no pull-ups to a first strict pull-up. */
    public val firstPullUp: SkillPath = app.levitx.core.skills.firstPullUp

    /** Path from first hangs to a strict bar muscle-up. */
    public val muscleUp: SkillPath = app.levitx.core.skills.muscleUp

    /** Every built-in path, roughly easiest goal first. */
    public val all: List<SkillPath> = listOf(
        firstPushUp,
        firstDip,
        firstPullUp,
        twentyPushUps,
        lSit,
        pistolSquat,
        tenPullUps,
        shrimpSquat,
        handstand,
        nordicCurl,
        muscleUp,
        dragonFlag,
        handstandPushUp,
        backLever,
        oneArmPushUp,
        tuckPlanche,
        frontLever,
        oneArmPullUp,
    )

    private val pathsById: Map<String, SkillPath> = all.associateBy { it.id }

    private val gatesById: Map<String, Gate> = all.flatMap { it.gates }.associateBy { it.id }

    /** Finds a path by its [SkillPath.id], or returns null. */
    public fun byId(id: String): SkillPath? = pathsById[id]

    /** Finds a gate on any path by its [Gate.id], or returns null. */
    public fun gateById(id: String): Gate? = gatesById[id]
}
