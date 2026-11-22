package app.levitx.phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.levitx.design.LevitxColors

/**
 * The design's square switch: an orange track with a dark knob when on, an
 * outline with a grey knob when off. It only draws the state. The row that
 * holds it does the toggling, so the whole row is the touch target.
 */
@Composable
fun SquareSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        modifier = modifier
            .width(44.dp)
            .height(24.dp)
            .background(if (checked) LevitxColors.Accent else LevitxColors.Background)
            .border(1.dp, if (checked) LevitxColors.Accent else LevitxColors.Outline)
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .size(16.dp)
                .background(if (checked) LevitxColors.Background else LevitxColors.TextFaint),
        )
    }
}
