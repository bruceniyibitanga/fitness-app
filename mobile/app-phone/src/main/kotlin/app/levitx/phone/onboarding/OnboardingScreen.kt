package app.levitx.phone.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PlanStyle
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.TrainingAim
import app.levitx.core.load.WeightUnit
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.LevitxMotion
import app.levitx.phone.ui.QuietBar
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.StepHeader
import java.time.DayOfWeek
import kotlinx.coroutines.launch

/** The header's height, which the steps leave clear because the header sits over them. */
internal val HeaderHeight = 56.dp

/** The gap under the header on a step with no photo. */
private val PlainTitleGap = 14.dp

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
            onNext = { viewModel.next() },
            onBack = { viewModel.back() },
            onSkip = { viewModel.next(skip = true) },
            onChooseGoal = { viewModel.answer(SetupAnswer.Goal(it)) },
            onChooseStyle = { viewModel.answer(SetupAnswer.Style(it)) },
            onChooseAim = { viewModel.answer(SetupAnswer.Aim(it)) },
            onAnswerPullUp = { viewModel.answer(SetupAnswer.PullUp(it)) },
            onTypeBodyweight = { viewModel.answer(SetupAnswer.Bodyweight(it)) },
            onChooseUnit = viewModel::chooseUnit,
            onToggleDay = { viewModel.answer(SetupAnswer.Day(it)) },
            onChooseMinutes = { viewModel.answer(SetupAnswer.Minutes(it)) },
            onToggleEquipment = { viewModel.answer(SetupAnswer.Kit(it)) },
            onChooseNoEquipment = { viewModel.answer(SetupAnswer.Kit(null)) },
            onCountTest = { viewModel.answer(SetupAnswer.TestCount(it)) },
            onFinish = { scope.launch { if (viewModel.finish()) onFinished() } },
        ),
    )
}

/**
 * Everything the person can do during setup, grouped so the screen takes one parameter for them.
 *
 * @property onNext Moves to the next step.
 * @property onBack Goes back a step, or a movement in the quick test.
 * @property onSkip Leaves the step on screen unanswered: bodyweight, or the rest of the quick test.
 * @property onChooseGoal Picks a skill path by id, or general fitness with null.
 * @property onChooseStyle Picks all-round or focused, and moves on.
 * @property onChooseAim Picks what the training is tuned for, and moves on.
 * @property onAnswerPullUp Answers "Can you do a strict pull-up?", and moves on.
 * @property onTypeBodyweight Updates the bodyweight field.
 * @property onChooseUnit Switches between kilograms and pounds, for the whole app.
 * @property onToggleDay Adds or removes a training day.
 * @property onChooseMinutes Picks the time per session.
 * @property onToggleEquipment Adds or removes a piece of kit.
 * @property onChooseNoEquipment Says they have no kit.
 * @property onCountTest Sets the reps counted for the quick test movement on screen.
 * @property onFinish Saves the answers and starts training.
 */
data class OnboardingActions(
    val onNext: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onSkip: () -> Unit = {},
    val onChooseGoal: (String?) -> Unit = {},
    val onChooseStyle: (PlanStyle) -> Unit = {},
    val onChooseAim: (TrainingAim) -> Unit = {},
    val onAnswerPullUp: (PullUpAnswer) -> Unit = {},
    val onTypeBodyweight: (String) -> Unit = {},
    val onChooseUnit: (WeightUnit) -> Unit = {},
    val onToggleDay: (DayOfWeek) -> Unit = {},
    val onChooseMinutes: (Int) -> Unit = {},
    val onToggleEquipment: (Equipment) -> Unit = {},
    val onChooseNoEquipment: () -> Unit = {},
    val onCountTest: (Int) -> Unit = {},
    val onFinish: () -> Unit = {},
)

/**
 * Setup, as the onboarding artboards: the welcome over a full-screen photo,
 * then one question a screen. Each step has a photo behind its top, the
 * question as its title, and big answers; a step with one answer moves on
 * when it is tapped. A back arrow and a thin progress line stay in place
 * while the steps move under them.
 */
@Composable
fun OnboardingScreen(uiState: OnboardingUiState, actions: OnboardingActions, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LevitxColors.Background),
    ) {
        AnimatedContent(
            targetState = uiState.step == OnboardingStep.WELCOME,
            transitionSpec = { if (targetState) LevitxMotion.back() else LevitxMotion.forward() },
            label = "welcome",
        ) { welcome ->
            if (welcome) WelcomeScreen(onStart = actions.onNext) else Steps(uiState, actions)
        }
    }
}

