package app.levitx.phone

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.WeighIn
import app.levitx.core.coach.WorkoutFocus
import app.levitx.core.coach.WorkoutRun
import app.levitx.core.coach.bodyweightTrend
import app.levitx.core.skills.SkillCatalogue
import app.levitx.data.equipment.Band
import app.levitx.phone.body.BarLoad
import app.levitx.phone.body.BodyActions
import app.levitx.phone.body.BodyScreen
import app.levitx.phone.body.BodyUiState
import app.levitx.phone.exercise.ExerciseScreen
import app.levitx.phone.exercise.exerciseState
import app.levitx.phone.main.MainScreen
import app.levitx.phone.main.MainTab
import app.levitx.phone.onboarding.OnboardingActions
import app.levitx.phone.onboarding.OnboardingScreen
import app.levitx.phone.onboarding.OnboardingStep
import app.levitx.phone.onboarding.OnboardingUiState
import app.levitx.phone.path.PathScreen
import app.levitx.phone.path.PathUiState
import app.levitx.phone.program.ProgramScreen
import app.levitx.phone.program.ProgramUiState
import app.levitx.phone.skills.SkillsScreen
import app.levitx.phone.skills.SkillsUiState
import app.levitx.phone.skills.standings
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.today.TodayActions
import app.levitx.phone.today.TodayScreen
import app.levitx.phone.today.TodayUiState
import app.levitx.phone.train.FocusWorkoutNavigation
import app.levitx.phone.train.FocusWorkoutScreen
import app.levitx.phone.train.FocusWorkoutUiState
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

@PreviewTest
@Preview(name = "11 Body", widthDp = 390, heightDp = 1100)
@Composable
fun BodyScreenshot() {
    val day = 24L * 60 * 60 * 1_000
    val now = 40 * day
    val weighIns = listOf(76.4, 76.1, 76.2, 75.8, 75.9, 75.5, 75.6, 75.2, 75.3, 74.9)
        .mapIndexed { i, kg -> WeighIn(now - (36 - i * 4) * day, kg) }
    val trend = checkNotNull(bodyweightTrend(weighIns, now))
    val change = checkNotNull(trend.weeklyChange) * trend.currentKg
    val loads = listOf("Strict pull-up" to 0.95, "Push-up" to 0.64).map { (name, share) ->
        BarLoad(name, name, share, measured = share < 0.9, trend.currentKg * share, change * share)
    }
    LevitxTheme {
        MainScreen(selected = MainTab.BODY, onSelect = {}) {
            BodyScreen(
                uiState = BodyUiState.Ready(
                    nowEpochMillis = now,
                    weighIns = weighIns,
                    trend = trend,
                    onTheBar = loads,
                    equipment = setOf(Equipment.PULL_UP_BAR, Equipment.BAND),
                    bands = listOf(Band("1", "Red", 22.5)),
                ),
                actions = BodyActions({}, {}, { _, _ -> }, {}),
            )
        }
    }
}

@PreviewTest
@Preview(name = "12 Plan", widthDp = 390, heightDp = 844)
@Composable
fun PlanScreenshot() {
    val path = SkillCatalogue.muscleUp
    val bests = mapOf(path.gates[0].id to 45, path.gates[1].id to 10, path.gates[2].id to 7)
    LevitxTheme {
        MainScreen(selected = MainTab.PLAN, onSelect = {}) {
            PathScreen(PathUiState.Ready(path, currentIndex = 2, bestByGateId = bests), onTrain = {}, onChangeGoal = {})
        }
    }
}

@PreviewTest
@Preview(name = "13 Skills", widthDp = 390, heightDp = 844)
@Composable
fun SkillsScreenshot() {
    LevitxTheme {
        MainScreen(selected = MainTab.SKILLS, onSelect = {}) {
            SkillsScreen(SkillsUiState.Ready(standings(muscleUp())), onOpenExercise = {})
        }
    }
}

@PreviewTest
@Preview(name = "14 Program", widthDp = 390, heightDp = 844)
@Composable
fun ProgramScreenshot() {
    val path = SkillCatalogue.firstPullUp
    LevitxTheme {
        ProgramScreen(
            uiState = ProgramUiState.Ready(path, mapOf(path.gates[0].id to 30), currentIndex = 1, isPlan = false),
            onMakePlan = {},
            onBack = {},
        )
    }
}

@PreviewTest
@Preview(name = "15 Workout detail", widthDp = 390, heightDp = 844)
@Composable
fun WorkoutDetailScreenshot() {
    LevitxTheme {
        FocusWorkoutScreen(
            uiState = FocusWorkoutUiState.Ready(
                focus = WorkoutFocus.UPPER,
                session = Coach().workout(muscleUp(), WorkoutFocus.UPPER, 0),
                bodyweightKg = 75.6,
                hasWorkoutInProgress = false,
            ),
            onStart = {},
            navigation = FocusWorkoutNavigation(onBack = {}, onOpenWorkout = {}, onOpenExercise = {}),
        )
    }
}

@PreviewTest
@Preview(name = "16 Exercise", widthDp = 390, heightDp = 844)
@Composable
fun ExerciseScreenshot() {
    LevitxTheme {
        ExerciseScreen(uiState = exerciseState(muscleUp(), "horizontal_push.push_up", nowEpochMillis = 0), onBack = {})
    }
}
