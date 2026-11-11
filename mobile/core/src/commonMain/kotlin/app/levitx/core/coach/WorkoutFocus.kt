package app.levitx.core.coach

import app.levitx.core.coach.MovementPattern.CORE
import app.levitx.core.coach.MovementPattern.HINGE
import app.levitx.core.coach.MovementPattern.HORIZONTAL_PULL
import app.levitx.core.coach.MovementPattern.HORIZONTAL_PUSH
import app.levitx.core.coach.MovementPattern.SQUAT
import app.levitx.core.coach.MovementPattern.VERTICAL_PULL
import app.levitx.core.coach.MovementPattern.VERTICAL_PUSH

/**
 * A workout the person picks themselves from the Train tab, by the part of
 * the body it works.
 *
 * It is built from the same exercises, targets and safety rules as the
 * coach's own sessions, so picking one never means training blind: it uses
 * the person's current rung on each ladder, and anything they trained too
 * recently is eased.
 *
 * @property patterns The movements it trains, in order. Order is priority:
 *   when time is short, the end is cut.
 * @property everyOption True when it trains every exercise on the person's
 *   rung of each movement, not only the usual one. Used where one exercise per
 *   movement would make too short a workout.
 */
public enum class WorkoutFocus(
    internal val patterns: List<MovementPattern>,
    internal val everyOption: Boolean = false,
) {
    /** Every movement, like the coach's full-body sessions. */
    FULL_BODY(listOf(VERTICAL_PULL, HORIZONTAL_PUSH, SQUAT, HORIZONTAL_PULL, VERTICAL_PUSH, HINGE, CORE)),

    /** Pulling and pushing, then core. */
    UPPER(listOf(VERTICAL_PULL, HORIZONTAL_PUSH, HORIZONTAL_PULL, VERTICAL_PUSH, CORE)),

    /** Back and biceps: both pulls, then core. */
    PULL(listOf(VERTICAL_PULL, HORIZONTAL_PULL, CORE)),

    /** Chest, shoulders and triceps: both pushes, then core. */
    PUSH(listOf(HORIZONTAL_PUSH, VERTICAL_PUSH, CORE)),

    /** Squat and hinge, then core. */
    LEGS(listOf(SQUAT, HINGE, CORE)),

    /** Core only, with every exercise on the person's core rung so there is enough to do. */
    CORE_ONLY(listOf(CORE), everyOption = true),
}
