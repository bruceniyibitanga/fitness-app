package app.levitx.phone.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import app.levitx.phone.exercise.ExerciseRoute
import app.levitx.phone.launch.LaunchViewModel
import app.levitx.phone.main.MainNavigation
import app.levitx.phone.main.MainRoute
import app.levitx.phone.onboarding.OnboardingRoute
import app.levitx.phone.program.ProgramRoute
import app.levitx.phone.train.FocusWorkoutNavigation
import app.levitx.phone.train.FocusWorkoutRoute
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

@Composable
private fun LevitxNavDisplay(start: Destination, modifier: Modifier) {
    val backStack = rememberNavBackStack(start)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<Destination.Onboarding> {
                OnboardingRoute(onFinished = { backStack.replaceAllWith(Destination.Main) })
            }
            entry<Destination.Main> {
                MainRoute(
                    MainNavigation(
                        onOpenWorkout = { backStack.add(Destination.Workout) },
                        onOpenFocusWorkout = { backStack.add(Destination.FocusWorkout(it)) },
                        onOpenExercise = { backStack.add(Destination.ExerciseDetail(it)) },
                        onOpenProgram = { backStack.add(Destination.Program(it)) },
                    ),
                )
            }
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
        },
    )
}

/**
 * Shows [destination] with nothing under it, so back leaves the app instead of
 * returning to setup. Adds first, then drops the rest, so the stack is never empty.
 */
private fun MutableList<NavKey>.replaceAllWith(destination: Destination) {
    add(destination)
    subList(0, size - 1).clear()
}
