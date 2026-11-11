package app.levitx.phone.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.session.SessionPlan
import app.levitx.core.session.TrainingDay
import app.levitx.core.session.trainingDay
import app.levitx.core.skills.SkillCatalogue
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxSpacing
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.FlatProgressBar
import app.levitx.phone.ui.PrimaryButton
import app.levitx.phone.ui.SectionLabel
import app.levitx.phone.ui.durationText
import app.levitx.phone.ui.measureText

/**
 * Home destination: connects [HomeViewModel] to [HomeScreen].
 *
 * Kept separate from [HomeScreen] so the screen itself takes plain state and
 * can be previewed and tested without a ViewModel.
 */
@Composable
fun HomeRoute(
    onStartSession: () -> Unit,
    onViewPath: () -> Unit,
    onChooseGoal: () -> Unit,
    onOpenEquipment: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    // Stops collecting while the app is in the background.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(uiState, onStartSession, onViewPath, onChooseGoal, onOpenEquipment)
}

/** The home screen: the goal, the gate in front of the person, and today's session. */
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onStartSession: () -> Unit,
    onViewPath: () -> Unit,
    onChooseGoal: () -> Unit,
    onOpenEquipment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(LevitxSpacing.Medium),
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LevitxSpacing.Medium, vertical = LevitxSpacing.XLarge),
        ) {
            when (uiState) {
                // Progress is local, so loading takes milliseconds. Showing a
                // spinner for that long would only flicker, so show nothing.
                HomeUiState.Loading -> Unit

                HomeUiState.Unavailable -> Text(
                    text = stringResource(R.string.data_unavailable),
                    style = MaterialTheme.typography.bodyLarge,
                )

                HomeUiState.NoGoal -> PrimaryButton(stringResource(R.string.home_choose_goal), onChooseGoal)

                is HomeUiState.Ready -> {
                    TrainingDayContent(uiState.day, onStartSession, onViewPath, onChooseGoal)
                    if (!uiState.hasBodyweight) BodyweightPrompt(onOpenEquipment)
                    TextButton(onClick = onOpenEquipment) { Text(stringResource(R.string.home_equipment)) }
                }
            }
        }
    }
}

@Composable
private fun TrainingDayContent(
    day: TrainingDay,
    onStartSession: () -> Unit,
    onViewPath: () -> Unit,
    onChooseGoal: () -> Unit,
) {
    SectionLabel(stringResource(R.string.home_path_label))
    Text(text = day.path.name, style = LevitxType.Display)

    val gate = day.currentGate
    val plan = day.plan
    if (gate == null || plan == null) {
        Text(stringResource(R.string.home_path_complete), style = MaterialTheme.typography.titleLarge)
        PrimaryButton(stringResource(R.string.home_choose_next_goal), onChooseGoal)
        return
    }

    Spacer(Modifier.height(LevitxSpacing.Small))
    SectionLabel(
        text = stringResource(R.string.home_gate_label, day.gateNumber, day.path.gates.size),
        color = LevitxColors.Accent,
    )
    Text(text = gate.title, style = MaterialTheme.typography.headlineSmall)
    Text(text = gate.trains, style = MaterialTheme.typography.bodyLarge, color = LevitxColors.TextSoft)
    FlatProgressBar(fraction = day.progress.currentFraction.toFloat())
    Text(
        text = stringResource(
            R.string.home_best,
            measureText(gate.measure, day.currentBest),
            measureText(gate.measure, gate.target),
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = LevitxColors.TextMuted,
    )

    Spacer(Modifier.height(LevitxSpacing.Small))
    TodayCard(plan)
    PrimaryButton(stringResource(R.string.home_start_session), onStartSession)
    TextButton(onClick = onViewPath) { Text(stringResource(R.string.home_view_path)) }
}

/** Asks for bodyweight, which every load-per-rep figure needs. */
@Composable
private fun BodyweightPrompt(onOpenEquipment: () -> Unit) {
    Surface(
        onClick = onOpenEquipment,
        color = LevitxColors.Surface,
        border = BorderStroke(Dp.Hairline, LevitxColors.Ice),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(LevitxSpacing.XSmall),
            modifier = Modifier.padding(LevitxSpacing.Medium),
        ) {
            SectionLabel(stringResource(R.string.home_bodyweight_label), color = LevitxColors.Ice)
            Text(stringResource(R.string.home_bodyweight_prompt), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/** Today's session in two lines: the work sets, then the best effort. */
@Composable
private fun TodayCard(plan: SessionPlan) {
    val work = plan.sets.filterNot { it.isBestEffort }
    val bestEffort = plan.sets.last { it.isBestEffort }
    Surface(
        color = LevitxColors.Surface,
        border = BorderStroke(Dp.Hairline, LevitxColors.Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(LevitxSpacing.XSmall),
            modifier = Modifier.padding(LevitxSpacing.Medium),
        ) {
            SectionLabel(stringResource(R.string.home_today_label))
            Text(
                text = pluralStringResource(
                    R.plurals.home_today_work,
                    work.size,
                    work.size,
                    measureText(plan.gate.measure, work.first().target),
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    R.string.home_today_best_effort,
                    measureText(plan.gate.measure, bestEffort.target),
                ),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.home_today_rest, durationText(plan.restSeconds)),
                style = MaterialTheme.typography.bodyMedium,
                color = LevitxColors.TextMuted,
            )
        }
    }
}

@Preview
@Composable
private fun HomeScreenPreview() {
    val path = SkillCatalogue.muscleUp
    val bests = mapOf(path.gates[0].id to 30, path.gates[1].id to 10, path.gates[2].id to 7)
    LevitxTheme {
        HomeScreen(
            uiState = HomeUiState.Ready(path.trainingDay(bests)),
            onStartSession = {},
            onViewPath = {},
            onChooseGoal = {},
            onOpenEquipment = {},
        )
    }
}
