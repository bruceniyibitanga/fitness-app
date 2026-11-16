package app.levitx.phone.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.history.ExerciseRecord
import app.levitx.core.history.WorkoutRecord
import app.levitx.core.load.formatKg
import app.levitx.core.session.LoggedSet
import app.levitx.core.skills.GateMeasure
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.ui.BackHeader
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.clockTime
import app.levitx.phone.ui.dayDate
import app.levitx.phone.ui.measureText
import app.levitx.phone.ui.patternName
import app.levitx.phone.ui.repsLeftLabel
import kotlin.math.roundToInt

/** Past workout destination: connects [PastWorkoutViewModel] to [PastWorkoutScreen]. */
@Composable
fun PastWorkoutRoute(
    workoutId: String,
    onBack: () -> Unit,
    viewModel: PastWorkoutViewModel = hiltViewModel<PastWorkoutViewModel, PastWorkoutViewModel.Factory>(
        creationCallback = { factory -> factory.create(workoutId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PastWorkoutScreen(uiState, onBack)
}

/**
 * One past workout, as the Session detail artboard: when, what, and every
 * set of every exercise with reps left and any vest or band.
 */
@Composable
fun PastWorkoutScreen(uiState: PastWorkoutUiState, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LevitxColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        val workout = (uiState as? PastWorkoutUiState.Ready)?.workout
        BackHeader(
            workout?.let {
                dayDate(it.finishedAtEpochMillis)
            } ?: stringResource(R.string.history_title),
            onBack,
        ) {
            workout?.let { Caption(clockTime(it.startedAtEpochMillis)) }
        }
        when (uiState) {
            PastWorkoutUiState.Loading -> Unit

            PastWorkoutUiState.Unavailable -> Lede(
                stringResource(R.string.data_unavailable),
                Modifier.padding(ScreenPadding),
            )

            PastWorkoutUiState.NotFound -> Lede(
                stringResource(R.string.past_workout_none),
                Modifier.padding(ScreenPadding),
            )

            is PastWorkoutUiState.Ready -> Column(Modifier.verticalScroll(rememberScrollState())) {
                Column(
                    modifier = Modifier.padding(
                        start = ScreenPadding,
                        end = ScreenPadding,
                        top = 22.dp,
                        bottom = 22.dp,
                    ),
                ) {
                    Text(
                        text = workoutTitle(uiState.workout),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp,
                        color = LevitxColors.Text,
                        modifier = Modifier.semantics { heading() },
                    )
                    Caption(workoutMeta(uiState.workout), modifier = Modifier.padding(top = 8.dp))
                }
                Rule()
                uiState.workout.exercises.forEach { ExerciseBlock(it) }
            }
        }
    }
}

/** An exercise: its name and heaviest rep, then a row per set. */
@Composable
private fun ExerciseBlock(record: ExerciseRecord) {
    val measure = record.exercise?.measure ?: GateMeasure.REPS
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 18.dp, bottom = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = record.exercise?.name ?: stringResource(R.string.past_workout_unknown_exercise),
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = LevitxColors.Text,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            record.topKgPerRep?.let { Caption(stringResource(R.string.past_workout_kg, formatKg(it))) }
        }
        Row(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
            SetCell(stringResource(R.string.past_workout_set), LevitxColors.TextFaint, header = true)
            SetCell(stringResource(R.string.past_workout_done), LevitxColors.TextFaint, header = true)
            SetCell(stringResource(R.string.past_workout_left), LevitxColors.TextFaint, header = true)
            SetCell(stringResource(R.string.past_workout_load), LevitxColors.TextFaint, header = true, wide = true)
        }
        record.sets.forEachIndexed { index, set ->
            Rule()
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(40.dp)) {
                SetCell((index + 1).toString(), LevitxColors.TextFaint)
                SetCell(measureText(measure, set.value), LevitxColors.Text)
                SetCell(
                    set.repsInReserve?.let {
                        repsLeftLabel(it)
                    } ?: stringResource(R.string.workout_summary_no_left),
                    LevitxColors.TextSoft,
                )
                SetCell(loadText(set), LevitxColors.TextSoft, wide = true)
            }
        }
    }
    Rule()
}

@Composable
private fun SetCell(text: String, color: Color, header: Boolean = false, wide: Boolean = false) {
    val modifier = if (wide) Modifier.fillMaxWidth() else Modifier.width(72.dp)
    if (header) {
        Caption(text, color = color, modifier = modifier)
    } else {
        Text(text, fontSize = 14.sp, color = color, modifier = modifier)
    }
}

/** "+10 kg", "15 kg band help", or a dot for bodyweight only. */
@Composable
private fun loadText(set: LoggedSet): String = when {
    set.addedKg > 0.0 -> stringResource(R.string.past_workout_added, formatKg(set.addedKg))
    set.assistanceKg > 0.0 -> stringResource(R.string.past_workout_band, formatKg(set.assistanceKg))
    else -> stringResource(R.string.workout_summary_no_left)
}

/** What was trained, as movements: "Pulling up · Push-ups · Core". */
@Composable
internal fun workoutTitle(workout: WorkoutRecord): String {
    val names = workout.exercises.mapNotNull { it.exercise?.pattern }.distinct().map { patternName(it) }
    return names.joinToString(" · ").ifEmpty { stringResource(R.string.past_workout_fallback_title) }
}

/** "48 MIN · 14 SETS · 8,940 KG MOVED", without kilograms when there is no figure. */
@Composable
internal fun workoutMeta(workout: WorkoutRecord): String {
    val parts = listOfNotNull(
        duration(workout.minutes),
        pluralStringResource(R.plurals.past_workout_sets, workout.sets, workout.sets),
        workout.kgMoved?.let { stringResource(R.string.past_workout_moved, it.roundToInt()) },
    )
    return parts.joinToString(" · ")
}
