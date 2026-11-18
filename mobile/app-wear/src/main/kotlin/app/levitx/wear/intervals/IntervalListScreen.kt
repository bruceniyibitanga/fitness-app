package app.levitx.wear.intervals

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import app.levitx.core.intervals.IntervalCatalogue
import app.levitx.core.intervals.IntervalKind
import app.levitx.core.intervals.IntervalWorkout
import app.levitx.wear.R
import app.levitx.wear.theme.LevitxWearTheme

/** Cardio and mobility list destination: starts the chosen workout and opens the timer. */
@Composable
fun IntervalListRoute(onOpenTimer: () -> Unit, viewModel: IntervalListViewModel = hiltViewModel()) {
    IntervalListScreen(
        workouts = viewModel.workouts,
        onStart = { workoutId ->
            viewModel.start(workoutId)
            onOpenTimer()
        },
    )
}

/**
 * Every cardio and mobility workout, one tap from starting. The timer gives
 * ten seconds to get ready, so there is no separate start screen.
 */
@Composable
fun IntervalListScreen(workouts: List<IntervalWorkout>, onStart: (String) -> Unit, modifier: Modifier = Modifier) {
    val listState = rememberTransformingLazyColumnState()
    ScreenScaffold(scrollState = listState, modifier = modifier) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item { ListHeader { Text(stringResource(R.string.intervals_title)) } }
            workouts.forEach { workout ->
                item(key = workout.id) {
                    FilledTonalButton(
                        onClick = { onStart(workout.id) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(workout.name) },
                        secondaryLabel = { Text(metaText(workout)) },
                    )
                }
            }
        }
    }
}

/** "Cardio · 16 min · quiet" or "Mobility · 11 min". Every mobility workout is quiet, so it isn't said. */
@Composable
private fun metaText(workout: IntervalWorkout): String = when {
    workout.kind == IntervalKind.MOBILITY -> stringResource(R.string.intervals_mobility, workout.minutes)
    workout.isQuiet -> stringResource(R.string.intervals_cardio_quiet, workout.minutes)
    else -> stringResource(R.string.intervals_cardio, workout.minutes)
}

@WearPreviewDevices
@Composable
private fun IntervalListPreview() {
    LevitxWearTheme { IntervalListScreen(workouts = IntervalCatalogue.all, onStart = {}) }
}
