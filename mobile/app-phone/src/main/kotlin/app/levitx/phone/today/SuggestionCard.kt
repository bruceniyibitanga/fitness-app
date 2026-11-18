package app.levitx.phone.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.coach.CoachPhrases
import app.levitx.core.coach.Suggestion
import app.levitx.core.coach.SuggestionChoice
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Help
import app.levitx.phone.ui.OptionCard
import app.levitx.phone.ui.ScreenPadding

/** The coach noticed something: what, and Keep, Lighter or Rest. The person decides. */
@Composable
internal fun SuggestionCard(
    suggestion: Suggestion,
    choice: SuggestionChoice?,
    onChoose: (SuggestionChoice) -> Unit,
    onStart: (SuggestionChoice?) -> Unit,
    onRest: () -> Unit,
) {
    val suggested = stringResource(R.string.how_suggested)
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(LevitxColors.Surface)
            .padding(ScreenPadding),
    ) {
        Caption(stringResource(R.string.today_check_label), color = LevitxColors.Accent)
        Text(
            text = stringResource(R.string.today_check_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = LevitxColors.Text,
            modifier = Modifier.semantics { heading() },
        )
        Caption(stringResource(R.string.today_check_noticed), modifier = Modifier.padding(top = 6.dp))
        suggestion.signals.forEach { signal ->
            Text(CoachPhrases.describe(signal), fontSize = 14.sp, lineHeight = 20.sp, color = LevitxColors.Text)
        }
        Caption(stringResource(R.string.today_check_question), modifier = Modifier.padding(top = 6.dp))
        listOf(
            Triple(SuggestionChoice.KEEP, R.string.today_choice_keep, R.string.today_choice_keep_body),
            Triple(SuggestionChoice.LIGHTER, R.string.today_choice_lighter, R.string.today_choice_lighter_body),
            Triple(SuggestionChoice.REST, R.string.today_choice_rest, R.string.today_choice_rest_body),
        ).forEach { (option, title, body) ->
            OptionCard(
                title = stringResource(title),
                body = stringResource(body),
                selected = choice == option,
                onClick = { onChoose(option) },
                badge = suggested.takeIf { option == suggestion.recommended },
                trailing = when (option) {
                    SuggestionChoice.LIGHTER -> stringResource(
                        R.string.today_minutes,
                        suggestion.lighter.estimatedMinutes,
                    )

                    else -> null
                },
            )
        }
        Help(stringResource(R.string.today_choice_note))
        when (choice) {
            SuggestionChoice.REST -> AccentBar(text = stringResource(R.string.today_take_rest), onClick = onRest)

            SuggestionChoice.LIGHTER -> AccentBar(
                text = stringResource(R.string.today_start_lighter),
                onClick = { onStart(SuggestionChoice.LIGHTER) },
            )

            SuggestionChoice.KEEP, null -> AccentBar(
                text = stringResource(R.string.today_start),
                onClick = { onStart(SuggestionChoice.KEEP) },
            )
        }
    }
}
