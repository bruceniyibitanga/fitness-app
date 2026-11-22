package app.levitx.phone.pain

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.coach.BodyArea
import app.levitx.core.pain.PainAdvice
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.ScreenTitle
import app.levitx.phone.ui.areaName
import app.levitx.phone.ui.soreColor
import app.levitx.phone.ui.soreStatus

private val RowHeight = 56.dp

/**
 * Step one: where it hurts. Tapping an area moves on, so there is no button.
 * Areas the coach is already working around say so, and picking one again
 * updates it.
 */
@Composable
internal fun ColumnScope.AreaStep(sore: Map<BodyArea, PainAdvice>, onChoose: (BodyArea) -> Unit) {
    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 26.dp, bottom = 20.dp)) {
            ScreenTitle(stringResource(R.string.pain_area_title))
            Lede(stringResource(R.string.pain_area_body), modifier = Modifier.padding(top = 12.dp))
        }
        Rule()
        BodyArea.entries.forEach { area ->
            AreaRow(area, sore[area], onClick = { onChoose(area) })
            Rule()
        }
    }
}

/** One area: its name, what the coach is doing about it if it's sore, and a chevron. */
@Composable
private fun AreaRow(area: BodyArea, advice: PainAdvice?, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowHeight)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = ScreenPadding),
    ) {
        Text(areaName(area), fontSize = 15.sp, color = LevitxColors.Text, modifier = Modifier.weight(1f))
        soreStatus(advice)?.let { status ->
            Caption(text = status, color = soreColor(advice), modifier = Modifier.padding(end = 12.dp))
        }
        Icon(painterResource(R.drawable.ic_chevron_forward), contentDescription = null, tint = LevitxColors.TextFaint)
    }
}
