package app.levitx.phone.skills

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PullUpAnswer
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxSpacing
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.FlatProgressBar
import app.levitx.phone.ui.SectionLabel
import app.levitx.phone.ui.measureText
import app.levitx.phone.ui.patternName

/**
 * Skills tab: connects [SkillsViewModel] to [SkillsScreen].
 *
 * @param onOpenExercise Opens an exercise's detail, by id.
 */
@Composable
fun SkillsRoute(onOpenExercise: (String) -> Unit, viewModel: SkillsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SkillsScreen(uiState, onOpenExercise)
}

/** Every movement's ladder: the rung the person is on, what they train there and what comes next. */
@Composable
fun SkillsScreen(uiState: SkillsUiState, onOpenExercise: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = LevitxSpacing.Medium)) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(LevitxSpacing.Small),
                modifier = Modifier.padding(vertical = LevitxSpacing.Large),
            ) {
                SectionLabel(stringResource(R.string.skills_label), color = LevitxColors.Accent)
                Text(stringResource(R.string.skills_title), style = LevitxType.Display)
                Text(
                    text = stringResource(R.string.skills_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = LevitxColors.TextSoft,
                )
            }
        }
        when (uiState) {
            SkillsUiState.Loading -> Unit

            SkillsUiState.Unavailable -> item { Text(stringResource(R.string.data_unavailable)) }

            is SkillsUiState.Ready -> items(uiState.ladders, key = { it.pattern.name }) { standing ->
                HorizontalDivider(color = LevitxColors.Line)
                LadderRow(standing, onOpenExercise)
            }
        }
    }
}

@Composable
private fun LadderRow(standing: LadderStanding, onOpenExercise: (String) -> Unit) {
    // Only a row with an exercise of its own opens anything.
    val openable = standing.exercise?.takeUnless { standing.isTrainedByGate }
    val openLabel = openable?.let { stringResource(R.string.open_exercise, it.name) }
    Column(
        verticalArrangement = Arrangement.spacedBy(LevitxSpacing.XSmall),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (openable == null) {
                    Modifier
                } else {
                    Modifier.clickable(onClickLabel = openLabel, role = Role.Button) { onOpenExercise(openable.id) }
                },
            )
            .padding(vertical = LevitxSpacing.Medium),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            SectionLabel(patternName(standing.pattern), modifier = Modifier.weight(1f))
            SectionLabel(stringResource(R.string.skills_rung, standing.rung + 1, standing.rungCount))
        }
        FlatProgressBar(fraction = (standing.rung + 1).toFloat() / standing.rungCount)
        // The gate's own exercise trains this movement for now, so the ladder would only confuse.
        if (standing.isTrainedByGate) {
            Text(
                text = stringResource(R.string.skills_gate),
                style = MaterialTheme.typography.bodyLarge,
                color = LevitxColors.Ice,
            )
            return@Column
        }
        val exercise = standing.exercise
        if (exercise == null) {
            Text(
                text = stringResource(R.string.skills_no_kit),
                style = MaterialTheme.typography.bodyLarge,
                color = LevitxColors.TextMuted,
            )
            return@Column
        }
        Text(exercise.name, style = MaterialTheme.typography.titleMedium)
        standing.target?.let { target ->
            Text(
                text = stringResource(R.string.skills_target, measureText(exercise.measure, target)),
                style = MaterialTheme.typography.bodyMedium,
                color = LevitxColors.TextSoft,
            )
        }
        Text(
            text = standing.next?.let { stringResource(R.string.skills_next, it.name) }
                ?: stringResource(R.string.skills_top),
            style = MaterialTheme.typography.bodyMedium,
            color = LevitxColors.TextMuted,
        )
    }
}

@Preview
@Composable
private fun SkillsScreenPreview() {
    val profile = CoachProfile("first_pull_up", equipment = setOf(Equipment.PULL_UP_BAR))
    LevitxTheme {
        SkillsScreen(
            SkillsUiState.Ready(
                standings(AthleteModel.starting(profile, PullUpAnswer.NOT_YET)),
            ),
            onOpenExercise = {
            },
        )
    }
}
