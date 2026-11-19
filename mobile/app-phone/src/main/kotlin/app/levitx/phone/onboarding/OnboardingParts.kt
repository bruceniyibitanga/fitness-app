package app.levitx.phone.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.CheckBadge
import app.levitx.phone.ui.Photo
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.ScreenTitle
import app.levitx.phone.ui.pathPhoto

/*
 * The pieces every setup step is built from: the photo behind its top, its
 * title, a tappable answer, and a labelled group of controls.
 */

/** How far above the photo's foot a step's title starts, so it sits over the fade. */
private val TitleOverPhoto = 180.dp

/** The photo on most steps, as on the onboarding artboards (330 of 844). */
private val StepPhotoHeight = 330.dp

/** A shorter photo, for a step with more to fit under it. */
private val ShortPhotoHeight = 250.dp

/** A taller photo, for the plan at the end. */
private val TallPhotoHeight = 380.dp

/** How dark the photo is behind the back arrow and the progress line. */
private const val PHOTO_TOP_SHADE = 0.45f

/** Where the photo starts fading into the background, as a fraction of its height. */
private const val PHOTO_FADE_START = 0.3f

private val OptionHeight = 68.dp
private val SelectedOutline = 2.dp

/**
 * The photo behind the top of a step.
 *
 * @property photo The picture.
 * @property height How far down the screen it reaches.
 */
internal data class StepArt(@param:DrawableRes val photo: Int, val height: Dp = StepPhotoHeight) {
    /** The gap between the header and the step's title, which puts the title over the photo's fade. */
    val titleGap: Dp get() = height - TitleOverPhoto
}

/**
 * The photo for [step], or null for the steps that are all controls: the
 * goal list and the quick test. Plan style and the finished plan show the
 * photo of the goal that was picked.
 */
internal fun stepArt(step: OnboardingStep, goalPathId: String?): StepArt? {
    val goalPhoto = goalPathId?.let(::pathPhoto) ?: R.drawable.photo_focus_full_body
    return when (step) {
        OnboardingStep.STYLE -> StepArt(goalPhoto)
        OnboardingStep.AIM -> StepArt(R.drawable.photo_focus_full_body)
        OnboardingStep.PULL_UP -> StepArt(R.drawable.photo_goal_first_pull_up)
        OnboardingStep.BODY -> StepArt(R.drawable.photo_focus_upper)
        OnboardingStep.WEEK -> StepArt(R.drawable.photo_focus_legs)
        OnboardingStep.KIT -> StepArt(R.drawable.photo_goal_first_dip, ShortPhotoHeight)
        OnboardingStep.READY -> StepArt(goalPhoto, TallPhotoHeight)
        OnboardingStep.WELCOME, OnboardingStep.GOAL, OnboardingStep.TEST -> null
    }
}

/** [art] across the top of a step, shaded at the top for the header and fading into the background at its foot. */
@Composable
internal fun StepPhoto(art: StepArt, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(art.height)) {
        Photo(art.photo, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to LevitxColors.Background.copy(alpha = PHOTO_TOP_SHADE),
                        PHOTO_FADE_START to LevitxColors.Background.copy(alpha = 0f),
                        1f to LevitxColors.Background,
                    ),
                ),
        )
    }
}

/** A step's title: the one thing the step asks. */
@Composable
internal fun StepTitle(title: String, modifier: Modifier = Modifier) {
    ScreenTitle(title, modifier = modifier.padding(horizontal = ScreenPadding))
}

/** The small print under a step's controls. [warning] turns it orange, for something that needs fixing. */
@Composable
internal fun StepNote(text: String, modifier: Modifier = Modifier, warning: Boolean = false) {
    Text(
        text = text,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = if (warning) LevitxColors.Accent else LevitxColors.TextMuted,
        modifier = modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 14.dp),
    )
}

/** A step's answers, stacked under its title and read out as one group. */
@Composable
internal fun Options(content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ScreenPadding, end = ScreenPadding, top = 22.dp)
            .selectableGroup(),
        content = content,
    )
}

/**
 * One answer to a step's question: a tall row with a [title], an optional
 * [caption] under it, and an orange outline and tick when it is [selected].
 */
@Composable
internal fun OptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = OptionHeight)
            .background(LevitxColors.Surface)
            .then(if (selected) Modifier.border(SelectedOutline, LevitxColors.Accent) else Modifier)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = LevitxColors.Text)
            if (caption != null) {
                Text(text = caption, fontSize = 13.sp, lineHeight = 18.sp, color = LevitxColors.TextMuted)
            }
        }
        if (selected) CheckBadge()
    }
}

/** A labelled group of controls on a step that has more than one, like the days and the minutes. */
@Composable
internal fun Question(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = ScreenPadding, end = ScreenPadding, top = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = LevitxColors.Text,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            if (trailing != null) Caption(trailing, color = LevitxColors.Accent)
        }
        content()
    }
}
