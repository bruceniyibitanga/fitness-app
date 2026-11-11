package app.levitx.phone.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachNotice
import app.levitx.core.coach.CoachPhrases
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.ExercisePlan
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.Recommendation
import app.levitx.core.coach.Suggestion
import app.levitx.core.coach.SuggestionChoice
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxSpacing
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.OptionCard
import app.levitx.phone.ui.PrimaryButton
import app.levitx.phone.ui.SecondaryButton
import app.levitx.phone.ui.SectionLabel
import app.levitx.phone.ui.measureText
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val ReasonMarkSize = 6.dp

/** Today tab: connects [TodayViewModel] to [TodayScreen]. */
@Composable
fun TodayRoute(onOpenWorkout: () -> Unit, viewModel: TodayViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    TodayScreen(
        uiState = uiState,
        onStart = { choice -> scope.launch { if (viewModel.start(choice)) onOpenWorkout() } },
        onRest = viewModel::rest,
        onAcknowledgeWeightNotice = viewModel::acknowledgeWeightNotice,
    )
}

/**
 * The next session as the coach planned it, the reason for it, and when the
 * coach noticed something, the choice to ease off.
 */
@Composable
fun TodayScreen(
    uiState: TodayUiState,
    onStart: (SuggestionChoice?) -> Unit,
    onRest: () -> Unit,
    onAcknowledgeWeightNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(LevitxSpacing.Medium),
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(LevitxSpacing.Medium),
    ) {
        when (uiState) {
            TodayUiState.Loading -> Unit
            TodayUiState.Unavailable -> Text(stringResource(R.string.data_unavailable))
            is TodayUiState.Ready -> Ready(uiState, onStart, onRest, onAcknowledgeWeightNotice)
        }
    }
}

@Composable
private fun Ready(
    state: TodayUiState.Ready,
    onStart: (SuggestionChoice?) -> Unit,
    onRest: () -> Unit,
    onAcknowledgeWeightNotice: () -> Unit,
) {
    val recommendation = state.recommendation
    Header(state.nowEpochMillis)
    if (CoachNotice.FAST_WEIGHT_LOSS in recommendation.notices) WeightNotice(onAcknowledgeWeightNotice)
    val session = recommendation.session
    if (session == null) {
        Text(stringResource(R.string.today_rest_title), style = LevitxType.Display)
        Text(
            text = stringResource(R.string.today_rest_body),
            style = MaterialTheme.typography.bodyLarge,
            color = LevitxColors.TextSoft,
        )
        Reason(recommendation)
        return
    }
    if (state.hasWorkoutInProgress) {
        PrimaryButton(stringResource(R.string.today_resume), onClick = { onStart(null) })
    }
    val suggestion = recommendation.suggestion
    if (suggestion != null && !state.hasWorkoutInProgress) {
        SuggestionCard(suggestion, onStart, onRest)
    }
    Lead(session.plans.first())
    Reason(recommendation)
    SessionList(session, state.bodyweightKg)
    if (state.bodyweightKg == null && session.plans.any { it.exercise.loadProfile != null }) {
        Text(
            text = stringResource(R.string.today_bodyweight_prompt),
            style = MaterialTheme.typography.bodySmall,
            color = LevitxColors.TextMuted,
        )
    }
    if (suggestion == null && !state.hasWorkoutInProgress) {
        PrimaryButton(
            text = stringResource(R.string.today_start_minutes, session.estimatedMinutes),
            onClick = { onStart(null) },
        )
    }
}

@Composable
private fun Header(nowEpochMillis: Long) {
    val date = remember(nowEpochMillis) {
        DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
            .format(Instant.ofEpochMilli(nowEpochMillis).atZone(ZoneId.systemDefault()))
            .uppercase(Locale.getDefault())
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.wordmark), color = LevitxColors.Text, modifier = Modifier.weight(1f))
        SectionLabel(date)
    }
    HorizontalDivider(color = LevitxColors.Line)
}

/** The first exercise, big: what the person does first. */
@Composable
private fun Lead(plan: ExercisePlan) {
    SectionLabel(
        text = if (plan.gateId !=
            null
        ) {
            stringResource(R.string.today_skill)
        } else {
            stringResource(R.string.today_session_label)
        },
        color = LevitxColors.Accent,
    )
    Text(
        text = stringResource(R.string.today_sets, plan.sets.size, measureText(plan.exercise.measure, plan.workTarget)),
        style = LevitxType.Display,
    )
    Text(
        text = plan.exercise.name,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.semantics { heading() },
    )
}

