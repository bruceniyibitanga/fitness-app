package app.levitx.phone.workout

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding

/** How much the scrim dims the set behind the menu. */
private const val SCRIM_ALPHA = 0.72f

/**
 * The session menu, over the set: skip the exercise, sounds, leave for now,
 * or end and save. A sheet from the bottom, so it is in reach of a thumb.
 */
@Composable
internal fun BoxScope.SessionMenu(state: WorkoutUiState.Active, actions: WorkoutActions, onClose: () -> Unit) {
    val closeLabel = stringResource(R.string.workout_menu_back)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevitxColors.Background.copy(alpha = SCRIM_ALPHA))
            .clickable(interactionSource = null, indication = null, onClickLabel = closeLabel, onClick = onClose),
    )
    val title = stringResource(R.string.workout_menu_title)
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(LevitxColors.Surface)
            .semantics {
                paneTitle = title
                isTraversalGroup = true
            },
    ) {
        Rule()
        MenuHeader(state, title)
        val nothingDone = state.run.logged[state.run.exerciseIndex].isEmpty()
        MenuRow(
            icon = R.drawable.ic_skip,
            title = stringResource(if (nothingDone) R.string.workout_menu_skip else R.string.workout_menu_next),
            body = stringResource(
                if (nothingDone) R.string.workout_menu_skip_body else R.string.workout_menu_next_body,
            ),
            onClick = {
                actions.onSkipExercise()
                onClose()
            },
        )
        MenuRow(
            icon = if (state.soundsOn) R.drawable.ic_sound else R.drawable.ic_sound_off,
            title = stringResource(R.string.workout_menu_sounds),
            body = stringResource(
                if (state.soundsOn) R.string.workout_menu_sounds_on else R.string.workout_menu_sounds_off,
            ),
            onClick = actions.onToggleSounds,
        )
        MenuRow(
            icon = R.drawable.ic_leave,
            title = stringResource(R.string.workout_menu_leave),
            body = stringResource(R.string.workout_menu_leave_body),
            onClick = actions.onDone,
        )
        MenuButtons(state, actions, onClose)
    }
}

/** The exercise and set, and the question. */
@Composable
private fun MenuHeader(state: WorkoutUiState.Active, title: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
    ) {
        Box(
            Modifier
                .width(36.dp)
                .height(4.dp)
                .background(LevitxColors.Outline),
        )
    }
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 10.dp, bottom = 14.dp)) {
        Caption(
            stringResource(
                R.string.workout_menu_where,
                state.plan.exercise.name,
                state.run.nextSetIndex + 1,
                state.plan.sets.size,
            ),
        )
        Text(
            text = title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = LevitxColors.Text,
            modifier = Modifier
                .padding(top = 4.dp)
                .semantics { heading() },
        )
    }
    Rule()
}

/** One choice: a square icon, a title and a line on what it does. */
@Composable
private fun MenuRow(@DrawableRes icon: Int, title: String, body: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = ScreenPadding, vertical = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .border(1.dp, LevitxColors.Text),
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = LevitxColors.Text)
        }
        Column(
            modifier = Modifier
                .padding(start = 14.dp)
                .weight(1f),
        ) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = LevitxColors.Text)
            Text(text = body, fontSize = 13.sp, lineHeight = 18.sp, color = LevitxColors.TextMuted)
        }
        Icon(
            painterResource(R.drawable.ic_chevron_forward),
            contentDescription = null,
            tint = LevitxColors.TextFaint,
        )
    }
    Rule()
}

/** Back to the set, and end the workout, saving what was done. */
@Composable
private fun MenuButtons(state: WorkoutUiState.Active, actions: WorkoutActions, onClose: () -> Unit) {
    val sets = state.run.logged.sumOf { it.size }
    Row(modifier = Modifier.height(BottomBarHeight)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .background(LevitxColors.Background)
                .clickable(role = Role.Button, onClick = onClose),
        ) {
            Text(stringResource(R.string.workout_menu_back), fontSize = 15.sp, color = LevitxColors.Text)
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .border(1.dp, LevitxColors.Accent)
                .clickable(role = Role.Button, onClick = actions.onFinish),
        ) {
            Text(
                text = if (sets == 0) {
                    stringResource(R.string.workout_menu_end_empty)
                } else {
                    pluralStringResource(R.plurals.workout_menu_end, sets, sets)
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = LevitxColors.Accent,
            )
        }
    }
}
