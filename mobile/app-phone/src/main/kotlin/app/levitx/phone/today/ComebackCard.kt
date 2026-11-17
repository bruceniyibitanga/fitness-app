package app.levitx.phone.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.coach.PlannedSession
import app.levitx.core.coach.ReadinessSignal
import app.levitx.core.coach.Suggestion
import app.levitx.core.coach.SuggestionChoice
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.ScreenPadding

private val OptionHeight = 64.dp

/**
 * Today's top card after a break, laid out as the "Comeback day" artboard:
 * how long they've been away, that nothing is lost, and a choice between the
 * lighter session the coach suggests and the full one. The choice is the
 * person's; resting stays one tap away. Start sits in Today's bottom bar.
 *
 * @param choice What's picked, LIGHTER or KEEP.
 * @param onChoose Picks LIGHTER or KEEP.
 * @param onRest Takes the day off instead. The session waits.
 */
@Composable
internal fun ComebackCard(
    comeback: Comeback,
    choice: SuggestionChoice?,
    onChoose: (SuggestionChoice) -> Unit,
    onRest: () -> Unit,
) {
    val lighter = choice != SuggestionChoice.KEEP
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LevitxColors.Surface)
            .padding(start = ScreenPadding, end = ScreenPadding, top = 26.dp, bottom = 12.dp),
    ) {
        Caption(
            pluralStringResource(R.plurals.comeback_label, comeback.days, comeback.days),
            color = LevitxColors.Accent,
        )
        Text(
            text = stringResource(R.string.comeback_title),
            style = LevitxType.Title.copy(fontSize = 40.sp, lineHeight = 39.sp),
            color = LevitxColors.Text,
            modifier = Modifier.padding(top = 12.dp).semantics { heading() },
        )
        Text(
            text = stringResource(R.string.comeback_body),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = LevitxColors.TextSoft,
            modifier = Modifier.padding(top = 12.dp),
        )
        Choices(comeback, lighter, onChoose)
        Text(
            text = stringResource(if (lighter) R.string.comeback_lighter_note else R.string.comeback_full_note),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = LevitxColors.TextMuted,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            text = stringResource(R.string.comeback_rest),
            fontSize = 14.sp,
            color = LevitxColors.TextMuted,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .padding(top = 4.dp)
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onRest)
                .wrapContentHeight(),
        )
    }
}

/** "HOW MUCH TODAY", then lighter or full, side by side. */
@Composable
private fun Choices(comeback: Comeback, lighter: Boolean, onChoose: (SuggestionChoice) -> Unit) {
    Caption(stringResource(R.string.comeback_how), modifier = Modifier.padding(top = 22.dp))
    Row(
        modifier = Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .border(1.dp, LevitxColors.Tick)
            .selectableGroup(),
    ) {
        Option(
            title = stringResource(R.string.comeback_lighter),
            sub = stringResource(R.string.comeback_suggested_minutes, comeback.lighterMinutes),
            selected = lighter,
            onClick = { onChoose(SuggestionChoice.LIGHTER) },
        )
        Option(
            title = stringResource(R.string.comeback_full),
            sub = stringResource(R.string.comeback_planned_minutes, comeback.fullMinutes),
            selected = !lighter,
            onClick = { onChoose(SuggestionChoice.KEEP) },
        )
    }
}

/** Half of the two-way choice: light when picked, as on the artboard. */
@Composable
private fun RowScope.Option(title: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        modifier = Modifier
            .weight(1f)
            .heightIn(min = OptionHeight)
            .background(if (selected) LevitxColors.Text else LevitxColors.Surface)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        Text(
            title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) LevitxColors.Background else LevitxColors.Text,
        )
        Caption(sub, color = if (selected) LevitxColors.Tick else LevitxColors.TextMuted)
    }
}

/**
 * The first session after two weeks or more away, as the coach sees it.
 *
 * @property days Whole days since the last session.
 * @property lighterMinutes How long the lighter session takes.
 * @property fullMinutes How long the session as planned takes.
 */
internal data class Comeback(val days: Int, val lighterMinutes: Int, val fullMinutes: Int)

/**
 * The comeback, when [suggestion] is the coach's offer after a long gap, or
 * null for any other suggestion. A long gap is a strong signal, so it always
 * comes alone.
 */
internal fun comebackOf(suggestion: Suggestion, session: PlannedSession?): Comeback? {
    val gap = suggestion.signals.singleOrNull() as? ReadinessSignal.LongGap ?: return null
    val lighter = suggestion.lighter.estimatedMinutes
    return Comeback(gap.days, lighter, session?.estimatedMinutes ?: lighter)
}
