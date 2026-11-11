package app.levitx.phone.train

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.focusPhoto
import app.levitx.phone.ui.pathPhoto

/*
 * The pieces of the Train tab, sized as on the Train artboard.
 */

private val ChipHeight = 40.dp
private val ProgramCardWidth = 220.dp
private val ProgramPhotoHeight = 112.dp
private val WorkoutRowHeight = 68.dp
private val WorkoutThumbSize = 52.dp

/** A body-focus chip: cream when chosen, dark with an outline when not. */
@Composable
internal fun FocusChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .height(ChipHeight)
            .background(if (selected) LevitxColors.Text else LevitxColors.Surface)
            .border(1.dp, if (selected) Color.Transparent else LevitxColors.Tick)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) LevitxColors.Background else LevitxColors.Text,
        )
    }
}

/**
 * A skill program as a card: its photo with a tag in the corner, then its
 * name and how far through it the person is. The tag says YOUR PLAN on the
 * program they are following.
 */
@Composable
internal fun ProgramCard(program: ProgramSummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(ProgramCardWidth)
            .background(LevitxColors.Surface)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(ProgramPhotoHeight)) {
            Image(
                painter = painterResource(pathPhoto(program.path.id)),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                text = stringResource(
                    if (program.isCurrent) R.string.train_current_program else R.string.train_tag_skill,
                ),
                style = LevitxType.Micro,
                color = LevitxColors.Background,
                modifier = Modifier
                    .padding(8.dp)
                    .background(LevitxColors.Accent)
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp),
        ) {
            Text(program.path.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = LevitxColors.Text)
            Text(
                text = pluralStringResource(
                    R.plurals.train_program_gates,
                    program.path.gates.size,
                    program.path.gates.size,
                    program.passed,
                ),
                style = LevitxType.Micro,
                color = LevitxColors.TextMuted,
            )
        }
    }
}

/**
 * A single workout as a row: a square photo, the name, minutes and moves (or
 * why it can't be done today), and a chevron.
 */
@Composable
internal fun WorkoutRow(workout: WorkoutSummary, onClick: () -> Unit) {
    val session = workout.session
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = WorkoutRowHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = ScreenPadding, vertical = 8.dp),
    ) {
        Image(
            painter = painterResource(focusPhoto(workout.focus)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(WorkoutThumbSize),
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
            Text(focusName(workout.focus), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = LevitxColors.Text)
            val meta = if (session == null) {
                stringResource(R.string.train_nothing_safe)
            } else {
                val parts = listOfNotNull(
                    pluralStringResource(
                        R.plurals.train_workout_meta,
                        session.plans.size,
                        session.estimatedMinutes,
                        session.plans.size,
                    ),
                    stringResource(R.string.train_some_light).takeIf { session.plans.any { !it.isHard } },
                )
                parts.joinToString(" · ")
            }
            Text(meta, style = LevitxType.Micro, color = LevitxColors.TextMuted)
        }
        Icon(painterResource(R.drawable.ic_chevron_forward), contentDescription = null, tint = LevitxColors.TextFaint)
    }
}
