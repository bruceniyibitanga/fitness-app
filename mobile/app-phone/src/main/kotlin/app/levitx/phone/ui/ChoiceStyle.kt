package app.levitx.phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxFonts

/*
 * Square choice buttons from the design, for single and multiple choices
 * laid out in rows.
 */

/** How a selected [ChoiceBar] looks. */
enum class ChoiceStyle {
    /** Orange fill: a single choice, like "45 min". */
    ACCENT,

    /** Cream fill: one of several, like training days. */
    CREAM,
}

/** A square choice in a row of choices. Unselected ones are outlined. */
@Composable
fun RowScope.ChoiceBar(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ChoiceStyle = ChoiceStyle.ACCENT,
    multiple: Boolean = false,
    mono: Boolean = false,
    description: String? = null,
) {
    val fill = when {
        !selected -> LevitxColors.Surface
        style == ChoiceStyle.CREAM -> LevitxColors.Text
        else -> LevitxColors.Accent
    }
    val select = if (multiple) {
        Modifier.toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
    } else {
        Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .weight(1f)
            .heightIn(min = QuietBarHeight)
            .background(fill)
            .then(if (selected) Modifier else Modifier.border(1.dp, LevitxColors.Tick))
            .then(select)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier),
    ) {
        Text(
            text = text,
            fontSize = if (mono) 12.sp else 15.sp,
            fontFamily = if (mono) LevitxFonts.JetBrainsMono else null,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = when {
                selected -> LevitxColors.Background
                mono -> LevitxColors.TextMuted
                else -> LevitxColors.Text
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}

/** A row of [ChoiceBar]s with the design's 6 dp gaps. */
@Composable
fun ChoiceRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier.fillMaxWidth(), content = content)
}
