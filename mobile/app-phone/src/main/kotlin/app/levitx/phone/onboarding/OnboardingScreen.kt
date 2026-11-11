package app.levitx.phone.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PlanStyle
import app.levitx.core.coach.PullUpAnswer
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxSpacing
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.FlatProgressBar
import app.levitx.phone.ui.PrimaryButton
import app.levitx.phone.ui.SectionLabel
import java.time.DayOfWeek
import kotlinx.coroutines.launch

/** Steps after the welcome, which the progress bar counts. */
private val CountedSteps = OnboardingStep.entries.drop(1)

/** Setup destination: connects [OnboardingViewModel] to [OnboardingScreen]. */
@Composable
fun OnboardingRoute(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    // Back steps through setup before it leaves the app.
    BackHandler(enabled = uiState.step != OnboardingStep.WELCOME) { viewModel.back() }
    OnboardingScreen(
        uiState = uiState,
        actions = OnboardingActions(
            onNext = viewModel::next,
            onBack = { viewModel.back() },
            onChooseGoal = viewModel::chooseGoal,
            onChooseStyle = viewModel::chooseStyle,
            onAnswerPullUp = viewModel::answerPullUp,
            onTypeBodyweight = viewModel::typeBodyweight,
            onToggleDay = viewModel::toggleDay,
            onChooseMinutes = viewModel::chooseMinutes,
            onToggleEquipment = viewModel::toggleEquipment,
            onFinish = { scope.launch { if (viewModel.finish()) onFinished() } },
        ),
    )
}

/**
 * Everything the person can do during setup, grouped so the screen takes one parameter for them.
 *
 * @property onNext Moves to the next step.
 * @property onBack Goes back a step.
 * @property onChooseGoal Picks a skill path by id, or general fitness with null.
 * @property onChooseStyle Picks all-round or focused.
 * @property onAnswerPullUp Answers "Can you do a strict pull-up?".
 * @property onTypeBodyweight Updates the bodyweight field.
 * @property onToggleDay Adds or removes a training day.
 * @property onChooseMinutes Picks the time per session.
 * @property onToggleEquipment Adds or removes a piece of kit.
 * @property onFinish Saves the answers and starts training.
 */
data class OnboardingActions(
    val onNext: () -> Unit,
    val onBack: () -> Unit,
    val onChooseGoal: (String?) -> Unit,
    val onChooseStyle: (PlanStyle) -> Unit,
    val onAnswerPullUp: (PullUpAnswer) -> Unit,
    val onTypeBodyweight: (String) -> Unit,
    val onToggleDay: (DayOfWeek) -> Unit,
    val onChooseMinutes: (Int) -> Unit,
    val onToggleEquipment: (Equipment) -> Unit,
    val onFinish: () -> Unit,
)

/** Setup, one step at a time, with the main action pinned to the bottom. */
@Composable
fun OnboardingScreen(uiState: OnboardingUiState, actions: OnboardingActions, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Column(modifier = Modifier.padding(LevitxSpacing.Medium)) {
                PrimaryButton(
                    text = stringResource(
                        when (uiState.step) {
                            OnboardingStep.WELCOME -> R.string.welcome_start
                            OnboardingStep.WEEK -> R.string.week_build
                            OnboardingStep.READY -> R.string.ready_start
                            else -> R.string.continue_button
                        },
                    ),
                    onClick = if (uiState.step == OnboardingStep.READY) actions.onFinish else actions.onNext,
                    enabled = uiState.canContinue && !uiState.saving,
                )
            }
        },
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(LevitxSpacing.Medium),
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LevitxSpacing.Medium, vertical = LevitxSpacing.Medium),
        ) {
            if (uiState.step != OnboardingStep.WELCOME) StepHeader(uiState.step, actions.onBack)
            when (uiState.step) {
                OnboardingStep.WELCOME -> Welcome()
                OnboardingStep.GOAL -> GoalStep(uiState, actions)
                OnboardingStep.HOW -> HowStep(uiState, actions)
                OnboardingStep.WEEK -> WeekStep(uiState, actions)
                OnboardingStep.READY -> ReadyStep(uiState)
            }
            if (uiState.unavailable) Text(stringResource(R.string.data_unavailable), color = LevitxColors.Accent)
        }
    }
}

@Composable
private fun StepHeader(step: OnboardingStep, onBack: () -> Unit) {
    val number = CountedSteps.indexOf(step) + 1
    Row(modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
        Spacer(Modifier.weight(1f))
        SectionLabel(
            stringResource(R.string.onboarding_step, number, CountedSteps.size),
            modifier = Modifier.padding(top = LevitxSpacing.Medium),
        )
    }
    FlatProgressBar(fraction = number.toFloat() / CountedSteps.size)
}

@Composable
internal fun Title(text: String) {
    Text(text = text, style = LevitxType.Display)
}

@Composable
internal fun Body(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge, color = LevitxColors.TextSoft)
}

@Composable
private fun Welcome() {
    Spacer(Modifier.padding(top = LevitxSpacing.XLarge))
    SectionLabel(stringResource(R.string.wordmark), color = LevitxColors.Text)
    SectionLabel(stringResource(R.string.welcome_kicker), color = LevitxColors.Accent)
    Title(stringResource(R.string.welcome_title))
    Body(stringResource(R.string.welcome_body))
    Text(
        text = stringResource(R.string.welcome_private),
        style = MaterialTheme.typography.bodySmall,
        color = LevitxColors.TextMuted,
    )
}

private val PreviewActions = OnboardingActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {})

@Preview
@Composable
private fun GoalStepPreview() {
    LevitxTheme {
        OnboardingScreen(OnboardingUiState(step = OnboardingStep.GOAL), PreviewActions)
    }
}

@Preview
@Composable
private fun WeekStepPreview() {
    LevitxTheme {
        OnboardingScreen(OnboardingUiState(step = OnboardingStep.WEEK), PreviewActions)
    }
}
