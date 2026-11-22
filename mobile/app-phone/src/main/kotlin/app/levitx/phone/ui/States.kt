package app.levitx.phone.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.design.LevitxColors

/** Dash and gap of the dashed lines in the empty states, in dp. */
private const val DASH_DP = 4f

/** Where the waiting line sits in its box, from the top. */
private const val LINE_AT = 0.75f

/**
 * An empty state from the States artboard: a small label, an optional
 * picture of what will appear, one short line, and at most one action.
 * It says what will show up, never why it isn't there yet.
 *
 * @param label What this is, in capitals (for example "NO SESSIONS YET").
 * @param title The one line. Keep it to a sentence of a few words.
 * @param picture A small drawing of what will appear, or null.
 * @param action The one thing to do, or null.
 */
@Composable
fun EmptyState(
    label: String,
    title: String,
    modifier: Modifier = Modifier,
    picture: (@Composable () -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(ScreenPadding),
    ) {
        Caption(label, color = LevitxColors.TextFaint)
        picture?.invoke()
        Text(
            text = title,
            fontSize = 20.sp,
            lineHeight = 25.sp,
            fontWeight = FontWeight.Bold,
            color = LevitxColors.Text,
            modifier = Modifier.semantics { heading() },
        )
        action?.let { Box(Modifier.padding(top = 4.dp)) { it() } }
    }
}

/**
 * The "no sessions yet" picture: a dashed line waiting to be drawn, with an
 * orange square where the first point will go. Decoration only.
 */
@Composable
fun FirstPointLine(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clearAndSetSemantics {},
    ) {
        val dash = DASH_DP.dp.toPx()
        val y = size.height * LINE_AT
        drawLine(
            color = LevitxColors.Tick,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
        )
        val square = 8.dp.toPx()
        drawRect(
            color = LevitxColors.Accent,
            topLeft = Offset(0f, y - square / 2),
            size = Size(square, square),
        )
    }
}

/**
 * How far along something is, as squares: [done] solid, the rest dashed.
 * The "not enough yet" picture. Decoration only: the line beside it says
 * the number.
 */
@Composable
fun SquaresToGo(done: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {},
    ) {
        repeat(total) { index ->
            val square = Modifier
                .weight(1f)
                .height(30.dp)
            if (index < done) {
                Box(square.background(LevitxColors.Text))
            } else {
                Box(square.dashedBorder())
            }
        }
    }
}

/** A 1 dp dashed outline, as the empty squares on the States artboard. */
private fun Modifier.dashedBorder(): Modifier = drawBehind {
    val dash = DASH_DP.dp.toPx()
    val stroke = 1.dp.toPx()
    drawRect(
        color = LevitxColors.Outline,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash))),
    )
}
