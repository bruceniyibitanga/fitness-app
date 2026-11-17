package app.levitx.phone.train

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.coach.Exercise
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.focusPhoto
import app.levitx.phone.ui.kitShort
import app.levitx.phone.ui.pathPhoto
import app.levitx.phone.ui.patternName
import app.levitx.phone.ui.programMeta

/*
 * The pieces of the Train tab, sized as on the Train artboard.
 */

private val ProgramCardWidth = 220.dp
private val ProgramPhotoHeight = 112.dp
private val WorkoutRowHeight = 68.dp
private val WorkoutThumbSize = 52.dp
private val ExerciseRowHeight = 56.dp

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
                text = if (program.passed > 0 || program.isCurrent) {
                    pluralStringResource(
                        R.plurals.train_program_gates,
                        program.path.gates.size,
                        program.path.gates.size,
                        program.passed,
                    )
                } else {
                    programMeta(program.path)
                },
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
    PhotoListRow(focusPhoto(workout.focus), focusName(workout.focus), meta, onClick)
}

/**
 * A row of the Train list: a square [photo], a [title] over its [meta] line,
 * and a chevron. A [tag], like YOUR PLAN, sits after the title.
 */
@Composable
internal fun PhotoListRow(
    @DrawableRes photo: Int,
    title: String,
    meta: String,
    onClick: () -> Unit,
    tag: String? = null,
) {
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
            painter = painterResource(photo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(WorkoutThumbSize),
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = LevitxColors.Text)
                if (tag != null) Tag(tag)
            }
            Text(meta, style = LevitxType.Micro, color = LevitxColors.TextMuted)
        }
        Icon(painterResource(R.drawable.ic_chevron_forward), contentDescription = null, tint = LevitxColors.TextFaint)
    }
}

/** A program as a list row, tagged YOUR PLAN when it's the person's goal. */
@Composable
internal fun ProgramRow(program: ProgramSummary, onClick: () -> Unit) {
    PhotoListRow(
        photo = pathPhoto(program.path.id),
        title = program.path.name,
        meta = programMeta(program.path),
        onClick = onClick,
        tag = if (program.isCurrent) stringResource(R.string.train_current_program) else null,
    )
}

/** An exercise as a text row: its name, then its movement and kit. */
@Composable
internal fun ExerciseRow(exercise: Exercise, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ExerciseRowHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = ScreenPadding, vertical = 8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
            Text(exercise.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = LevitxColors.Text)
            Text(
                text = listOf(
                    patternName(exercise.pattern).uppercase(),
                    kitShort(exercise.equipment),
                ).joinToString(" · "),
                style = LevitxType.Micro,
                color = LevitxColors.TextMuted,
            )
        }
        Icon(painterResource(R.drawable.ic_chevron_forward), contentDescription = null, tint = LevitxColors.TextFaint)
    }
}

/** A small orange label, like YOUR PLAN. */
@Composable
private fun Tag(text: String) {
    Text(
        text = text,
        style = LevitxType.Micro,
        color = LevitxColors.Background,
        modifier = Modifier.background(LevitxColors.Accent).padding(horizontal = 5.dp, vertical = 2.dp),
    )
}