/** The coach's one sentence on why this session. */
@Composable
private fun Reason(recommendation: Recommendation) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(LevitxSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(LevitxColors.Surface)
            .padding(LevitxSpacing.Medium),
    ) {
        Spacer(Modifier.size(ReasonMarkSize).background(LevitxColors.Accent))
        Column(verticalArrangement = Arrangement.spacedBy(LevitxSpacing.XSmall)) {
            SectionLabel(stringResource(R.string.today_why), color = LevitxColors.Accent)
            Text(CoachPhrases.describe(recommendation.reason), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** The coach noticed something: what, and Keep, Lighter or Rest. The person decides. */
@Composable
private fun SuggestionCard(suggestion: Suggestion, onStart: (SuggestionChoice?) -> Unit, onRest: () -> Unit) {
    var choice by rememberSaveable(suggestion) { mutableStateOf(suggestion.recommended) }
    val suggested = stringResource(R.string.how_suggested)
    Column(
        verticalArrangement = Arrangement.spacedBy(LevitxSpacing.Small),
        modifier = Modifier
            .fillMaxWidth()
            .background(LevitxColors.Surface)
            .padding(LevitxSpacing.Medium),
    ) {
        SectionLabel(stringResource(R.string.today_check_label), color = LevitxColors.Accent)
        Text(stringResource(R.string.today_check_title), style = MaterialTheme.typography.headlineSmall)
        SectionLabel(stringResource(R.string.today_check_noticed))
        suggestion.signals.forEach { signal ->
            Text(CoachPhrases.describe(signal), style = MaterialTheme.typography.bodyMedium)
        }
        SectionLabel(stringResource(R.string.today_check_question))
        listOf(
            Triple(SuggestionChoice.KEEP, R.string.today_choice_keep, R.string.today_choice_keep_body),
            Triple(SuggestionChoice.LIGHTER, R.string.today_choice_lighter, R.string.today_choice_lighter_body),
            Triple(SuggestionChoice.REST, R.string.today_choice_rest, R.string.today_choice_rest_body),
        ).forEach { (option, title, body) ->
            OptionCard(
                title = stringResource(title),
                body = stringResource(body),
                selected = choice == option,
                onClick = { choice = option },
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
        Text(
            text = stringResource(R.string.today_choice_note),
            style = MaterialTheme.typography.bodySmall,
            color = LevitxColors.TextMuted,
        )
        when (choice) {
            SuggestionChoice.REST -> PrimaryButton(text = stringResource(R.string.today_take_rest), onClick = onRest)

            SuggestionChoice.LIGHTER -> PrimaryButton(
                text = stringResource(R.string.today_start_lighter),
                onClick = { onStart(SuggestionChoice.LIGHTER) },
            )

            SuggestionChoice.KEEP -> PrimaryButton(
                text = stringResource(R.string.today_start),
                onClick = { onStart(SuggestionChoice.KEEP) },
            )
        }
    }
}

/** Said once per spell of fast weight loss, plainly and without judgement. */
@Composable
private fun WeightNotice(onAcknowledge: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(LevitxSpacing.Small),
        modifier = Modifier
            .fillMaxWidth()
            .background(LevitxColors.Surface)
            .padding(LevitxSpacing.Medium),
    ) {
        SectionLabel(stringResource(R.string.today_weight_label), color = LevitxColors.Ice)
        Text(stringResource(R.string.today_weight_body), style = MaterialTheme.typography.bodyMedium)
        SecondaryButton(stringResource(R.string.today_weight_ok), onAcknowledge)
    }
}

@Preview
@Composable
private fun TodayScreenPreview() {
    val model = AthleteModel.starting(
        CoachProfile("first_pull_up", equipment = setOf(Equipment.PULL_UP_BAR, Equipment.LOW_BAR)),
        PullUpAnswer.NOT_YET,
    )
    LevitxTheme {
        TodayScreen(
            uiState = TodayUiState.Ready(
                nowEpochMillis = 0,
                recommendation = Coach().recommend(model, 0),
                bodyweightKg = 74.2,
                hasWorkoutInProgress = false,
            ),
            onStart = {},
            onRest = {},
            onAcknowledgeWeightNotice = {},
        )
    }
}
