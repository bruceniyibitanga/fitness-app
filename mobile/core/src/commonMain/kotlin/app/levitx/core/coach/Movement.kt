package app.levitx.core.coach

/** A part of the body a pain check-in can be about. */
public enum class BodyArea {
    /** Wrist and hand. */
    WRIST,

    /** Elbow and forearm. */
    ELBOW,

    /** Shoulder. */
    SHOULDER,

    /** Lower back. */
    LOWER_BACK,

    /** Hip. */
    HIP,

    /** Knee. */
    KNEE,

    /** Ankle and foot. */
    ANKLE,
}

/**
 * The basic movements the coach balances a plan across.
 *
 * Capability, weekly volume and recovery are all tracked per pattern, because
 * two exercises of the same pattern tire and train the same things.
 *
 * @property loads The body areas this pattern puts under load. A sore area
 *   eases off or stops every pattern that loads it.
 */
public enum class MovementPattern(public val loads: Set<BodyArea>) {
    /** Pulling yourself up: hangs, pull-ups. */
    VERTICAL_PULL(setOf(BodyArea.ELBOW, BodyArea.SHOULDER)),

    /** Pulling toward your chest: rows. */
    HORIZONTAL_PULL(setOf(BodyArea.ELBOW, BodyArea.SHOULDER)),

    /** Pressing overhead or down: pike push-ups, dips. */
    VERTICAL_PUSH(setOf(BodyArea.WRIST, BodyArea.ELBOW, BodyArea.SHOULDER)),

    /** Pressing away from your chest: push-ups. */
    HORIZONTAL_PUSH(setOf(BodyArea.WRIST, BodyArea.ELBOW, BodyArea.SHOULDER)),

    /** Bending the knees under load: squats, lunges. */
    SQUAT(setOf(BodyArea.KNEE, BodyArea.HIP, BodyArea.ANKLE)),

    /** Bending at the hips: bridges, single-leg deadlifts. */
    HINGE(setOf(BodyArea.LOWER_BACK, BodyArea.HIP)),

    /** Holding the trunk still or curling it: planks, hollow holds, leg raises. */
    CORE(setOf(BodyArea.LOWER_BACK)),
}

/**
 * Kit an exercise can need. A floor and something stable to step or lean on
 * (a chair, a step, a counter) are assumed, so they aren't listed.
 */
public enum class Equipment {
    /** A bar high enough to hang from. */
    PULL_UP_BAR,

    /** A bar around waist height, for rows. */
    LOW_BAR,

    /** Parallel bars for dips. */
    DIP_BARS,

    /** Gymnastic rings. */
    RINGS,

    /** A resistance band. */
    BAND,
}
