package app.levitx.phone

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.ExerciseLookup
import app.levitx.core.coach.ExercisePlan
import app.levitx.core.coach.Intensity
import app.levitx.core.coach.PlannedSession
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.SkillTraining
import app.levitx.core.coach.WeighIn
import app.levitx.core.coach.WorkoutFocus
import app.levitx.core.coach.WorkoutRun
import app.levitx.core.coach.bodyweightTrend
import app.levitx.core.history.ExerciseRecord
import app.levitx.core.history.WorkoutRecord
import app.levitx.core.session.LoggedSet
import app.levitx.core.session.PlannedSet
import app.levitx.core.session.SetLoad
import app.levitx.core.skills.GateMeasure
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
import app.levitx.phone.progress.ExerciseProgressNavigation
import app.levitx.phone.progress.ExerciseProgressScreen
import app.levitx.phone.progress.HistoryScreen
import app.levitx.phone.progress.PastWorkoutScreen
import app.levitx.phone.progress.PastWorkoutUiState
import app.levitx.phone.progress.ProgressNavigation
import app.levitx.phone.progress.ProgressRange
import app.levitx.phone.progress.ProgressScreen
import app.levitx.phone.progress.exerciseProgressState
import app.levitx.phone.progress.historyState
import app.levitx.phone.progress.progressState
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
import app.levitx.phone.workout.GatePass
import app.levitx.phone.workout.PendingSet
import app.levitx.phone.workout.ReportBest
import app.levitx.phone.workout.ReportChange
import app.levitx.phone.workout.ReportExercise
import app.levitx.phone.workout.SessionReport
import app.levitx.phone.workout.SetTimer
import app.levitx.phone.workout.TimerKind
import app.levitx.phone.workout.WorkoutActions
import app.levitx.phone.workout.WorkoutScreen
import app.levitx.phone.workout.WorkoutUiState
import com.android.tools.screenshot.PreviewTest
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

/*
 * Every screen, rendered at the design's 390 × 844 artboard size, so CI can
 * turn each into an image to compare with its artboard in "Levitx App Design".
 * The data is what a real person would see, worked out by the real coach.
 */

private val noOnboardingActions = OnboardingActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
private val noTodayActions =
    TodayActions(onStart = {}, onRest = {}, onAcknowledgeWeightNotice = {}, onOpenExercise = {}, onOpenProgress = {})

/** Someone after a muscle-up who can do a pull-up and has a bar. */
private fun muscleUp(kit: Set<Equipment> = setOf(Equipment.PULL_UP_BAR)) =
    AthleteModel.starting(CoachProfile("muscle_up", equipment = kit), PullUpAnswer.YES)

private val noWorkoutActions = WorkoutActions.None

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
            PathScreen(
                PathUiState.Ready(path, currentIndex = 2, bestByGateId = bests),
                onTrain = {},
                onChangeGoal = {},
                onOpenProgress = {},
            )
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

/** A workout that starts with three sets of [target] of the exercise [id]. */
private fun timedRun(id: String, target: Int): WorkoutRun {
    val plan = ExercisePlan(
        exercise = checkNotNull(ExerciseLookup.byId(id)),
        sets = List(3) { PlannedSet(target, false) },
        restSeconds = 120,
        repsInReserve = 1,
        intensity = Intensity.HARD,
    )
    return WorkoutRun("preview", PlannedSession(listOf(plan)), startedAtEpochMillis = 0)
}

@PreviewTest
@Preview(name = "17 Hold timer", widthDp = 390, heightDp = 844)
@Composable
fun HoldTimerScreenshot() {
    LevitxTheme {
        WorkoutScreen(
            uiState = WorkoutUiState.Active(
                timedRun("vertical_pull.dead_hang", target = 30),
                bodyweightKg = 75.6,
                bands = emptyList(),
                timer = SetTimer(TimerKind.HOLD, startsAtElapsedMillis = 0),
            ),
            // 18 seconds into the hold.
            now = { 18_400L },
            actions = noWorkoutActions,
        )
    }
}

