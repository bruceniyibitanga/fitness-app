package app.levitx.phone

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.WorkoutFocus
import app.levitx.core.skills.SkillCatalogue
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.train.AllProgramsScreen
import app.levitx.phone.train.ProgramSummary
import app.levitx.phone.train.SearchScreen
import app.levitx.phone.train.TrainNavigation
import app.levitx.phone.train.TrainUiState
import app.levitx.phone.train.WorkoutSummary
import com.android.tools.screenshot.PreviewTest

/*
 * Search and the list of every program, rendered by CI to check against the
 * "Search" and "All programs" artboards.
 */

/** Goes nowhere. */
val noTrainNavigation = TrainNavigation({}, {}, {}, {}, {}, {})

/** Someone with a bar, working toward a first pull-up. */
private fun trainState(): TrainUiState.Ready {
    val model = AthleteModel.starting(
        CoachProfile("first_pull_up", equipment = setOf(Equipment.PULL_UP_BAR)),
        PullUpAnswer.NOT_YET,
    )
    val programs = SkillCatalogue.all.map { ProgramSummary(it, passed = 0, isCurrent = it.id == "first_pull_up") }
    val workouts = WorkoutFocus.entries.map { WorkoutSummary(it, Coach().workout(model, it, 0)) }
    return TrainUiState.Ready(programs, workouts, owned = model.profile.equipment)
}

@PreviewTest
@Preview(name = "38 Search", widthDp = 390, heightDp = 1400)
@Composable
fun SearchScreenshot() {
    LevitxTheme { SearchScreen(trainState(), noTrainNavigation, onBack = {}, initialQuery = "pull") }
}

@PreviewTest
@Preview(name = "39 Search, nothing found", widthDp = 390, heightDp = 844)
@Composable
fun SearchNothingScreenshot() {
    LevitxTheme { SearchScreen(trainState(), noTrainNavigation, onBack = {}, initialQuery = "kettlebell") }
}

@PreviewTest
@Preview(name = "40 All programs", widthDp = 390, heightDp = 1500)
@Composable
fun AllProgramsScreenshot() {
    LevitxTheme { AllProgramsScreen(trainState(), onBack = {}, onOpenProgram = {}) }
}
