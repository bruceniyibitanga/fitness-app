package app.levitx.phone

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.WorkoutFocus
import app.levitx.core.coach.WorkoutRun
import app.levitx.core.skills.SkillCatalogue
import app.levitx.phone.main.MainScreen
import app.levitx.phone.main.MainTab
import app.levitx.phone.onboarding.OnboardingActions
import app.levitx.phone.onboarding.OnboardingScreen
import app.levitx.phone.onboarding.OnboardingStep
import app.levitx.phone.onboarding.OnboardingUiState
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.today.TodayActions
import app.levitx.phone.today.TodayScreen
import app.levitx.phone.today.TodayUiState
import app.levitx.phone.train.ProgramSummary
import app.levitx.phone.train.TrainScreen
import app.levitx.phone.train.TrainUiState
import app.levitx.phone.train.WorkoutSummary
import app.levitx.phone.workout.WorkoutActions
import app.levitx.phone.workout.WorkoutScreen
import app.levitx.phone.workout.WorkoutUiState
import com.android.tools.screenshot.PreviewTest

/*
 * Every screen, rendered at the design's 390 × 844 artboard size, so CI can
 * turn each into an image to compare with its artboard in "Levitx App Design".
 * The data is what a real person would see, worked out by the real coach.
 */

private val noOnboardingActions = OnboardingActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
private val noTodayActions =
    TodayActions(onStart = {}, onRest = {}, onAcknowledgeWeightNotice = {}, onOpenExercise = {})

/** Someone after a muscle-up who can do a pull-up and has a bar. */
private fun muscleUp(kit: Set<Equipment> = setOf(Equipment.PULL_UP_BAR)) =
    AthleteModel.starting(CoachProfile("muscle_up", equipment = kit), PullUpAnswer.YES)

private val noWorkoutActions = WorkoutActions({ _, _, _, _ -> }, {}, {}, {}, {})

/** The first session the coach gives [muscleUp], about to start. */
private fun firstRun(): WorkoutRun =
    WorkoutRun("preview", checkNotNull(Coach().recommend(muscleUp(), 0).session), startedAtEpochMillis = 0)

private fun onboarding(state: OnboardingUiState): @Composable () -> Unit = {
    LevitxTheme { OnboardingScreen(state, noOnboardingActions) }
}

@PreviewTest
@Preview(name = "01 Welcome", widthDp = 390, heightDp = 844)
@Composable
fun WelcomeScreenshot() = onboarding(OnboardingUiState())()

@PreviewTest
@Preview(name = "02 Goal", widthDp = 390, heightDp = 844)
@Composable
fun GoalScreenshot() =
    onboarding(OnboardingUiState(step = OnboardingStep.GOAL, goalChosen = true, goalPathId = "first_pull_up"))()

@PreviewTest
@Preview(name = "03 How", widthDp = 390, heightDp = 844)
@Composable
fun HowScreenshot() = onboarding(
    OnboardingUiState(
        step = OnboardingStep.HOW,
        goalChosen = true,
        goalPathId = "first_pull_up",
        pullUp = PullUpAnswer.NOT_YET,
    ),
)()

@PreviewTest
@Preview(name = "04 Week, no kit", widthDp = 390, heightDp = 844)
@Composable
fun WeekScreenshot() = onboarding(
    OnboardingUiState(step = OnboardingStep.WEEK, goalChosen = true, goalPathId = "muscle_up"),
)()

@PreviewTest
@Preview(name = "05 Ready", widthDp = 390, heightDp = 1200)
@Composable
fun ReadyScreenshot() {
    val model = muscleUp()
    onboarding(
        OnboardingUiState(
            step = OnboardingStep.READY,
            goalChosen = true,
            goalPathId = "muscle_up",
            pullUp = PullUpAnswer.YES,
            bodyweightText = "75.6",
            equipment = model.profile.equipment,
            firstSession = Coach().recommend(model, 0).session,
        ),
    )()
}

@PreviewTest
@Preview(name = "06 Today", widthDp = 390, heightDp = 844)
@Composable
fun TodayScreenshot() {
    LevitxTheme {
        MainScreen(selected = MainTab.TODAY, onSelect = {}) {
            TodayScreen(
                uiState = TodayUiState.Ready(
                    nowEpochMillis = 0,
                    recommendation = Coach().recommend(muscleUp(), 0),
                    bodyweightKg = 75.6,
                    hasWorkoutInProgress = false,
                    sessionsLastWeek = 1,
                    sessionsPerWeek = 3,
                ),
                actions = noTodayActions,
            )
        }
    }
}

@PreviewTest
@Preview(name = "07 Today, no kit", widthDp = 390, heightDp = 844)
@Composable
fun TodayNoKitScreenshot() {
    LevitxTheme {
        MainScreen(selected = MainTab.TODAY, onSelect = {}) {
            TodayScreen(
                uiState = TodayUiState.Ready(
                    nowEpochMillis = 0,
                    recommendation = Coach().recommend(muscleUp(kit = emptySet()), 0),
                    bodyweightKg = null,
                    hasWorkoutInProgress = false,
                    sessionsPerWeek = 3,
                ),
                actions = noTodayActions,
            )
        }
    }
}

@PreviewTest
@Preview(name = "08 Train", widthDp = 390, heightDp = 844)
@Composable
fun TrainScreenshot() {
    val model = muscleUp()
    val programs = SkillCatalogue.all.map { path ->
        ProgramSummary(path, passed = if (path.id == "muscle_up") 1 else 0, isCurrent = path.id == "muscle_up")
    }
    val workouts = WorkoutFocus.entries.map { WorkoutSummary(it, Coach().workout(model, it, 0)) }
    LevitxTheme {
        MainScreen(selected = MainTab.TRAIN, onSelect = {}) {
            TrainScreen(TrainUiState.Ready(programs, workouts), onOpenWorkout = {}, onOpenProgram = {})
        }
    }
}

@PreviewTest
@Preview(name = "09 Live set", widthDp = 390, heightDp = 844)
@Composable
fun LiveSetScreenshot() {
    LevitxTheme {
        WorkoutScreen(
            uiState = WorkoutUiState.Active(firstRun(), bodyweightKg = 75.6, bands = emptyList()),
            now = { 0L },
            actions = noWorkoutActions,
        )
    }
}

@PreviewTest
@Preview(name = "10 Rest", widthDp = 390, heightDp = 844)
@Composable
fun RestScreenshot() {
    val run = firstRun()
    val resting = run.logSet(checkNotNull(run.nextSet).target, repsInReserve = 2, nowElapsedMillis = 0)
    LevitxTheme {
        WorkoutScreen(
            uiState = WorkoutUiState.Active(resting, bodyweightKg = 75.6, bands = emptyList()),
            now = { 0L },
            actions = noWorkoutActions,
        )
    }
}