@PreviewTest
@Preview(name = "18 Negative timer", widthDp = 390, heightDp = 844)
@Composable
fun NegativeTimerScreenshot() {
    LevitxTheme {
        WorkoutScreen(
            uiState = WorkoutUiState.Active(
                timedRun("vertical_pull.negative", target = 5),
                bodyweightKg = 75.6,
                bands = emptyList(),
                timer = SetTimer(TimerKind.NEGATIVE, startsAtElapsedMillis = 0, lowerMillis = 5_000, reps = 5),
            ),
            // Rep 2, about three seconds into the lowering.
            now = { 9_000L + 4_000L + 2_500L },
            actions = noWorkoutActions,
        )
    }
}

@PreviewTest
@Preview(name = "19 Session summary", widthDp = 390, heightDp = 1100)
@Composable
fun SessionSummaryScreenshot() {
    fun sets(vararg pairs: Pair<Int, Int?>) =
        pairs.map { (value, left) -> LoggedSet(value, left, isBestEffort = false) }
    val report = SessionReport(
        sets = 11,
        minutes = 48,
        kgMoved = 8_940,
        bests = listOf(
            ReportBest("Strict pull-up", GateMeasure.REPS, value = 8, was = 7),
            ReportBest("Tuck front lever", GateMeasure.HOLD_SECONDS, value = 8, was = 6),
        ),
        exercises = listOf(
            ReportExercise("Tuck front lever", GateMeasure.HOLD_SECONDS, sets(8 to null, 7 to null, 6 to null)),
            ReportExercise("Strict pull-up", GateMeasure.REPS, sets(8 to 2, 8 to 1, 7 to 1)),
            ReportExercise("Feet-up push-up", GateMeasure.REPS, sets(12 to 3, 12 to 2)),
            ReportExercise("Hollow body hold", GateMeasure.HOLD_SECONDS, sets(30 to null, 22 to null, 30 to null)),
        ),
        changes = listOf(
            ReportChange.Target("Strict pull-up", GateMeasure.REPS, from = 8, to = 9),
            ReportChange.MovedUp("Feet-up push-up", next = "Box-elevated push-up"),
            ReportChange.Held("Hollow body hold", GateMeasure.HOLD_SECONDS, target = 30),
        ),
    )
    LevitxTheme {
        WorkoutScreen(uiState = WorkoutUiState.Finished(report), now = { 0L }, actions = noWorkoutActions)
    }
}

/** A workout that starts with an attempt at the muscle-up path's 10 pull-ups gate. */
private fun gateAttemptRun(): WorkoutRun {
    val gate = checkNotNull(SkillCatalogue.gateById("muscle_up.pull_ups"))
    val plan = ExercisePlan(
        exercise = checkNotNull(SkillTraining.exerciseFor(gate)),
        sets = listOf(PlannedSet(10, true)) + List(3) { PlannedSet(5, false) },
        restSeconds = 120,
        repsInReserve = 2,
        intensity = Intensity.HARD,
        gateId = gate.id,
        isGateAttempt = true,
    )
    return WorkoutRun("preview", PlannedSession(listOf(plan)), startedAtEpochMillis = 0)
}

private val gateBestsBefore = mapOf(
    "muscle_up.dead_hang" to 30,
    "muscle_up.dips" to 10,
    "muscle_up.pull_ups" to 8,
)

@PreviewTest
@Preview(name = "20 Gate test", widthDp = 390, heightDp = 844)
@Composable
fun GateTestScreenshot() {
    LevitxTheme {
        WorkoutScreen(
            uiState = WorkoutUiState.Active(gateAttemptRun(), 75.6, emptyList(), gateBests = gateBestsBefore),
            now = { 0L },
            actions = noWorkoutActions,
        )
    }
}

@PreviewTest
@Preview(name = "21 Gate check", widthDp = 390, heightDp = 844)
@Composable
fun GateCheckScreenshot() {
    LevitxTheme {
        WorkoutScreen(
            uiState = WorkoutUiState.Active(
                gateAttemptRun().deferAttempt(),
                75.6,
                emptyList(),
                gateBests = gateBestsBefore,
                pending = PendingSet(10, repsInReserve = 0, load = SetLoad.None),
            ),
            now = { 0L },
            actions = noWorkoutActions,
        )
    }
}

