package app.levitx.phone.pain

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.coach.BodyArea
import app.levitx.core.pain.RedFlag
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.BackHeader
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.QuietBar
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.SecureWindow

/** Height of the buttons pinned to the bottom, as on the workout screens. */
private val BottomBarHeight = 64.dp

/**
 * Everything the person can do in a pain check.
 *
 * @property onChooseArea Picks where it hurts.
 * @property onToggleSign Switches a warning sign on or off.
 * @property onChooseRating Sets pain at its worst, 0 to 10.
 * @property onNext The step's main button.
 * @property onBack Goes back a step, or leaves from the first.
 * @property onClose Leaves without saving.
 */
data class PainCheckActions(
    val onChooseArea: (BodyArea) -> Unit,
    val onToggleSign: (RedFlag) -> Unit,
    val onChooseRating: (Int) -> Unit,
    val onNext: () -> Unit,
    val onBack: () -> Unit,
    val onClose: () -> Unit,
) {
    /** Ready-made actions. */
    companion object {
        /** Actions that do nothing, for previews and screenshots. */
        val None = PainCheckActions({}, {}, {}, {}, {}, {})
    }
}

/**
 * The pain check's destination: connects [PainCheckViewModel] to
 * [PainCheckScreen], closes once the answer is saved, and keeps the screen out
 * of screenshots and the recent apps list while it is open.
 *
 * @param onClose Leaves the check.
 */
@Composable
fun PainCheckRoute(onClose: () -> Unit, viewModel: PainCheckViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val ready = uiState as? PainCheckUiState.Ready
    val close by rememberUpdatedState(onClose)
    val saved = ready?.saved == true
    LaunchedEffect(saved) { if (saved) close() }
    // Back steps through the check before it leaves.
    BackHandler(enabled = ready != null && ready.step != PainStep.AREA) { viewModel.back() }
    SecureWindow()
    PainCheckScreen(
        uiState = uiState,
        actions = PainCheckActions(
            onChooseArea = viewModel::chooseArea,
            onToggleSign = viewModel::toggleSign,
            onChooseRating = viewModel::chooseRating,
            onNext = viewModel::next,
            onBack = { if (!viewModel.back()) onClose() },
            onClose = onClose,
        ),
    )
}

/**
 * The pain check, laid out as the "Warning-sign screen" and "Pain check-in"
 * artboards, with a step before them to pick the area: where it hurts, a few
 * checks that mean a professional should look first, then how bad it is.
 */
@Composable
fun PainCheckScreen(uiState: PainCheckUiState, actions: PainCheckActions, modifier: Modifier = Modifier) {
    val ready = uiState as? PainCheckUiState.Ready
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LevitxColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        BackHeader(stringResource(R.string.pain_title), actions.onBack) {
            if (ready != null) {
                Caption(
                    text = stringResource(R.string.pain_step, ready.step.ordinal + 1, PainStep.entries.size),
                    color = LevitxColors.Ice,
                )
            }
        }
        when (uiState) {
            PainCheckUiState.Loading -> Unit

            PainCheckUiState.Unavailable ->
                Lede(stringResource(R.string.data_unavailable), Modifier.padding(ScreenPadding))

            is PainCheckUiState.Ready -> Step(uiState, actions)
        }
    }
}

/** The step on screen, with its buttons pinned to the bottom. */
@Composable
private fun ColumnScope.Step(state: PainCheckUiState.Ready, actions: PainCheckActions) {
    when (state.step) {
        PainStep.AREA -> AreaStep(state.sore, actions.onChooseArea)

        PainStep.SIGNS -> {
            SignsStep(state, actions.onToggleSign)
            StepButtons(
                text = stringResource(if (state.signs.isEmpty()) R.string.continue_button else R.string.pain_pause),
                enabled = state.canContinue,
                actions = actions,
            )
        }

        PainStep.RATING -> {
            RatingStep(state, actions.onChooseRating)
            StepButtons(stringResource(R.string.pain_save), state.canContinue, actions)
        }
    }
}

/** The main button, two thirds wide, beside Not now. */
@Composable
private fun StepButtons(text: String, enabled: Boolean, actions: PainCheckActions) {
    Row(modifier = Modifier.height(BottomBarHeight)) {
        AccentBar(
            text = text,
            onClick = actions.onNext,
            enabled = enabled,
            height = BottomBarHeight,
            modifier = Modifier.weight(2f),
        )
        QuietBar(
            text = stringResource(R.string.pain_not_now),
            onClick = actions.onClose,
            modifier = Modifier
                .weight(1f)
                .height(BottomBarHeight),
        )
    }
}
