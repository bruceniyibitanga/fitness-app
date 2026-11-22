package app.levitx.phone.pain

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.pain.MAX_ACCEPTABLE_PAIN_DURING_EXERCISE
import app.levitx.core.pain.MAX_PAIN_RATING
import app.levitx.core.pain.MIN_PAIN_RATING
import app.levitx.core.pain.PainZone
import app.levitx.core.pain.painZone
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.ScreenTitle
import app.levitx.phone.ui.areaName

private val CellHeight = 52.dp
private val LabelWidth = 48.dp

/** The ratings the guide accepts, and the ones over it: the two rows of the scale. */
private val WithinGuide = MIN_PAIN_RATING..MAX_ACCEPTABLE_PAIN_DURING_EXERCISE
private val OverGuide = MAX_ACCEPTABLE_PAIN_DURING_EXERCISE + 1..MAX_PAIN_RATING

/**
 * Step three, the "Pain check-in" artboard: pain at its worst as one big
 * number, picked from two rows of squares. The top row is everything the
 * guide accepts and the bottom row is everything over it, so where a number
 * sits already says what it means. Nothing is picked to begin with, so a
 * rating is never saved by accident.
 */
@Composable
internal fun ColumnScope.RatingStep(state: PainCheckUiState.Ready, onChoose: (Int) -> Unit) {
    val zone = state.rating?.let(::painZone)
    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 24.dp, bottom = 16.dp)) {
            state.area?.let { Caption(areaName(it)) }
            ScreenTitle(stringResource(R.string.pain_rating_title), modifier = Modifier.padding(top = 12.dp))
            Chosen(state.rating, zone)
            Scale(state.rating, onChoose)
            Text(
                text = stringResource(zoneMessage(zone)),
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = LevitxColors.Text,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Rule()
        NoteRow(stringResource(R.string.pain_guide_label), stringResource(R.string.pain_guide_body))
        NoteRow(stringResource(R.string.pain_note_label), stringResource(R.string.pain_note_body))
    }
}

/** The number picked, huge and in its zone's colour, with the zone beside it. A dash until one is picked. */
@Composable
private fun Chosen(rating: Int?, zone: PainZone?) {
    val description = if (rating == null) {
        stringResource(R.string.pain_rating_unset)
    } else {
        stringResource(R.string.pain_rating_description, rating)
    }
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .padding(top = 18.dp)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Text(
            text = rating?.toString() ?: stringResource(R.string.pain_rating_none),
            style = LevitxType.Hero.copy(fontSize = 96.sp, lineHeight = 80.sp),
            color = zoneColor(zone),
        )
        Caption(
            text = stringResource(R.string.pain_rating_scale, stringResource(zoneName(zone))),
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
}

/** 0 to 5 on the top row, 6 to 10 under it. */
@Composable
private fun Scale(rating: Int?, onChoose: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .border(1.dp, LevitxColors.Line)
            .selectableGroup(),
    ) {
        Row { for (value in WithinGuide) Cell(value, rating == value, onChoose) }
        Rule()
        Row {
            for (value in OverGuide) Cell(value, rating == value, onChoose)
            // The bottom row is one short: an empty square keeps the columns lined up.
            Box(Modifier.weight(1f))
        }
    }
}

/** One number on the scale: filled with its zone's colour when picked. */
@Composable
private fun RowScope.Cell(value: Int, selected: Boolean, onChoose: (Int) -> Unit) {
    val zone = painZone(value)
    val color = zoneColor(zone)
    val description = stringResource(R.string.pain_rating_description, value)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .height(CellHeight)
            .background(if (selected) color else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = { onChoose(value) })
            .semantics { contentDescription = description },
    ) {
        Text(
            text = value.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                selected -> LevitxColors.Background
                zone == PainZone.LOW -> LevitxColors.TextMuted
                else -> color
            },
        )
    }
}

/** A small label and a line of text, like "GUIDE: up to 5 while training". */
@Composable
private fun NoteRow(label: String, text: String) {
    Row(modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 12.dp)) {
        Caption(label, color = LevitxColors.TextFaint, modifier = Modifier.width(LabelWidth).padding(top = 3.dp))
        Text(
            text = text,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = LevitxColors.Text,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
    Rule()
}

private fun zoneColor(zone: PainZone?): Color = when (zone) {
    PainZone.LOW, null -> LevitxColors.Text
    PainZone.WITHIN_GUIDE -> LevitxColors.Ice
    PainZone.OVER_GUIDE -> LevitxColors.Accent
}

@StringRes
private fun zoneName(zone: PainZone?): Int = when (zone) {
    null -> R.string.pain_zone_unset
    PainZone.LOW -> R.string.pain_zone_low
    PainZone.WITHIN_GUIDE -> R.string.pain_zone_guide
    PainZone.OVER_GUIDE -> R.string.pain_zone_over
}

@StringRes
private fun zoneMessage(zone: PainZone?): Int = when (zone) {
    null -> R.string.pain_message_unset
    PainZone.LOW -> R.string.pain_message_low
    PainZone.WITHIN_GUIDE -> R.string.pain_message_guide
    PainZone.OVER_GUIDE -> R.string.pain_message_over
}
