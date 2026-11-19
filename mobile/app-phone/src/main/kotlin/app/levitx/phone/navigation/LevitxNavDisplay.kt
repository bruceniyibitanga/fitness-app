package app.levitx.phone.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import app.levitx.phone.exercise.ExerciseRoute
import app.levitx.phone.intervals.IntervalDetailNavigation
import app.levitx.phone.intervals.IntervalDetailRoute
import app.levitx.phone.intervals.IntervalTimerRoute
import app.levitx.phone.launch.LaunchViewModel
import app.levitx.phone.main.MainNavigation
import app.levitx.phone.main.MainRoute
import app.levitx.phone.onboarding.OnboardingRoute
import app.levitx.phone.program.ProgramRoute
import app.levitx.phone.progress.ExerciseProgressNavigation
import app.levitx.phone.progress.ExerciseProgressRoute
import app.levitx.phone.progress.HistoryRoute
import app.levitx.phone.progress.PastWorkoutRoute
import app.levitx.phone.progress.ProgressNavigation
import app.levitx.phone.progress.ProgressRoute
import app.levitx.phone.review.WeekReviewRoute
import app.levitx.phone.settings.PhotoCreditsScreen
import app.levitx.phone.settings.SettingsRoute
import app.levitx.phone.train.AllProgramsRoute
import app.levitx.phone.train.FocusWorkoutNavigation
import app.levitx.phone.train.FocusWorkoutRoute
import app.levitx.phone.train.SearchRoute
import app.levitx.phone.train.TrainNavigation
import app.levitx.phone.ui.LevitxMotion
import app.levitx.phone.ui.LocalSharedTransitionScope
import app.levitx.phone.workout.WorkoutRoute

/**
 * The phone app's navigation, built on Navigation 3.
 *
 * The back stack is a list of [Destination]s that we own, so moving between
 * screens is adding to or removing from a list. It is saved across
 * configuration changes and process death.
 *
 * The decorators give each screen its own saved state and its own ViewModel
 * store, so a ViewModel lives exactly as long as its screen is on the stack.
 */
@Composable
fun LevitxNavDisplay(modifier: Modifier = Modifier, launchViewModel: LaunchViewModel = hiltViewModel()) {
    // Reading the coach's state takes milliseconds; until then draw nothing rather than flash a screen.
    val start by launchViewModel.startDestination.collectAsStateWithLifecycle()
    start?.let { LevitxNavDisplay(start = it, modifier = modifier) }
}

/**
 * The screens, and how they change. Opening a screen moves it in from the
 * right and going back moves it out the same way, including while the back
 * gesture is being dragged. Photos that appear on both screens travel between
 * them: see [Photo].
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun LevitxNavDisplay(start: Destination, modifier: Modifier) {
    val backStack = rememberNavBackStack(start)
    SharedTransitionLayout(modifier = modifier) {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                sharedTransitionScope = this,
                transitionSpec = { LevitxMotion.forward() },
                popTransitionSpec = { LevitxMotion.back() },
                predictivePopTransitionSpec = { LevitxMotion.back() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    mainEntries(backStack)
                    detailEntries(backStack)
                    trainEntries(backStack)
                    progressEntries(backStack)
                    settingsEntries(backStack)
                },
            )
        }
    }
}

/**
 * Shows [destination] with nothing under it, so back leaves the app instead of
 * returning to setup. Adds first, then drops the rest, so the stack is never empty.
 */
private fun MutableList<NavKey>.replaceAllWith(destination: Destination) {
    add(destination)
    subList(0, size - 1).clear()
}

/** Setup, and the tabs it leads to. */
private fun EntryProviderScope<NavKey>.mainEntries(backStack: MutableList<NavKey>) {
    entry<Destination.Onboarding> {
        OnboardingRoute(onFinished = { backStack.replaceAllWith(Destination.Main) })
    }
    entry<Destination.Main> {
        MainRoute(
            MainNavigation(
                onOpenWorkout = { backStack.add(Destination.Workout) },
                onOpenFocusWorkout = { backStack.add(Destination.FocusWorkout(it)) },
                onOpenInterval = { backStack.add(Destination.IntervalWorkout(it)) },
                onOpenExercise = { backStack.add(Destination.ExerciseDetail(it)) },
                onOpenProgram = { backStack.add(Destination.Program(it)) },
                onOpenProgress = { backStack.add(Destination.Progress) },
                onOpenSettings = { backStack.add(Destination.Settings) },
                onOpenSearch = { backStack.add(Destination.Search) },
                onOpenWeekReview = { backStack.add(Destination.WeekReview) },
                onOpenAllPrograms = { backStack.add(Destination.AllPrograms) },
            ),
        )
    }
}

