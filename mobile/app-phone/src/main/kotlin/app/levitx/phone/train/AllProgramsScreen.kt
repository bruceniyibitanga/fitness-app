package app.levitx.phone.train

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.skills.SkillLevel
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.BackHeader
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.DataUnavailable
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.levelName

/**
 * Every program's destination. Reuses [TrainViewModel] for the programs and
 * the person's progress through them.
 *
 * @param onOpenProgram Opens a program, by id.
 */
@Composable
fun AllProgramsRoute(
    onBack: () -> Unit,
    onOpenProgram: (String) -> Unit,
    viewModel: TrainViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AllProgramsScreen(uiState, onBack, onOpenProgram)
}

/**
 * Every skill program, laid out as the "All programs" artboard: a section
 * per level, easiest first, each with a count and a row per program.
 */
@Composable
fun AllProgramsScreen(
    uiState: TrainUiState,
    onBack: () -> Unit,
    onOpenProgram: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LevitxColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        BackHeader(stringResource(R.string.train_all_programs), onBack)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            when (uiState) {
                TrainUiState.Loading -> Unit

                TrainUiState.Unavailable -> DataUnavailable()

                is TrainUiState.Ready -> SkillLevel.entries.forEach { level ->
                    val programs = uiState.programs.filter { it.path.level == level }
                    if (programs.isNotEmpty()) {
                        Caption(
                            stringResource(
                                R.string.train_search_section,
                                stringResource(levelName(level)),
                                programs.size,
                            ),
                            modifier = Modifier.padding(start = ScreenPadding, top = 20.dp, bottom = 4.dp),
                        )
                        programs.forEach { program ->
                            ProgramRow(program, onClick = { onOpenProgram(program.path.id) })
                            Rule()
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun AllProgramsScreenPreview() {
    val model = AthleteModel.starting(
        CoachProfile("first_pull_up", equipment = setOf(Equipment.PULL_UP_BAR)),
        PullUpAnswer.NOT_YET,
    )
    LevitxTheme {
        AllProgramsScreen(TrainUiState.Ready(programsFor(model), emptyList()), onBack = {}, onOpenProgram = {})
    }
}
