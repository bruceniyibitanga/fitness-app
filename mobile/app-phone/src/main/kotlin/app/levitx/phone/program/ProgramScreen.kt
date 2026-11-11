package app.levitx.phone.program

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.skills.Gate
import app.levitx.core.skills.GateMeasure
import app.levitx.core.skills.SkillCatalogue
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.BackButton
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.GateStatus
import app.levitx.phone.ui.Help
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.PhotoHeader
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.SquareNote
import app.levitx.phone.ui.gateStatus
import app.levitx.phone.ui.pathPhoto
import kotlinx.coroutines.launch

private val GateRowHeight = 54.dp
private val GateNumberWidth = 18.dp

/** How far the title block rides up over the photo's faded edge. */
private val TitleOverlap = (-28).dp

/**
 * A program's destination: connects [ProgramViewModel] to [ProgramScreen].
 *
 * @param pathId The skill path's id from the back stack.
 * @param onBack Leaves the screen.
 */
@Composable
fun ProgramRoute(
    pathId: String,
    onBack: () -> Unit,
    viewModel: ProgramViewModel = hiltViewModel<ProgramViewModel, ProgramViewModel.Factory>(
        creationCallback = { factory -> factory.create(pathId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    ProgramScreen(uiState = uiState, onMakePlan = { scope.launch { viewModel.makePlan() } }, onBack = onBack)
}

/**
 * A skill program, laid out as the "Program detail" artboard: its photo, its
 * name, the gates to pass with their targets, and Make this my plan.
 */
@Composable
fun ProgramScreen(uiState: ProgramUiState, onMakePlan: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().background(LevitxColors.Background).navigationBarsPadding()) {
        when (uiState) {
            ProgramUiState.Loading -> Unit

            ProgramUiState.Unavailable -> Message(stringResource(R.string.data_unavailable), onBack)

            ProgramUiState.NotFound -> Message(stringResource(R.string.program_not_found), onBack)

            is ProgramUiState.Ready -> {
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    PhotoHeader(pathPhoto(uiState.path.id), onBack)
                    Title(uiState)
                    Gates(uiState)
                }
                Footer(uiState, onMakePlan)
            }
        }
    }
}

/** A sentence, with back. */
@Composable
private fun Message(text: String, onBack: () -> Unit) {
    BackButton(onBack, modifier = Modifier.statusBarsPadding().padding(4.dp))
    Lede(text, modifier = Modifier.padding(ScreenPadding))
}

/** The tag, the name, progress so far and how gates work. */
@Composable
private fun Title(state: ProgramUiState.Ready) {
    val gates = state.path.gates.size
    val passed = state.currentIndex ?: gates
    Column(
        modifier = Modifier
            .offset(y = TitleOverlap)
            .padding(horizontal = ScreenPadding),
    ) {
        Text(
            text = stringResource(R.string.program_label),
            style = LevitxType.Micro,
            color = LevitxColors.Background,
            modifier = Modifier.background(LevitxColors.Accent).padding(horizontal = 6.dp, vertical = 3.dp),
        )
        Text(
            text = state.path.name,
            style = LevitxType.Poster.copy(fontSize = 46.sp, lineHeight = 44.sp),
            color = LevitxColors.Text,
            modifier = Modifier.padding(top = 10.dp).semantics { heading() },
        )
        Caption(
            pluralStringResource(R.plurals.train_program_gates, gates, gates, passed),
            modifier = Modifier.padding(top = 10.dp),
        )
        Lede(stringResource(R.string.program_body), modifier = Modifier.padding(top = 10.dp))
    }
}

/** "GATES · TO PASS" and a numbered row for each, the one being worked on in orange. */
@Composable
private fun Gates(state: ProgramUiState.Ready) {
    Rule()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(horizontal = ScreenPadding),
    ) {
        Caption(stringResource(R.string.program_gates), modifier = Modifier.weight(1f))
        Caption(stringResource(R.string.program_to_pass), color = LevitxColors.TextFaint)
    }
    Rule()
    state.path.gates.forEachIndexed { index, gate ->
        val status = gateStatus(index, state.currentIndex)
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().heightIn(min = GateRowHeight).padding(horizontal = ScreenPadding),
        ) {
            Text(
                text = (index + 1).toString().padStart(2, '0'),
                style = LevitxType.Caption,
                color = if (status == GateStatus.CURRENT) LevitxColors.Accent else LevitxColors.TextFaint,
                modifier = Modifier.width(GateNumberWidth),
            )
            Text(
                text = gate.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (status == GateStatus.PASSED) LevitxColors.TextMuted else LevitxColors.Text,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            )
            Text(
                text = target(gate),
                style = LevitxType.Figure.copy(fontSize = 15.sp),
                color = if (index == state.path.gates.lastIndex) LevitxColors.Accent else LevitxColors.Text,
            )
        }
        Rule()
    }
}

/** "×10" or "30 s". */
@Composable
private fun target(gate: Gate): String = when (gate.measure) {
    GateMeasure.REPS -> stringResource(R.string.gate_reps_target, gate.target)
    GateMeasure.HOLD_SECONDS -> stringResource(R.string.seconds_short, gate.target)
}

/** Make this my plan, with what changes; or a note that it already is. */
@Composable
private fun Footer(state: ProgramUiState.Ready, onMakePlan: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 12.dp, bottom = 20.dp),
    ) {
        if (state.isPlan) {
            SquareNote(title = null, text = stringResource(R.string.program_is_plan))
        } else {
            Help(stringResource(R.string.program_switch_note))
            AccentBar(text = stringResource(R.string.program_make_plan), onClick = onMakePlan)
        }
    }
}

@Preview
@Composable
private fun ProgramScreenPreview() {
    val path = SkillCatalogue.muscleUp
    LevitxTheme {
        ProgramScreen(
            uiState = ProgramUiState.Ready(path, mapOf(path.gates[0].id to 30), currentIndex = 1, isPlan = false),
            onMakePlan = {},
            onBack = {},
        )
    }
}
