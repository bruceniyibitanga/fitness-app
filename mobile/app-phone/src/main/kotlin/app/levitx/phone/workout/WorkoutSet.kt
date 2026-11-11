package app.levitx.phone.workout

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.load.formatKg
import app.levitx.core.skills.GateMeasure
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.QuietBar
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.durationText
import app.levitx.phone.ui.measureText

/*
 * The set up next: the counter, how hard to go, the load, what follows, and
 * the buttons to log it or time it.
 */

/**
 * The set to do now. For a hold or slow negatives the main action starts the
 * timer, and what it counted comes back here as the figure to log, ready to
 * correct.
 */
@Composable
internal fun ColumnScope.NextSet(state: WorkoutUiState.Active, actions: WorkoutActions) {
    val run = state.run
    val plan = state.plan
    val set = state.nextSet
    val isReps = plan.exercise.measure == GateMeasure.REPS
    // Keyed on the exercise and set, so each set starts at its target, and on
    // what a timer counted, so a stopped timer fills it in.
    val key = "${run.exerciseIndex}/${run.nextSetIndex}"
    var value by rememberSaveable(key, state.counted) { mutableIntStateOf(state.counted ?: set.target) }
    var repsLeft by rememberSaveable(key) { mutableIntStateOf(if (set.isBestEffort) 0 else plan.repsInReserve) }
    // The run keeps the load, so it carries between sets and survives a timer.
    val addedKg = run.load.addedKg
    val assistanceKg = run.load.assistanceKg
    val bandId = state.bands.firstOrNull { assistanceKg > 0.0 && it.assistanceKg == assistanceKg }?.id

    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
    ) {
        SetHeader(state, loadPerRep(state, addedKg, assistanceKg), value, onChange = { value = it })
        if (isReps) {
            Rule(Modifier.padding(top = 22.dp))
            RepsLeft(selected = repsLeft, onSelect = { repsLeft = it })
        }
        if (state.tracksLoad) {
            Rule(Modifier.padding(top = 22.dp))
            LoadChooser(
                state = state,
                addedKg = addedKg,
                onAddedKgChange = { actions.onChooseLoad(it, assistanceKg) },
                bandId = bandId,
                onBandChange = { id ->
                    actions.onChooseLoad(
                        addedKg,
                        state.bands.firstOrNull { it.id == id }?.assistanceKg ?: 0.0,
                    )
                },
                assistanceKg = assistanceKg,
            )
        }
    }
    NextUp(state)
    SetButtons(
        state = state,
        hint = if (isReps) stringResource(R.string.workout_log_hint, value, repsLeftLabel(repsLeft)) else null,
        // Holds have no reps left in the tank, so none is recorded for them.
        onLog = { actions.onLogSet(value, repsLeft.takeIf { isReps }, addedKg, assistanceKg) },
        actions = actions,
    )
}

/** The exercise line, the counter and how hard to go. */
@Composable
private fun SetHeader(state: WorkoutUiState.Active, load: Double?, value: Int, onChange: (Int) -> Unit) {
    val run = state.run
    val plan = state.plan
    val isReps = plan.exercise.measure == GateMeasure.REPS
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 22.dp)) {
        Caption(
            listOfNotNull(
                (run.exerciseIndex + 1).toString().padStart(2, '0'),
                plan.exercise.name,
                load?.let { stringResource(R.string.kg_per_rep, formatKg(it)) },
            ).joinToString(" · "),
        )
        Counter(
            value = value,
            target = state.nextSet.target,
            unit = stringResource(if (isReps) R.string.workout_unit_reps else R.string.workout_unit_seconds),
            onChange = onChange,
        )
        Text(
            text = instructions(state),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = LevitxColors.TextMuted,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

/**
 * The main action and the one beside it. Counted sets: Log set, and Skip (or
 * Next once a set is in). Timed sets: start the timer, and Log for a set done
 * without it; once the timer has counted, Log set, and Again to redo it.
 */
@Composable
private fun SetButtons(state: WorkoutUiState.Active, hint: String?, onLog: () -> Unit, actions: WorkoutActions) {
    val kind = state.timerKind
    val startTimer = kind != null && state.counted == null
    Row(modifier = Modifier.height(BottomBarHeight)) {
        AccentBar(
            text = when {
                !startTimer -> stringResource(R.string.workout_log_set)
                kind == TimerKind.HOLD -> stringResource(R.string.workout_start_hold)
                else -> stringResource(R.string.workout_start_negatives)
            },
            hint = if (startTimer) stringResource(R.string.workout_count_in_hint) else hint,
            onClick = if (startTimer) ({ actions.onTimer(TimerAction.START) }) else onLog,
            height = BottomBarHeight,
            modifier = Modifier.weight(2f),
        )
        val nothingDone = state.run.logged[state.run.exerciseIndex].isEmpty()
        QuietBar(
            text = stringResource(
                when {
                    startTimer -> R.string.workout_log_short
                    kind != null -> R.string.workout_again_short
                    nothingDone -> R.string.workout_skip_short
                    else -> R.string.workout_next_short
                },
            ),
            onClick = when {
                startTimer -> onLog
                kind != null -> ({ actions.onTimer(TimerAction.START) })
                else -> actions.onSkipExercise
            },
            modifier = Modifier
                .weight(1f)
                .height(BottomBarHeight),
        )
    }
}

/** How hard to go on this set, in one line. */
@Composable
private fun instructions(state: WorkoutUiState.Active): String = when {
    !state.plan.isHard -> stringResource(R.string.workout_light_instructions)

    state.nextSet.isBestEffort -> stringResource(R.string.workout_best_effort_instructions)

    state.plan.exercise.measure == GateMeasure.HOLD_SECONDS -> stringResource(R.string.today_lead_note_hold)

    // Negatives are all about the lowering, so that's the instruction, whatever the effort.
    state.plan.exercise.lowerSeconds != null -> stringResource(
        R.string.workout_negative_instructions,
        checkNotNull(state.plan.exercise.lowerSeconds),
    )

    else -> pluralStringResource(
        R.plurals.workout_work_instructions,
        state.plan.repsInReserve,
        state.plan.repsInReserve,
    )
}

/** The strip above the buttons: the rest that follows this set, and what comes after it. */
@Composable
private fun NextUp(state: WorkoutUiState.Active) {
    val run = state.run
    val plan = state.plan
    val lastSetOfExercise = run.nextSetIndex == plan.sets.size - 1
    val nextPlan = if (lastSetOfExercise) run.session.plans.getOrNull(run.exerciseIndex + 1) else plan
    Rule()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(BottomBarHeight)
            .padding(horizontal = ScreenPadding),
    ) {
        if (nextPlan != null) {
            // "REST" over the time, in a fixed column so the next line always starts in the same place.
            Column(modifier = Modifier.width(64.dp)) {
                Caption(stringResource(R.string.workout_rest_label), color = LevitxColors.Ice)
                Caption(durationText(plan.restSeconds), color = LevitxColors.Ice)
            }
            Text(
                text = if (lastSetOfExercise) {
                    stringResource(R.string.workout_then_exercise, nextPlan.exercise.name)
                } else {
                    stringResource(R.string.workout_then_set, run.nextSetIndex + 2, plan.sets.size)
                },
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = LevitxColors.Text,
                modifier = Modifier.weight(1f),
            )
            Caption(
                stringResource(R.string.workout_aim, measureText(nextPlan.exercise.measure, nextPlan.workTarget)),
                color = LevitxColors.TextFaint,
            )
        } else {
            Text(
                text = stringResource(R.string.workout_last_set),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = LevitxColors.Text,
            )
        }
    }
}
