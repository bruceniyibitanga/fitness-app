package app.levitx.phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R

/*
 * Action bars from the design: the one orange main action on a screen, and
 * quieter dark and outlined bars. All square-edged.
 */

/** Side margin of every screen in the design. */
val ScreenPadding: Dp = 20.dp

/** Height of the main action bar. */
internal val BarHeight = 56.dp

/** Height of a quieter action and of choice buttons. */
internal val QuietBarHeight = 48.dp

/** A disabled bar at reduced opacity, so it still reads as the same control. */
private const val DISABLED_ALPHA = 0.4f

/**
 * The one main action on a screen: an orange bar, label on the left, an
 * optional mono [hint] (like "2 MIN") and an arrow on the right.
 */
@Composable
fun AccentBar(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    enabled: Boolean = true,
    height: Dp = BarHeight,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = height)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .background(LevitxColors.Accent)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = ScreenPadding),
    ) {
        Text(
            text = text,
            color = LevitxColors.Background,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f),
        )
        if (hint != null) {
            Text(
                text = hint,
                style = LevitxType.Micro,
                fontWeight = FontWeight.Medium,
                color = LevitxColors.Background,
                modifier = Modifier.padding(end = 10.dp),
            )
        }
        Icon(painterResource(R.drawable.ic_arrow_forward), contentDescription = null, tint = LevitxColors.Background)
    }
}

/** A quieter action: a dark bar with its label centred. */
@Composable
fun QuietBar(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = QuietBarHeight)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .background(LevitxColors.Surface)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Text(text = text, fontSize = 15.sp, color = LevitxColors.Text)
    }
}

/** An outlined action, for adding something to a list. */
@Composable
fun OutlineBar(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = QuietBarHeight)
            .border(1.dp, LevitxColors.Tick)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Text(text = text, fontSize = 15.sp, color = LevitxColors.Text)
    }
}