/** The screens that open over the tabs: a workout, a single workout's page, an exercise and a program. */
private fun EntryProviderScope<NavKey>.detailEntries(backStack: MutableList<NavKey>) {
    entry<Destination.Workout> {
        WorkoutRoute(onDone = { backStack.removeLastOrNull() })
    }
    entry<Destination.FocusWorkout> { key ->
        FocusWorkoutRoute(
            focusName = key.focus,
            navigation = FocusWorkoutNavigation(
                onBack = { backStack.removeLastOrNull() },
                // The workout replaces this screen, so finishing it goes back to the tabs.
                onOpenWorkout = {
                    backStack.removeLastOrNull()
                    backStack.add(Destination.Workout)
                },
                onOpenExercise = { backStack.add(Destination.ExerciseDetail(it)) },
            ),
        )
    }
    entry<Destination.ExerciseDetail> { key ->
        ExerciseRoute(exerciseId = key.exerciseId, onBack = { backStack.removeLastOrNull() })
    }
    entry<Destination.Program> { key ->
        ProgramRoute(pathId = key.pathId, onBack = { backStack.removeLastOrNull() })
    }
}

/** Search, every program, and a cardio or mobility workout's page and its timer. */
private fun EntryProviderScope<NavKey>.trainEntries(backStack: MutableList<NavKey>) {
    entry<Destination.Search> {
        SearchRoute(
            navigation = TrainNavigation(
                onOpenWorkout = { backStack.add(Destination.FocusWorkout(it)) },
                onOpenProgram = { backStack.add(Destination.Program(it)) },
                onOpenInterval = { backStack.add(Destination.IntervalWorkout(it)) },
                onOpenExercise = { backStack.add(Destination.ExerciseDetail(it)) },
                onOpenSearch = {},
                onOpenAllPrograms = { backStack.add(Destination.AllPrograms) },
            ),
            onBack = { backStack.removeLastOrNull() },
        )
    }
    entry<Destination.WeekReview> { WeekReviewRoute(onClose = { backStack.removeLastOrNull() }) }
    entry<Destination.AllPrograms> {
        AllProgramsRoute(
            onBack = { backStack.removeLastOrNull() },
            onOpenProgram = { backStack.add(Destination.Program(it)) },
        )
    }
    entry<Destination.IntervalWorkout> { key ->
        IntervalDetailRoute(
            workoutId = key.workoutId,
            navigation = IntervalDetailNavigation(
                onBack = { backStack.removeLastOrNull() },
                // The timer replaces this screen, so finishing it goes back to the tabs.
                onStart = {
                    backStack.removeLastOrNull()
                    backStack.add(Destination.IntervalTimer(key.workoutId))
                },
                onOpenMove = { backStack.add(Destination.ExerciseDetail(it)) },
            ),
        )
    }
    entry<Destination.IntervalTimer> { key ->
        IntervalTimerRoute(workoutId = key.workoutId, onDone = { backStack.removeLastOrNull() })
    }
}

/** Progress, an exercise's progress, History and a past workout. */
private fun EntryProviderScope<NavKey>.progressEntries(backStack: MutableList<NavKey>) {
    entry<Destination.Progress> {
        ProgressRoute(
            ProgressNavigation(
                onBack = { backStack.removeLastOrNull() },
                onOpenExercise = { backStack.add(Destination.ExerciseProgress(it)) },
                onOpenHistory = { backStack.add(Destination.History) },
            ),
        )
    }
    entry<Destination.ExerciseProgress> { key ->
        ExerciseProgressRoute(
            exerciseId = key.exerciseId,
            navigation = ExerciseProgressNavigation(
                onBack = { backStack.removeLastOrNull() },
                onOpenHowTo = { backStack.add(Destination.ExerciseDetail(it)) },
                onOpenWorkout = { backStack.add(Destination.PastWorkout(it)) },
            ),
        )
    }
    entry<Destination.History> {
        HistoryRoute(
            onBack = { backStack.removeLastOrNull() },
            onOpenWorkout = { backStack.add(Destination.PastWorkout(it)) },
        )
    }
    entry<Destination.PastWorkout> { key ->
        PastWorkoutRoute(workoutId = key.workoutId, onBack = { backStack.removeLastOrNull() })
    }
}

/** Settings and photo credits. Deleting everything starts again from setup. */
private fun EntryProviderScope<NavKey>.settingsEntries(backStack: MutableList<NavKey>) {
    entry<Destination.Settings> {
        SettingsRoute(
            onBack = { backStack.removeLastOrNull() },
            onOpenCredits = { backStack.add(Destination.PhotoCredits) },
            onDeleted = { backStack.replaceAllWith(Destination.Onboarding) },
        )
    }
    entry<Destination.PhotoCredits> {
        PhotoCreditsScreen(onBack = { backStack.removeLastOrNull() })
    }
}
