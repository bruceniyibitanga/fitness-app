package app.levitx.phone.goal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.skills.SkillCatalogue
import app.levitx.core.skills.SkillPath
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxSpacing
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.SectionLabel

/** Goal picker destination: connects [GoalPickerViewModel] to [GoalPickerScreen]. */
@Composable
fun GoalPickerRoute(onGoalChosen: () -> Unit, viewModel: GoalPickerViewModel = hiltViewModel()) {
    val goalSaved by viewModel.goalSaved.collectAsStateWithLifecycle()
    LaunchedEffect(goalSaved) {
        if (goalSaved) {
            viewModel.onNavigated()
            onGoalChosen()
        }
    }
    GoalPickerScreen(goals = viewModel.goals, onChoose = viewModel::choose)
}

/** Lists the goals as large cards. Tapping one chooses it. */
@Composable
fun GoalPickerScreen(goals: List<SkillPath>, onChoose: (SkillPath) -> Unit, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(LevitxSpacing.Medium),
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LevitxSpacing.Medium, vertical = LevitxSpacing.XLarge),
        ) {
            SectionLabel(stringResource(R.string.goal_label))
            Text(stringResource(R.string.goal_title), style = LevitxType.Display)
            Text(
                text = stringResource(R.string.goal_body),
                style = MaterialTheme.typography.bodyLarge,
                color = LevitxColors.TextSoft,
            )
            goals.forEach { path -> GoalCard(path = path, onClick = { onChoose(path) }) }
            Text(
                text = stringResource(R.string.goal_more_coming),
                style = MaterialTheme.typography.bodyMedium,
                color = LevitxColors.TextMuted,
            )
        }
    }
}

@Composable
private fun GoalCard(path: SkillPath, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = LevitxColors.Surface,
        border = BorderStroke(width = Dp.Hairline, color = LevitxColors.Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(LevitxSpacing.Medium)) {
            Text(text = path.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = pluralStringResource(R.plurals.goal_card_detail, path.gates.size, path.gates.size, path.gates.first().title),
                style = MaterialTheme.typography.bodyMedium,
                color = LevitxColors.TextMuted,
                modifier = Modifier.padding(top = LevitxSpacing.XSmall),
            )
        }
    }
}

@Preview
@Composable
private fun GoalPickerScreenPreview() {
    LevitxTheme { GoalPickerScreen(goals = SkillCatalogue.all, onChoose = {}) }
}
