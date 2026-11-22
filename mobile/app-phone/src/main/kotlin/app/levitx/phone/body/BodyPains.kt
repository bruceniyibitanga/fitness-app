package app.levitx.phone.body

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.coach.BodyArea
import app.levitx.core.pain.PainAdvice
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.OutlineBar
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.areaName
import app.levitx.phone.ui.soreColor
import app.levitx.phone.ui.soreStatus

private val RowHeight = 48.dp

/**
 * "ACHES AND PAINS": the areas the coach is working around, each with what it
 * is doing about it, and the way into the pain check. With nothing sore it is
 * one line and the button.
 */
@Composable
internal fun Pains(sore: Map<BodyArea, PainAdvice>, onOpenPainCheck: () -> Unit) {
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 22.dp, bottom = 24.dp)) {
        Caption(
            stringResource(R.string.body_pain_label),
            modifier = Modifier
                .padding(bottom = 8.dp)
                .semantics { heading() },
        )
        // In body order, whatever order they were reported in.
        BodyArea.entries.filter { it in sore }.forEach { area ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().heightIn(min = RowHeight),
            ) {
                Text(areaName(area), fontSize = 15.sp, color = LevitxColors.Text, modifier = Modifier.weight(1f))
                soreStatus(sore[area])?.let { Caption(text = it, color = soreColor(sore[area])) }
            }
            Rule()
        }
        OutlineBar(
            text = stringResource(if (sore.isEmpty()) R.string.body_pain_open else R.string.body_pain_update),
            onClick = onOpenPainCheck,
            modifier = Modifier.padding(top = if (sore.isEmpty()) 0.dp else 12.dp),
        )
    }
}
