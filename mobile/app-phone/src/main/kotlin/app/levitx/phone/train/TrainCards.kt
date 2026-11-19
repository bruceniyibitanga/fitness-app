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
import app.levitx.phone.ui.PhotoCard
import app.levitx.phone.ui.PhotoTileTitle
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.focusPhoto
import app.levitx.phone.ui.focusPhotoKey
import app.levitx.phone.ui.kitShort
import app.levitx.phone.ui.pathPhoto
import app.levitx.phone.ui.patternName
import app.levitx.phone.ui.programMeta
import app.levitx.phone.ui.programPhotoKey

/*
 * The pieces of the Train tab, sized as on the Train artboard.
 */

private val ProgramCardWidth = 300.dp
private val ProgramCardHeight = 190.dp
private val WorkoutTileHeight = 140.dp
private val WorkoutRowHeight = 68.dp
private val WorkoutThumbSize = 52.dp
private val ExerciseRowHeight = 56.dp

/**
 * A skill program as a big photo card with its name over the photo and, under
 * the name, how far through it the person is or how long it takes. The one
 * they are following is tagged YOUR PLAN. Opening it grows the photo into
 * the program's page.
 */
@Composable
internal fun ProgramCard(program: ProgramSummary, onClick: () -> Unit) {
    val path = program.path
    val caption = if (program.passed > 0 || program.isCurrent) {
        pluralStringResource(R.plurals.train_program_gates, path.gates.size, path.gates.size, program.passed)
    } else {
        programMeta(path)
    }
    Box(
        modifier = Modifier
            .size(ProgramCardWidth, ProgramCardHeight)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        PhotoCard(
            photo = pathPhoto(path.id),
            title = path.name,
            caption = caption,
            sharedKey = programPhotoKey(path.id),
            modifier = Modifier.fillMaxSize(),
        )
        if (program.isCurrent) Tag(stringResource(R.string.train_current_program), Modifier.padding(10.dp))
    }
}

/**
 * A single workout as a photo tile, two to a row: its name, and how long it
 * takes today or that it can't be done today. Opening it grows the photo
 * into the workout's page.
 */
@Composable
internal fun WorkoutTile(workout: WorkoutSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PhotoCard(
        photo = focusPhoto(workout.focus),
        title = focusName(workout.focus),
        caption = workout.session?.let { stringResource(R.string.minutes_short, it.estimatedMinutes) }
            ?: stringResource(R.string.train_not_today),
        titleSize = PhotoTileTitle,
        sharedKey = focusPhotoKey(workout.focus.name),
        modifier = modifier
            .height(WorkoutTileHeight)
            .clickable(role = Role.Button, onClick = onClick),
    )
}

/**
 * A single workout as a row, for search results: a square photo, the name,
 * minutes and moves (or why it can't be done today), and a chevron.
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
private fun Tag(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = LevitxType.Micro,
        color = LevitxColors.Background,
        modifier = modifier.background(LevitxColors.Accent).padding(horizontal = 5.dp, vertical = 2.dp),
    )
}
