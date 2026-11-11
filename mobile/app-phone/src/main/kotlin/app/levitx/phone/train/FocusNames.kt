package app.levitx.phone.train

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.levitx.core.coach.WorkoutFocus
import app.levitx.phone.R

/** What the apps call a workout focus. */
@Composable
fun focusName(focus: WorkoutFocus): String = stringResource(
    when (focus) {
        WorkoutFocus.FULL_BODY -> R.string.focus_full_body
        WorkoutFocus.UPPER -> R.string.focus_upper
        WorkoutFocus.PULL -> R.string.focus_pull
        WorkoutFocus.PUSH -> R.string.focus_push
        WorkoutFocus.LEGS -> R.string.focus_legs
        WorkoutFocus.CORE_ONLY -> R.string.focus_core
    },
)
