package app.levitx.phone.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachNotice
import app.levitx.core.coach.CoachPhrases
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PlannedSession
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.SuggestionChoice
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Help
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.QuietBar
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.SessionList
import app.levitx.phone.ui.SquareNote
import app.levitx.phone.ui.StatStrip
import app.levitx.phone.ui.Wordmark
import app.levitx.phone.ui.unitCaps
import app.levitx.phone.ui.weightNumber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

/** Today tab: connects [TodayViewModel] to [TodayScreen]. */
@Composable
fun TodayRoute(
    onOpenWorkout: () -> Unit,
    onOpenExercise: (String) -> Unit,
    onOpenProgress: () -> Unit,
    onOpenWeekReview: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    TodayScreen(
        uiState = uiState,
        actions = TodayActions(
            onStart = { choice -> scope.launch { if (viewModel.start(choice)) onOpenWorkout() } },
            onAnswerWeighIn = { kg, choice ->
                scope.launch { if (viewModel.answerWeighIn(kg, choice)) onOpenWorkout() }
            },
            onRest = viewModel::rest,
            onAcknowledgeWeightNotice = viewModel::acknowledgeWeightNotice,
            onOpenExercise = onOpenExercise,
            onOpenProgress = onOpenProgress,
            onOpenWeekReview = onOpenWeekReview,
        ),
    )
}

/**
 * Everything the person can do on Today, grouped so the screen takes one parameter for them.
 *
 * @property onStart Starts the session; the choice is what they picked when the coach offered an easier day.
 * @property onAnswerWeighIn Saves the weigh-in in kg, or skips it when null, then starts with the choice.
 * @property onRest Takes the rest day the coach offered.
 * @property onAcknowledgeWeightNotice Marks the weight notice as read.
 * @property onOpenExercise Opens an exercise's detail by id.
 * @property onOpenProgress Opens Progress, from the figures under the first exercise.
 * @property onOpenWeekReview Opens last week's review.
 */
data class TodayActions(
    val onStart: (SuggestionChoice?) -> Unit,
    val onAnswerWeighIn: (Double?, SuggestionChoice?) -> Unit,
    val onRest: () -> Unit,
    val onAcknowledgeWeightNotice: () -> Unit,
    val onOpenExercise: (String) -> Unit,
    val onOpenProgress: () -> Unit,
    val onOpenWeekReview: () -> Unit,
)

/**
 * The next session as the coach planned it, laid out as the Today artboard:
 * the first exercise big, with its load per rep; the week at a glance; the
 * coach's reason; the session; and Start pinned to the bottom. When the coach
 * noticed something, the choice to ease off comes first. When a weigh-in is
 * due, Start asks for it first.
 */
@Composable
fun TodayScreen(uiState: TodayUiState, actions: TodayActions, modifier: Modifier = Modifier) {
    // Which start is waiting on the weigh-in. Null choice means a plain start, so it needs its own flag.
    var asking by rememberSaveable { mutableStateOf(false) }
    var pending by rememberSaveable { mutableStateOf<SuggestionChoice?>(null) }
    val weighIn = (uiState as? TodayUiState.Ready)?.weighIn
    val starting = actions.copy(
        onStart = { choice ->
            if (weighIn == null) {
                actions.onStart(choice)
            } else {
                pending = choice
                asking = true
            }
        },
    )
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            when (uiState) {
                TodayUiState.Loading -> Unit

                TodayUiState.Unavailable -> Text(
                    text = stringResource(R.string.data_unavailable),
                    color = LevitxColors.TextMuted,
                    modifier = Modifier.padding(ScreenPadding),
                )

                is TodayUiState.Ready -> Ready(uiState, starting)
            }
        }
        if (asking && weighIn != null) {
            WeighInSheet(
                ask = weighIn,
                onSave = { kg ->
                    asking = false
                    actions.onAnswerWeighIn(kg, pending)
                },
                onSkip = {
                    asking = false
                    actions.onAnswerWeighIn(null, pending)
                },
                onDismiss = { asking = false },
            )
        }
    }
}

@Composable
private fun ColumnScope.Ready(state: TodayUiState.Ready, actions: TodayActions) {
    val recommendation = state.recommendation
    val session = recommendation.session
    val suggestion = recommendation.suggestion.takeIf { !state.hasWorkoutInProgress }
    // Lifted out of the cards so the session below shows what was picked.
    var choice by rememberSaveable(suggestion) { mutableStateOf(suggestion?.recommended) }
    val shown = if (choice == SuggestionChoice.LIGHTER) suggestion?.lighter else session
    val comeback = suggestion?.let { comebackOf(it, session) }
    Header(state.nowEpochMillis)
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
    ) {
        if (state.weekReviewDue) WeekReviewCard(actions.onOpenWeekReview)
        if (CoachNotice.FAST_WEIGHT_LOSS in recommendation.notices) WeightNotice(actions.onAcknowledgeWeightNotice)
        if (shown == null) {
            RestDay()
        } else {
            Lead(shown.plans.first(), state.bodyweightKg)
            Rule()
        }
        WeekFigures(state, shown?.estimatedMinutes, actions.onOpenProgress)
        Rule()
        SquareNote(title = stringResource(R.string.today_why), text = CoachPhrases.describe(recommendation.reason))
        Rule()
        if (suggestion != null) {
            if (comeback != null) {
                ComebackCard(comeback, choice, onChoose = { choice = it }, onRest = actions.onRest)
            } else {
                SuggestionCard(suggestion, choice, onChoose = { choice = it }, actions.onStart, actions.onRest)
            }
        }
        if (shown != null) {
            SessionList(shown, state.bodyweightKg, onExerciseClick = actions.onOpenExercise)
            if (state.bodyweightKg == null && shown.plans.any { it.exercise.loadProfile != null }) {
                Help(
                    text = stringResource(R.string.today_bodyweight_prompt),
                    modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 12.dp),
                )
            }
        }
    }
    if (shown != null && (suggestion == null || comeback != null)) {
        StartBar(shown, resuming = state.hasWorkoutInProgress, comeback = comeback != null, choice, actions.onStart)
    }
}

