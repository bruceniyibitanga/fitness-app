package app.levitx.phone.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PlanStyle
import app.levitx.core.coach.PullUpAnswer
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Help
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.ScreenTitle
import app.levitx.phone.ui.StatStrip
import app.levitx.phone.ui.StepHeader
import app.levitx.phone.ui.Wordmark
import java.time.DayOfWeek
import kotlinx.coroutines.launch

/** Steps after the welcome, which the "2 / 4" counter counts. */
private val CountedSteps = OnboardingStep.entries.drop(1)

/** How much of the welcome screen the photo covers, as on the artboard (560 of 844). */
private const val WELCOME_PHOTO_FRACTION = 0.66f

/** Where the photo starts fading into the background, as a fraction of its height. */
private const val PHOTO_FADE_START = 0.5f

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
            onToggleEquipment = { viewModel.chooseEquipment(it) },
            onChooseNoEquipment = { viewModel.chooseEquipment(null) },
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
 * @property onChooseNoEquipment Says they have no kit.
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
    val onChooseNoEquipment: () -> Unit,
    val onFinish: () -> Unit,
)

/**
 * Setup, one step at a time, laid out as the onboarding artboards: the
 * welcome over a photo, then each step under a back arrow, a "2 / 4" counter
 * and a progress line, with the main action pinned to the bottom.
 */
@Composable
fun OnboardingScreen(uiState: OnboardingUiState, actions: OnboardingActions, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LevitxColors.Background),
    ) {
        if (uiState.step == OnboardingStep.WELCOME) {
            Welcome(onStart = actions.onNext)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                StepHeader(
                    index = CountedSteps.indexOf(uiState.step) + 1,
                    count = CountedSteps.size,
                    onBack = actions.onBack,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 22.dp, bottom = 20.dp),
                ) {
                    when (uiState.step) {
                        OnboardingStep.GOAL -> GoalStep(uiState, actions)
                        OnboardingStep.HOW -> HowStep(uiState, actions)
                        OnboardingStep.WEEK -> WeekStep(uiState, actions)
                        OnboardingStep.READY -> ReadyStep(uiState)
                        OnboardingStep.WELCOME -> Unit
                    }
                    if (uiState.unavailable) {
                        Text(
                            text = stringResource(R.string.data_unavailable),
                            color = LevitxColors.Accent,
                            modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 12.dp),
                        )
                    }
                }
                AccentBar(
                    text = stringResource(
                        when (uiState.step) {
                            OnboardingStep.WEEK -> R.string.week_build
                            OnboardingStep.READY -> R.string.ready_start
                            else -> R.string.continue_button
                        },
                    ),
                    onClick = if (uiState.step == OnboardingStep.READY) actions.onFinish else actions.onNext,
                    enabled = uiState.canContinue && !uiState.saving,
                    modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, bottom = ScreenPadding),
                )
            }
        }
    }
}

/** The first screen: a photo, the promise, three facts and "Build my plan". */
@Composable
private fun Welcome(onStart: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(WELCOME_PHOTO_FRACTION),
        ) {
            Image(
                painter = painterResource(R.drawable.photo_welcome),
                contentDescription = stringResource(R.string.welcome_photo_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            PHOTO_FADE_START to Color.Transparent,
                            1f to LevitxColors.Background,
                        ),
                    ),
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Wordmark(
                modifier = Modifier
                    .height(56.dp)
                    .padding(horizontal = ScreenPadding),
            )
            Spacer(Modifier.weight(1f))
            WelcomeText()
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, bottom = ScreenPadding),
            ) {
                AccentBar(
                    text = stringResource(R.string.welcome_start),
                    hint = stringResource(R.string.welcome_minutes),
                    onClick = onStart,
                )
                Text(
                    text = stringResource(R.string.welcome_private),
                    fontSize = 12.sp,
                    color = LevitxColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.WelcomeText() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, bottom = 28.dp),
    ) {
        Caption(stringResource(R.string.welcome_kicker), color = LevitxColors.Accent)
        Text(
            text = stringResource(R.string.welcome_title),
            style = LevitxType.Poster,
            color = LevitxColors.Text,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = stringResource(R.string.welcome_body),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = LevitxColors.TextMuted,
        )
        Column {
            Rule()
            StatStrip(
                listOf(
                    stringResource(R.string.welcome_fact_kg) to stringResource(R.string.welcome_fact_kg_caption),
                    stringResource(R.string.welcome_fact_gates) to stringResource(R.string.welcome_fact_gates_caption),
                    stringResource(R.string.welcome_fact_watch) to stringResource(R.string.welcome_fact_watch_caption),
                ),
            )
            Rule()
        }
    }
}

/** A step's title and the line under it, padded to the screen edges. */
@Composable
internal fun StepTitle(title: String, lede: String? = null) {
    Column(modifier = Modifier.padding(horizontal = ScreenPadding)) {
        ScreenTitle(title)
        if (lede != null) Lede(lede, modifier = Modifier.padding(top = 8.dp))
    }
}

/** A step's group of controls: a sentence-case question and what goes under it. */
@Composable
internal fun Question(
    text: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    help: String? = null,
    asCaption: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = ScreenPadding, end = ScreenPadding, top = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            if (asCaption) {
                Caption(text, modifier = Modifier.weight(1f))
            } else {
                Text(
                    text = text,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LevitxColors.Text,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
            }
            if (trailing != null) Caption(trailing, color = LevitxColors.Accent)
        }
        content()
        if (help != null) Help(help)
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun WelcomePreview() {
    LevitxTheme { OnboardingScreen(OnboardingUiState(), PreviewActions) }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun GoalStepPreview() {
    LevitxTheme {
        OnboardingScreen(
            OnboardingUiState(step = OnboardingStep.GOAL, goalChosen = true, goalPathId = "first_pull_up"),
            PreviewActions,
        )
    }
}

/** Does nothing; for previews. */
internal val PreviewActions = OnboardingActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