/** The steps after the welcome: the step on screen, with the header fixed over it. */
@Composable
private fun Steps(uiState: OnboardingUiState, actions: OnboardingActions) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = uiState.step,
            // Later steps come in from the right, earlier ones from the left.
            transitionSpec = { if (targetState > initialState) LevitxMotion.forward() else LevitxMotion.back() },
            label = "step",
        ) { step ->
            Step(step, uiState, actions)
        }
        StepHeader(progress = uiState.progress, onBack = actions.onBack, modifier = Modifier.statusBarsPadding())
    }
}

/**
 * One step: its photo behind, its content scrolling over the photo, and its
 * actions pinned to the bottom.
 *
 * @param step The step to draw. While steps change, the one leaving is still
 *   drawn for a moment, so this isn't always the step in [uiState].
 */
@Composable
private fun Step(step: OnboardingStep, uiState: OnboardingUiState, actions: OnboardingActions) {
    val art = stepArt(step, uiState.goalPathId)
    Box(modifier = Modifier.fillMaxSize()) {
        if (art != null) StepPhoto(art)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                // Clear of the header, so what scrolls is cut off under it instead of running behind it.
                .padding(top = HeaderHeight),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 20.dp),
            ) {
                Spacer(Modifier.height(art?.titleGap ?: PlainTitleGap))
                StepContent(step, uiState, actions)
                if (uiState.unavailable) StepNote(stringResource(R.string.data_unavailable), warning = true)
            }
            StepBar(
                step,
                uiState,
                actions,
                modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, bottom = ScreenPadding),
            )
        }
    }
}

@Composable
private fun StepContent(step: OnboardingStep, uiState: OnboardingUiState, actions: OnboardingActions) {
    when (step) {
        OnboardingStep.GOAL -> GoalStep(uiState, actions)
        OnboardingStep.STYLE -> StyleStep(uiState, actions)
        OnboardingStep.AIM -> AimStep(uiState, actions)
        OnboardingStep.PULL_UP -> PullUpStep(uiState, actions)
        OnboardingStep.BODY -> BodyStep(uiState, actions)
        OnboardingStep.WEEK -> WeekStep(uiState, actions)
        OnboardingStep.KIT -> KitStep(uiState, actions)
        OnboardingStep.TEST -> QuickTestStep(uiState, actions)
        OnboardingStep.READY -> ReadyStep(uiState)
        OnboardingStep.WELCOME -> Unit
    }
}

/**
 * The actions pinned under a step. The steps answered with one tap have
 * none: tapping the answer is the action.
 */
@Composable
private fun StepBar(
    step: OnboardingStep,
    uiState: OnboardingUiState,
    actions: OnboardingActions,
    modifier: Modifier = Modifier,
) {
    // Only the step on screen can be acted on, not one that is on its way out.
    val live = step == uiState.step && !uiState.saving
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        when (step) {
            OnboardingStep.GOAL -> {
                GeneralGoalLink(
                    selected = uiState.goalChosen && uiState.goalPathId == null,
                    onClick = { actions.onChooseGoal(null) },
                )
                Continue(actions.onNext, enabled = live && uiState.canContinue)
            }

            OnboardingStep.BODY -> {
                QuietBar(stringResource(R.string.body_skip), onClick = actions.onSkip, enabled = live)
                Continue(actions.onNext, enabled = live && uiState.canContinue)
            }

            OnboardingStep.WEEK, OnboardingStep.KIT -> Continue(actions.onNext, enabled = live && uiState.canContinue)

            OnboardingStep.TEST -> QuickTestBar(uiState, actions)

            OnboardingStep.READY -> AccentBar(
                text = stringResource(R.string.ready_start),
                onClick = actions.onFinish,
                enabled = live,
            )

            OnboardingStep.WELCOME, OnboardingStep.STYLE, OnboardingStep.AIM, OnboardingStep.PULL_UP -> Unit
        }
    }
}

@Composable
private fun Continue(onClick: () -> Unit, enabled: Boolean) {
    AccentBar(text = stringResource(R.string.continue_button), onClick = onClick, enabled = enabled)
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun GoalStepPreview() {
    LevitxTheme {
        OnboardingScreen(
            OnboardingUiState(step = OnboardingStep.GOAL, goalChosen = true, goalPathId = "first_pull_up"),
            OnboardingActions(),
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun BodyStepPreview() {
    LevitxTheme {
        OnboardingScreen(OnboardingUiState(step = OnboardingStep.BODY, bodyweightText = "74.2"), OnboardingActions())
    }
}
