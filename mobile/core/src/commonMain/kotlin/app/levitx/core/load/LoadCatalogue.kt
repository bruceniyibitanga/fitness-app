package app.levitx.core.load

private const val EBBEN_2011 =
    "Ebben et al. (2011), Kinetic analysis of several variations of push-ups, " +
        "J Strength Cond Res 25(10):2891-2894"

/**
 * Share of bodyweight moved when the hands and forearms stay put, as on a bar
 * or dip bars. Forearm and hand together are about 2.2% of body mass per arm
 * in men and 1.9% in women (de Leva 1996, J Biomech 29(9):1223-1230), so
 * about 4 to 4.5% for both arms. Rounded down to 95%.
 */
private const val BODY_WITHOUT_FOREARMS = 0.95

/**
 * The load profiles the apps know about.
 *
 * Push-up figures are force-plate measurements. Pull-ups and dips count
 * everything but the forearms and hands as moved, worked out from published
 * segment masses. That is an estimate until someone measures it.
 * Add new profiles here rather than hard-coding shares elsewhere.
 */
public object LoadCatalogue {
    /** Push-up with hands on a 61 cm box (easiest incline). */
    public val pushUpHandsOn61cmBox: LoadProfile = measuredPushUp(
        id = "push_up.hands_61cm",
        name = "Push-up, hands on 61 cm box",
        share = 0.41,
    )

    /** Push-up from the knees. */
    public val pushUpKnees: LoadProfile = measuredPushUp(
        id = "push_up.knees",
        name = "Knee push-up",
        share = 0.49,
    )

    /** Push-up with hands on a 30 cm box. */
    public val pushUpHandsOn30cmBox: LoadProfile = measuredPushUp(
        id = "push_up.hands_30cm",
        name = "Push-up, hands on 30 cm box",
        share = 0.55,
    )

    /** Regular push-up on the floor. */
    public val pushUp: LoadProfile = measuredPushUp(
        id = "push_up",
        name = "Push-up",
        share = 0.64,
    )

    /** Push-up with feet on a 30 cm box. */
    public val pushUpFeetOn30cmBox: LoadProfile = measuredPushUp(
        id = "push_up.feet_30cm",
        name = "Push-up, feet on 30 cm box",
        share = 0.70,
    )

    /** Push-up with feet on a 61 cm box (hardest decline measured). */
    public val pushUpFeetOn61cmBox: LoadProfile = measuredPushUp(
        id = "push_up.feet_61cm",
        name = "Push-up, feet on 61 cm box",
        share = 0.74,
    )

    /** Pull-up. Counts everything but the forearms and hands as moved (estimate). */
    public val pullUp: LoadProfile = LoadProfile(
        id = "pull_up",
        name = "Pull-up",
        bodyweightShare = BODY_WITHOUT_FOREARMS,
        evidence = LoadEvidence.ESTIMATED,
        addedWeightShare = 1.0,
    )

    /** Parallel bar dip. Counts everything but the forearms and hands as moved (estimate). */
    public val dip: LoadProfile = LoadProfile(
        id = "dip",
        name = "Dip",
        bodyweightShare = BODY_WITHOUT_FOREARMS,
        evidence = LoadEvidence.ESTIMATED,
        addedWeightShare = 1.0,
    )

    /** Push-up variations from easiest to hardest. */
    public val pushUps: List<LoadProfile> = listOf(
        pushUpHandsOn61cmBox,
        pushUpKnees,
        pushUpHandsOn30cmBox,
        pushUp,
        pushUpFeetOn30cmBox,
        pushUpFeetOn61cmBox,
    )

    /** Every profile in the catalogue. */
    public val all: List<LoadProfile> = pushUps + listOf(pullUp, dip)

    private val profilesById: Map<String, LoadProfile> = all.associateBy { it.id }

    /** Finds a profile by its [LoadProfile.id], or returns null. */
    public fun byId(id: String): LoadProfile? = profilesById[id]

    private fun measuredPushUp(id: String, name: String, share: Double): LoadProfile = LoadProfile(
        id = id,
        name = name,
        bodyweightShare = share,
        evidence = LoadEvidence.MEASURED,
        source = EBBEN_2011,
    )
}
