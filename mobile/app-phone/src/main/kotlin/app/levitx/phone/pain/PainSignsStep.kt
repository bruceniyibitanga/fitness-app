package app.levitx.phone.pain

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.pain.RedFlag
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Help
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.ScreenTitle
import app.levitx.phone.ui.SquareSwitch
import app.levitx.phone.ui.areaName

private val RowHeight = 56.dp

/**
 * Step two, the "Warning-sign screen" artboard: signs that mean a doctor or
 * physio should look at the area before it's trained. Switching any on pauses
 * the area, and the screen says so before the button is pressed.
 */
@Composable
internal fun ColumnScope.SignsStep(state: PainCheckUiState.Ready, onToggle: (RedFlag) -> Unit) {
    Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
        Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 26.dp, bottom = 20.dp)) {
            state.area?.let { Caption(areaName(it)) }
            ScreenTitle(stringResource(R.string.pain_signs_title), modifier = Modifier.padding(top = 12.dp))
            Lede(stringResource(R.string.pain_signs_body), modifier = Modifier.padding(top = 12.dp))
        }
        Rule()
        RedFlag.entries.forEach { sign ->
            SignRow(signText(sign), checked = sign in state.signs, onChange = { onToggle(sign) })
            Rule()
        }
        Outcome(paused = state.signs.isNotEmpty())
    }
}

/** A warning sign with the design's square switch. The whole row toggles it. */
@Composable
private fun SignRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = RowHeight)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = ScreenPadding, vertical = 8.dp),
    ) {
        Text(text, fontSize = 15.sp, lineHeight = 20.sp, color = LevitxColors.Text, modifier = Modifier.weight(1f))
        SquareSwitch(checked, Modifier.padding(start = 16.dp))
    }
}

/** What the answers so far mean, read out when it changes. */
@Composable
private fun Outcome(paused: Boolean) {
    val announce = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    if (paused) {
        Column(
            modifier = announce
                .padding(start = ScreenPadding, end = ScreenPadding, top = 16.dp, bottom = 16.dp)
                .fillMaxWidth()
                .background(LevitxColors.Surface)
                .border(1.dp, LevitxColors.Accent)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Caption(stringResource(R.string.pain_signs_stop_label), color = LevitxColors.Accent)
            Text(
                text = stringResource(R.string.pain_signs_stop_body),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = LevitxColors.Text,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    } else {
        Help(
            text = stringResource(R.string.pain_signs_clear),
            modifier = announce.padding(start = ScreenPadding, end = ScreenPadding, top = 16.dp, bottom = 16.dp),
        )
    }
}

/** A warning sign in plain words. */
@Composable
private fun signText(sign: RedFlag): String = stringResource(
    when (sign) {
        RedFlag.NIGHT_OR_REST_PAIN -> R.string.pain_sign_night
        RedFlag.NUMBNESS_OR_TINGLING -> R.string.pain_sign_numb
        RedFlag.SWELLING_OR_DEFORMITY -> R.string.pain_sign_swelling
        RedFlag.SUDDEN_INJURY -> R.string.pain_sign_sudden
        RedFlag.WORSENING_WEAKNESS -> R.string.pain_sign_weak
        RedFlag.FEVER_OR_WEIGHT_LOSS -> R.string.pain_sign_fever
    },
)
