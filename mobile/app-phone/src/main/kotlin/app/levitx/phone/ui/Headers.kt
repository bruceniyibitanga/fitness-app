package app.levitx.phone.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R

/*
 * The tops of screens from the design: the Levitx wordmark, a tab's header,
 * the back button, and a flow step's "2 / 4" header with its progress line.
 */

private const val WORDMARK_TRACKING = 2.4f
private val ProgressHeight = 2.dp

/** The Levitx mark and wordmark, as in the top-left of the design. */
@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    MarkLabel(stringResource(R.string.app_name), modifier)
}

/**
 * The top of a tab: the mark and the tab's name, 56 dp tall, with a rule
 * under it. [trailing] sits on the right, for a tab that has an action there.
 */
@Composable
fun TabHeader(title: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .padding(start = ScreenPadding, end = 8.dp),
        ) {
            MarkLabel(title, Modifier.weight(1f).semantics { heading() })
            trailing()
        }
        Rule()
    }
}

/** The orange mark beside a name in wide-spaced capitals. */
@Composable
private fun MarkLabel(text: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Image(painter = painterResource(R.drawable.ic_mark), contentDescription = null)
        Text(
            text = text.uppercase(),
            style = LevitxType.Caption,
            fontWeight = FontWeight.Medium,
            letterSpacing = WORDMARK_TRACKING.sp,
            color = LevitxColors.Text,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

/** A back button: a 48 dp target with the arrow centred. */
@Composable
fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.back)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(QuietBarHeight)
            .clickable(role = Role.Button, onClick = onBack)
            .semantics { contentDescription = description },
    ) { Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = null, tint = LevitxColors.Text) }
}

/**
 * The top of a step in a flow: back on the left, "2 / 4" on the right, and a
 * 2 dp progress line under it.
 */
@Composable
fun StepHeader(index: Int, count: Int, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(BarHeight)
                .padding(start = 4.dp, end = ScreenPadding),
        ) {
            BackButton(onBack)
            Box(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.step_of, index, count),
                style = LevitxType.Caption,
                color = LevitxColors.TextMuted,
            )
        }
        FlatProgressBar(
            fraction = index.toFloat() / count,
            height = ProgressHeight,
            modifier = Modifier.padding(horizontal = ScreenPadding),
        )
    }
}
