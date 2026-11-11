package app.levitx.phone.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R

/*
 * Content blocks from the design: stat strips, photo tiles, numbered rows and
 * square-marked notes.
 */

private val PhotoTileHeight = 80.dp
private val CheckBadgeSize = 20.dp
private val SelectedOutline = 2.dp
private val Fade = Color(0xFF0B0B0A)
private const val FADE_TOP = 0.1f
private const val FADE_BOTTOM = 0.85f

/**
 * A strip of figures with mono captions under them, split by thin lines, like
 * "74.2 / BODY KG" on Today.
 *
 * @param figures Each figure and its caption, in order.
 */
@Composable
fun StatStrip(figures: List<Pair<String, String>>, modifier: Modifier = Modifier, accentLast: Boolean = false) {
    Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        figures.forEachIndexed { index, (figure, caption) ->
            if (index > 0) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(LevitxColors.Line),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (index == 0) 0.dp else 14.dp, top = 12.dp, bottom = 12.dp),
            ) {
                val isAccent = accentLast && index == figures.lastIndex
                Text(
                    text = figure,
                    style = LevitxType.Figure,
                    color = if (isAccent) LevitxColors.Ice else LevitxColors.Text,
                )
                Text(
                    text = caption.uppercase(),
                    style = LevitxType.Micro,
                    color = LevitxColors.TextMuted,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/**
 * A goal as a photo with its name over a dark fade, as on the goal step. The
 * selected one gets an orange outline and a tick.
 */
@Composable
fun PhotoTile(
    @DrawableRes photo: Int,
    title: String,
    caption: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(PhotoTileHeight)
            .background(LevitxColors.Surface)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Image(
            painter = painterResource(photo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(Fade.copy(alpha = FADE_TOP), Fade.copy(alpha = FADE_BOTTOM))),
                ),
        )
        if (selected) {
            Box(Modifier.fillMaxSize().border(SelectedOutline, LevitxColors.Accent))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(CheckBadgeSize)
                    .background(LevitxColors.Accent),
            ) { Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = LevitxColors.Background) }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 10.dp, end = 12.dp),
        ) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = LevitxColors.Text)
            Text(text = caption.uppercase(), style = LevitxType.Micro, color = LevitxColors.TextSoft)
        }
    }
}

/**
 * A row in a numbered list, as on Today: number, name and detail, and a figure
 * with its caption on the right.
 */
@Composable
fun NumberedRow(
    number: Int,
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
    figure: String? = null,
    figureCaption: String? = null,
    highlighted: Boolean = false,
    figureColor: Color = LevitxColors.Text,
    onClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(if (highlighted) LevitxColors.Surface else Color.Transparent)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = ScreenPadding, vertical = 10.dp),
    ) {
        Text(
            text = number.toString().padStart(2, '0'),
            style = LevitxType.Caption,
            color = if (highlighted) LevitxColors.Accent else LevitxColors.TextFaint,
            modifier = Modifier.width(32.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = LevitxColors.Text)
            Text(
                text = detail,
                fontSize = 13.sp,
                color = LevitxColors.TextMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (figure != null) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = figure,
                    style = LevitxType.Figure,
                    fontSize = 17.sp,
                    lineHeight = 20.sp,
                    color = figureColor,
                )
                if (figureCaption != null) {
                    Text(text = figureCaption.uppercase(), style = LevitxType.Micro, color = LevitxColors.TextFaint)
                }
            }
        }
    }
    Rule()
}

/** A one-line note with the design's small orange square in front, like "WHY THIS SESSION". */
@Composable
fun SquareNote(title: String?, text: String, modifier: Modifier = Modifier, filled: Boolean = true) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(if (filled) LevitxColors.Surface else Color.Transparent)
            .padding(horizontal = ScreenPadding, vertical = 10.dp),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(LevitxColors.Accent),
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            if (title != null) Caption(title, color = LevitxColors.Accent)
            Text(text = text, fontSize = 13.sp, lineHeight = 17.sp, color = LevitxColors.Text)
        }
    }
}
