package app.levitx.phone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.design.LevitxColors

/** How much the scrim dims the screen behind the sheet. */
private const val SCRIM_ALPHA = 0.72f

/**
 * Asks before something that can't be undone, as the Confirm sheet on the
 * States artboard: what goes, then a way out on the left and the action on
 * the right. Tapping outside it or going back keeps things as they are.
 *
 * @param title The question, such as "Delete this workout?".
 * @param body What goes with it, and that it can't be undone.
 * @param keepLabel The way out.
 * @param confirmLabel The action.
 * @param onKeep Called to close the sheet without doing anything.
 * @param onConfirm Called to go ahead.
 */
@Composable
fun BoxScope.ConfirmSheet(
    title: String,
    body: String,
    keepLabel: String,
    confirmLabel: String,
    onKeep: () -> Unit,
    onConfirm: () -> Unit,
) {
    BackHandler(onBack = onKeep)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevitxColors.Background.copy(alpha = SCRIM_ALPHA))
            .clickable(interactionSource = null, indication = null, onClickLabel = keepLabel, onClick = onKeep),
    )
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
        Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 20.dp, bottom = 22.dp)) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = LevitxColors.Text,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = body,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = LevitxColors.TextSoft,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Row(modifier = Modifier.height(BarHeight)) {
            QuietBar(
                text = keepLabel,
                onClick = onKeep,
                modifier = Modifier
                    .weight(1f)
                    .height(BarHeight),
            )
            AccentBar(text = confirmLabel, onClick = onConfirm, modifier = Modifier.weight(1f))
        }
    }
}
