package app.levitx.phone.ui

import androidx.annotation.DrawableRes
import app.levitx.core.coach.MovementPattern
import app.levitx.core.coach.WorkoutFocus
import app.levitx.phone.R

/*
 * Which bundled photo goes with which program and workout, kept in one place
 * so every screen shows the same picture for the same thing. The photos are
 * from Unsplash and are listed with their sources in docs/PHOTO-CREDITS.md.
 */

private val PathPhotos = mapOf(
    "first_pull_up" to R.drawable.photo_goal_first_pull_up,
    "muscle_up" to R.drawable.photo_goal_muscle_up,
)

/** The photo for a skill program, by its id. A program without its own photo gets the pull-up one. */
@DrawableRes
fun pathPhoto(pathId: String): Int = PathPhotos[pathId] ?: R.drawable.photo_goal_first_pull_up

/** The photo for a single workout's focus. */
@DrawableRes
fun focusPhoto(focus: WorkoutFocus): Int = when (focus) {
    WorkoutFocus.FULL_BODY -> R.drawable.photo_focus_full_body
    WorkoutFocus.UPPER -> R.drawable.photo_focus_upper
    WorkoutFocus.PULL -> R.drawable.photo_goal_first_pull_up
    WorkoutFocus.PUSH -> R.drawable.photo_focus_push
    WorkoutFocus.LEGS -> R.drawable.photo_focus_legs
    WorkoutFocus.CORE_ONLY -> R.drawable.photo_focus_core
}

/** The photo for an exercise, by the movement it trains. */
@DrawableRes
fun patternPhoto(pattern: MovementPattern): Int = when (pattern) {
    MovementPattern.VERTICAL_PULL, MovementPattern.HORIZONTAL_PULL -> R.drawable.photo_goal_first_pull_up
    MovementPattern.HORIZONTAL_PUSH, MovementPattern.VERTICAL_PUSH -> R.drawable.photo_focus_push
    MovementPattern.SQUAT, MovementPattern.HINGE -> R.drawable.photo_focus_legs
    MovementPattern.CORE -> R.drawable.photo_focus_core
}