/**
 * Start, pinned to the bottom: Resume when a workout is under way, and on a
 * comeback day it starts whichever of lighter or full was picked.
 */
@Composable
private fun StartBar(
    shown: PlannedSession,
    resuming: Boolean,
    comeback: Boolean,
    choice: SuggestionChoice?,
    onStart: (SuggestionChoice?) -> Unit,
) {
    val label = when {
        resuming -> R.string.today_resume
        !comeback -> R.string.today_start
        choice == SuggestionChoice.LIGHTER -> R.string.comeback_start_lighter
        else -> R.string.comeback_start_full
    }
    AccentBar(
        text = stringResource(label),
        hint = stringResource(R.string.today_minutes, shown.estimatedMinutes),
        onClick = { onStart(if (comeback) choice else null) },
        height = 64.dp,
    )
}

/**
 * Bodyweight, sessions in the last seven days and the session's length.
 * Tapping them opens Progress: the week's figures lead on to the full picture.
 */
@Composable
private fun WeekFigures(state: TodayUiState.Ready, sessionMinutes: Int?, onOpenProgress: () -> Unit) {
    StatStrip(
        figures = listOf(
            (state.bodyweightKg?.let { weightNumber(it) } ?: stringResource(R.string.no_value)) to
                stringResource(R.string.today_body_kg, unitCaps()),
            stringResource(R.string.today_week_count, state.sessionsLastWeek, state.sessionsPerWeek) to
                stringResource(R.string.today_last_seven_days),
            (sessionMinutes?.let { stringResource(R.string.minutes_short, it) } ?: stringResource(R.string.no_value)) to
                stringResource(R.string.today_session_length),
        ),
        modifier = Modifier
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.today_open_progress),
                onClick = onOpenProgress,
            )
            .padding(start = ScreenPadding),
    )
}

/** The top bar: the Levitx mark and today's date. */
@Composable
private fun Header(nowEpochMillis: Long) {
    // Read through the configuration so the date follows a language change.
    val locale = LocalConfiguration.current.locales[0]
    val date = remember(nowEpochMillis, locale) {
        DateTimeFormatter.ofPattern("EEE d MMM", locale)
            .format(Instant.ofEpochMilli(nowEpochMillis).atZone(ZoneId.systemDefault()))
            .uppercase(locale)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = ScreenPadding),
    ) {
        Wordmark(modifier = Modifier.weight(1f))
        Caption(date)
    }
    Rule()
}

/** Nothing safe to train today. */
@Composable
private fun RestDay() {
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 24.dp, bottom = 22.dp)) {
        Caption(stringResource(R.string.today_rest_label))
        Text(
            text = stringResource(R.string.today_rest_title),
            style = LevitxType.Poster,
            color = LevitxColors.Text,
            modifier = Modifier.padding(top = 14.dp).semantics { heading() },
        )
        Lede(stringResource(R.string.today_rest_body), modifier = Modifier.padding(top = 12.dp))
    }
    Rule()
}

/** Said once per spell of fast weight loss, plainly and without judgement. */
@Composable
private fun WeightNotice(onAcknowledge: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(LevitxColors.Surface)
            .padding(ScreenPadding),
    ) {
        Caption(stringResource(R.string.today_weight_label), color = LevitxColors.Ice)
        Text(
            stringResource(R.string.today_weight_body),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = LevitxColors.Text,
        )
        QuietBar(stringResource(R.string.today_weight_ok), onAcknowledge)
    }
}

/** Offers last week's review, at the top of Today until it's opened. */
@Composable
private fun WeekReviewCard(onOpen: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(LevitxColors.Surface)
            .padding(ScreenPadding),
    ) {
        Caption(stringResource(R.string.today_review_label), color = LevitxColors.Accent)
        Text(
            stringResource(R.string.today_review_body),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = LevitxColors.Text,
        )
        QuietBar(stringResource(R.string.today_review_open), onOpen)
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
            actions = TodayActions(
                onStart = {},
                onAnswerWeighIn = { _, _ -> },
                onRest = {},
                onAcknowledgeWeightNotice = {},
                onOpenExercise = {},
                onOpenProgress = {},
                onOpenWeekReview = {},
            ),
        )
    }
}