@PreviewTest
@Preview(name = "22 Gate passed", widthDp = 390, heightDp = 844)
@Composable
fun GatePassedScreenshot() {
    val run = gateAttemptRun()
    val gate = checkNotNull(SkillCatalogue.gateById("muscle_up.pull_ups"))
    LevitxTheme {
        WorkoutScreen(
            uiState = WorkoutUiState.Active(
                run.logSet(11, repsInReserve = 0, nowElapsedMillis = 0),
                75.6,
                emptyList(),
                gateBests = gateBestsBefore + (gate.id to 11),
                passed = GatePass(gate, SkillCatalogue.muscleUp, value = 11, was = 8),
            ),
            now = { 0L },
            actions = noWorkoutActions,
        )
    }
}

private const val PREVIEW_DAY = 24L * 60 * 60 * 1000

/** Twelve weeks of made-up workouts for the progress screens, three a week with one week off. */
private val previewWorkouts: List<WorkoutRecord> = (0 until 36).filter { it / 3 != 6 }.map { index ->
    val day = 20_400 + index * 7 / 3
    val pullUps = 4 + index / 6
    val vest = if (index >= 24) 5.0 + (index - 24) / 3 * 2.5 else 0.0
    val pushUp = when {
        index < 12 -> "horizontal_push.knees"
        index < 24 -> "horizontal_push.push_up"
        else -> "horizontal_push.feet_30cm"
    }
    WorkoutRecord(
        id = "preview-$index",
        startedAtEpochMillis = day * PREVIEW_DAY + 17 * 3_600_000L,
        finishedAtEpochMillis = day * PREVIEW_DAY + 17 * 3_600_000L + 46 * 60_000L,
        exercises = listOf(
            ExerciseRecord(
                "vertical_pull.pull_up",
                // A vest from week nine, so the load climbs as well as the reps.
                List(3) { LoggedSet(pullUps - it / 2, 2 - it.coerceAtMost(1), false, addedKg = vest) },
                78.0,
            ),
            ExerciseRecord(pushUp, listOf(LoggedSet(10, 2, false), LoggedSet(9, 2, false)), 78.0),
            ExerciseRecord("core.plank", listOf(LoggedSet(40, null, false), LoggedSet(35, null, false)), 78.0),
        ),
    )
}

private val previewNow = previewWorkouts.last().finishedAtEpochMillis + PREVIEW_DAY

private fun previewDate(epochMillis: Long): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate()

private val noProgressNavigation = ProgressNavigation(onBack = {}, onOpenExercise = {}, onOpenHistory = {})

@PreviewTest
@Preview(name = "23 Progress", widthDp = 390, heightDp = 1500)
@Composable
fun ProgressScreenshot() {
    val hang = checkNotNull(SkillCatalogue.gateById("muscle_up.dead_hang"))
    val model = muscleUp().copy(gateBests = mapOf(hang.id to hang.target))
    LevitxTheme {
        ProgressScreen(
            uiState = progressState(previewWorkouts, model, ProgressRange.THREE_MONTHS, previewNow),
            navigation = noProgressNavigation,
            onChooseRange = {},
        )
    }
}

@PreviewTest
@Preview(name = "24 Exercise progress", widthDp = 390, heightDp = 1200)
@Composable
fun ExerciseProgressScreenshot() {
    LevitxTheme {
        ExerciseProgressScreen(
            uiState = exerciseProgressState("vertical_pull.pull_up", previewWorkouts, muscleUp()),
            navigation = ExerciseProgressNavigation(onBack = {}, onOpenHowTo = {}, onOpenWorkout = {}),
        )
    }
}

@PreviewTest
@Preview(name = "25 History", widthDp = 390, heightDp = 1100)
@Composable
fun HistoryScreenshot() {
    LevitxTheme {
        HistoryScreen(
            uiState = historyState(
                month = YearMonth.from(previewDate(previewNow)),
                workouts = previewWorkouts,
                trainingDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                today = previewDate(previewNow),
                zone = ZoneOffset.UTC,
            ),
            onBack = {},
            onOpenWorkout = {},
            onMoveMonth = {},
        )
    }
}

@PreviewTest
@Preview(name = "26 Past workout", widthDp = 390, heightDp = 1000)
@Composable
fun PastWorkoutScreenshot() {
    LevitxTheme {
        PastWorkoutScreen(PastWorkoutUiState.Ready(previewWorkouts.last()), onBack = {})
    }
}
