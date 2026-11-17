package app.levitx.phone.train

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.levitx.core.intervals.IntervalKind
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.ScreenPadding

/*
 * The Train tab's filter row: small mono toggles under the chips, orange
 * and underlined when on, as on the Train artboard.
 */

private val FilterTouchHeight = 48.dp
private val UnderlineHeight = 2.dp

/** Every filter as a toggle, scrolling sideways. */
@Composable
internal fun FilterRow(on: Set<TrainFilter>, onToggle: (TrainFilter) -> Unit) {
    val label = stringResource(R.string.train_filters)
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = ScreenPadding, vertical = 4.dp)
            .semantics { contentDescription = label },
    ) {
        TrainFilter.entries.forEach { filter ->
            FilterToggle(stringResource(filterName(filter)), filter in on, onToggle = { onToggle(filter) })
        }
    }
}

/** One filter: muted when off, orange with a line under it when on. 48 dp tall to be easy to hit. */
@Composable
private fun FilterToggle(text: String, on: Boolean, onToggle: () -> Unit) {
    val color = if (on) LevitxColors.Accent else LevitxColors.TextMuted
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .heightIn(min = FilterTouchHeight)
            .toggleable(value = on, role = Role.Checkbox, onValueChange = { onToggle() }),
    ) {
        Text(
            text = text,
            style = LevitxType.Caption,
            color = color,
            modifier = Modifier
                .drawBehind {
                    if (on) {
                        val line = UnderlineHeight.toPx()
                        drawRect(color, topLeft = Offset(0f, size.height - line), size = Size(size.width, line))
                    }
                }
                .padding(vertical = 8.dp),
        )
    }
}

/** What a chip says. */
@Composable
internal fun chipName(chip: TrainChip): String = chip.focus?.let { focusName(it) } ?: stringResource(
    when (chip.kind) {
        null -> R.string.train_all
        IntervalKind.CARDIO -> R.string.pattern_cardio
        IntervalKind.MOBILITY -> R.string.pattern_mobility
    },
)

private fun filterName(filter: TrainFilter): Int = when (filter) {
    TrainFilter.NO_KIT -> R.string.train_filter_no_kit
    TrainFilter.MY_KIT -> R.string.train_filter_my_kit
    TrainFilter.SHORT -> R.string.train_filter_short
    TrainFilter.QUIET -> R.string.train_filter_quiet
    TrainFilter.BEGINNER -> R.string.train_filter_beginner
}
