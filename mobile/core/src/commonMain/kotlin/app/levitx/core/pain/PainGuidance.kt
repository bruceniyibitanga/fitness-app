package app.levitx.core.pain

import app.levitx.core.requireInRange

/** Lowest pain rating: no pain. */
public const val MIN_PAIN_RATING: Int = 0

/** Highest pain rating: the worst pain imaginable. */
public const val MAX_PAIN_RATING: Int = 10

/** Highest pain during exercise the guide accepts. */
public const val MAX_ACCEPTABLE_PAIN_DURING_EXERCISE: Int = 5

/**
 * Warning signs that mean training advice should stop and a professional
 * should take over.
 *
 * This list is a starting point and must be reviewed by a physiotherapist
 * before release.
 */
public enum class RedFlag {
    /** Pain that wakes the person at night or is there at rest. */
    NIGHT_OR_REST_PAIN,

    /** Numbness, tingling or pins and needles. */
    NUMBNESS_OR_TINGLING,

    /** Visible swelling, bruising or a joint that looks wrong. */
    SWELLING_OR_DEFORMITY,

    /** Pain that started with a fall, pop or impact. */
    SUDDEN_INJURY,

    /** Weakness that is getting worse. */
    WORSENING_WEAKNESS,

    /** Fever or unexplained weight loss alongside the pain. */
    FEVER_OR_WEIGHT_LOSS,
}

/** What the apps suggest after a pain check-in. */
public enum class PainAdvice {
    /** Pain is inside the guide. Train as planned. */
    CONTINUE_AS_PLANNED,

    /** Ease off: swap to a lighter variation or reduce sets. */
    EASE_OFF,

    /** Stop giving training advice for this area and see a professional. */
    SEE_PROFESSIONAL,
}

/** Why the advice was given, so the UI can explain it in plain words. */
public enum class PainReason {
    /** One or more [RedFlag]s were reported. */
    RED_FLAG,

    /** Pain during exercise went above [MAX_ACCEPTABLE_PAIN_DURING_EXERCISE]. */
    TOO_HIGH_DURING_EXERCISE,

    /** Pain the next morning was worse than the morning before. */
    WORSE_NEXT_MORNING,

    /** Everything was inside the guide. */
    WITHIN_GUIDE,
}

/**
 * A pain check-in for one body area. Ratings run from [MIN_PAIN_RATING] (none)
 * to [MAX_PAIN_RATING] (worst). Morning ratings are optional because they
 * arrive later than the session.
 *
 * @property duringExercise Highest pain during the session.
 * @property morningBefore Pain or stiffness on the morning of the session, before training.
 * @property morningAfter Pain or stiffness on the morning after the session.
 * @property redFlags Warning signs the person reported.
 * @throws IllegalArgumentException if a rating is outside the scale.
 */
public data class PainCheckIn(
    val duringExercise: Int,
    val morningBefore: Int? = null,
    val morningAfter: Int? = null,
    val redFlags: Set<RedFlag> = emptySet(),
) {
    init {
        requireInRange(duringExercise, MIN_PAIN_RATING, MAX_PAIN_RATING, "duringExercise")
        morningBefore?.let { requireInRange(it, MIN_PAIN_RATING, MAX_PAIN_RATING, "morningBefore") }
        morningAfter?.let { requireInRange(it, MIN_PAIN_RATING, MAX_PAIN_RATING, "morningAfter") }
    }
}

/**
 * The result of [assessPain].
 *
 * @property advice What to do next.
 * @property reason Why.
 */
public data class PainAssessment(val advice: PainAdvice, val reason: PainReason)

/**
 * Turns a check-in into advice, following the pain-monitoring model
 * (Silbernagel et al., 2007): pain during exercise may reach 5 out of 10,
 * should settle by the next morning, and should not build up over time.
 *
 * This is a training guide, not a diagnosis. Red flags always win.
 */
public fun assessPain(checkIn: PainCheckIn): PainAssessment {
    val before = checkIn.morningBefore
    val after = checkIn.morningAfter
    return when {
        checkIn.redFlags.isNotEmpty() ->
            PainAssessment(PainAdvice.SEE_PROFESSIONAL, PainReason.RED_FLAG)
        checkIn.duringExercise > MAX_ACCEPTABLE_PAIN_DURING_EXERCISE ->
            PainAssessment(PainAdvice.EASE_OFF, PainReason.TOO_HIGH_DURING_EXERCISE)
        before != null && after != null && after > before ->
            PainAssessment(PainAdvice.EASE_OFF, PainReason.WORSE_NEXT_MORNING)
        else ->
            PainAssessment(PainAdvice.CONTINUE_AS_PLANNED, PainReason.WITHIN_GUIDE)
    }
}
