package app.levitx.phone.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.skills.SkillCatalogue
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.SessionList
import app.levitx.phone.ui.StatStrip

/**
 * The plan, as the OnbPlan artboard: the goal's name over its photo, three
 * figures, and the first session the coach planned. Nothing here is asked;
 * it shows what the answers added up to.
 */
@Composable
internal fun ReadyStep(state: OnboardingUiState) {
    val path = state.goalPathId?.let(SkillCatalogue::byId)
    Column(modifier = Modifier.padding(horizontal = ScreenPadding)) {
        Caption(stringResource(R.string.ready_label), color = LevitxColors.Accent)
        Text(
            text = path?.name ?: stringResource(R.string.ready_general),
            style = LevitxType.Poster.copy(fontSize = 56.sp, lineHeight = 52.sp),
            color = LevitxColors.Text,
            modifier = Modifier
                .padding(top = 8.dp)
                .semantics { heading() },
        )
        Rule(Modifier.padding(top = 18.dp))
        StatStrip(
            listOfNotNull(
                path?.let { it.gates.size.toString() to stringResource(R.string.ready_gates) },
                stringResource(R.string.ready_times, state.days.size) to stringResource(R.string.ready_per_week),
                state.minutes.toString() to stringResource(R.string.ready_minutes),
            ),
        )
        Rule()
    }
    state.firstSession?.let { session ->
        Column(modifier = Modifier.padding(top = 8.dp)) {
            SessionList(session, heading = stringResource(R.string.ready_first_session))
        }
    }
}
