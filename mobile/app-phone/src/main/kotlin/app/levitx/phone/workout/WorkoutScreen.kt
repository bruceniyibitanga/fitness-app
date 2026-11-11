package app.levitx.phone.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.WorkoutRun
import app.levitx.core.load.formatKg
import app.levitx.core.skills.GateMeasure
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.BackButton
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.QuietBar
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.durationText
import app.levitx.phone.ui.measureText

/** The bottom bars, as on the artboard. */
internal val BottomBarHeight = 64.dp

/** Workout destination: connects [WorkoutViewModel] to [WorkoutScreen]. */
@Composable
fun WorkoutRoute(onDone: () -> Unit, viewModel: WorkoutViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WorkoutScreen(
        uiState = uiState,
        now = viewModel::elapsedRealtimeMillis,
        actions = WorkoutActions(
            onLogSet = viewModel::logSet,
            onEndRest = viewModel::endRest,
            onSkipExercise = viewModel::skipExercise,
            onFinish = viewModel::finish,
            onDone = onDone,
        ),
    )
}

/**
 * Everything the person can do during a workout, grouped so the screen takes one parameter for them.
 *
 * @property onLogSet Records a set: reps or seconds, reps left in the tank (null for holds), added
 *   weight and band help in kg.
 * @property onEndRest Ends rest early, or when it runs out.
 * @property onSkipExercise Moves on to the next exercise.
 * @property onFinish Ends the workout and saves what was done.
 * @property onDone Leaves the workout screen.
 */
data class WorkoutActions(
    val onLogSet: (value: Int, repsInReserve: Int?, addedKg: Double, assistanceKg: Double) -> Unit,
    val onEndRest: () -> Unit,
    val onSkipExercise: () -> Unit,
    val onFinish: () -> Unit,
    val onDone: () -> Unit,
)

/**
 * The workout in progress, laid out as the Live set artboard: the set as one
 * huge number, how many were left in the tank, what comes next, and Log set
 * pinned to the bottom. Then the rest countdown, and a summary at the end.
 */
@Composable
fun WorkoutScreen(uiState: WorkoutUiState, now: () -> Long, actions: WorkoutActions, modifier: Modifier = Modifier) {
    // Nobody wants the screen to lock between sets.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LevitxColors.Background)
            .keepScreenOn()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        when (uiState) {
            WorkoutUiState.Loading, WorkoutUiState.Saving -> Unit

            WorkoutUiState.Unavailable -> Message(stringResource(R.string.data_unavailable), actions.onDone)

            WorkoutUiState.NoWorkout -> Message(stringResource(R.string.workout_nothing), actions.onDone)

            is WorkoutUiState.Active -> {
                SetBar(uiState, actions)
                val restEndsAt = uiState.run.restEndsAtElapsedMillis
                if (restEndsAt != null) Rest(uiState, restEndsAt, now, actions) else NextSet(uiState, actions)
            }

            is WorkoutUiState.Finished -> Summary(uiState, actions.onDone)
        }
    }
}

/**
 * The top bar: back, the sets of this exercise as short segments (done in
 * orange, this one in white), "SET 2/3", and finish once anything is done.
 */
@Composable
private fun SetBar(state: WorkoutUiState.Active, actions: WorkoutActions) {
    val run = state.run
    val plan = state.plan
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(start = 4.dp, end = ScreenPadding),
    ) {
        BackButton(actions.onDone)
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            repeat(plan.sets.size) { index ->
                Box(
                    Modifier
                        .padding(end = 4.dp)
                        .width(18.dp)
                        .height(3.dp)
                        .background(
                            when {
                                index < run.nextSetIndex -> LevitxColors.Accent
                                index == run.nextSetIndex -> LevitxColors.Text
                                else -> LevitxColors.Tick
                            },
                        ),
                )
            }
            Caption(
                stringResource(R.string.workout_set_short, run.nextSetIndex + 1, plan.sets.size),
                color = LevitxColors.Text,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        if (run.hasStarted) {
            Caption(
                text = stringResource(R.string.workout_finish_short),
                color = LevitxColors.TextMuted,
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = actions.onFinish)
                    .padding(vertical = 14.dp),
            )
        } else {
            Caption(stringResource(R.string.workout_exercise_short, run.exerciseIndex + 1, run.session.plans.size))
        }
    }
    Rule()
}

/** The set to do now, with what came before it kept for the next set of the same exercise. */
@Composable
private fun ColumnScope.NextSet(state: WorkoutUiState.Active, actions: WorkoutActions) {
    val run = state.run
    val plan = state.plan
    val set = state.nextSet
    val isReps = plan.exercise.measure == GateMeasure.REPS
    // Keyed on the exercise and set, so each set starts at its target.
    val key = "${run.exerciseIndex}/${run.nextSetIndex}"
    var value by rememberSaveable(key) { mutableIntStateOf(set.target) }
    var repsLeft by rememberSaveable(key) { mutableIntStateOf(if (set.isBestEffort) 0 else plan.repsInReserve) }
    // The run carries the load between sets of an exercise: most people keep the same vest or band.
    var addedKg by rememberSaveable(key) { mutableDoubleStateOf(run.load.addedKg) }
    var bandId by rememberSaveable(key) {
        mutableStateOf(state.bands.firstOrNull { it.assistanceKg == run.load.assistanceKg }?.id)
    }
    val assistanceKg = state.bands.firstOrNull { it.id == bandId }?.assistanceKg ?: 0.0
    val load = loadPerRep(state, addedKg, assistanceKg)

    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState()),
    ) {
        SetHeader(state, load, value, onChange = { value = it })
        if (isReps) {
            Rule(Modifier.padding(top = 22.dp))
            RepsLeft(selected = repsLeft, onSelect = { repsLeft = it })
        }
        if (state.tracksLoad) {
            Rule(Modifier.padding(top = 22.dp))
            LoadChooser(
                state = state,
                addedKg = addedKg,
                onAddedKgChange = { addedKg = it },
                bandId = bandId,
                onBandChange = { bandId = it },
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
        onSkip = actions.onSkipExercise,
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

/** Log set, the main action, and Skip (or Next once a set is in) beside it. */
@Composable
private fun SetButtons(state: WorkoutUiState.Active, hint: String?, onLog: () -> Unit, onSkip: () -> Unit) {
    Row(modifier = Modifier.height(BottomBarHeight)) {
        AccentBar(
            text = stringResource(R.string.workout_log_set),
            hint = hint,
            onClick = onLog,
            height = BottomBarHeight,
            modifier = Modifier.weight(2f),
        )
        val nothingDone = state.run.logged[state.run.exerciseIndex].isEmpty()
        QuietBar(
            text = stringResource(if (nothingDone) R.string.workout_skip_short else R.string.workout_next_short),
            onClick = onSkip,
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

@Preview
@Composable
private fun WorkoutScreenPreview() {
    val model = AthleteModel.starting(
        CoachProfile("first_pull_up", equipment = setOf(Equipment.PULL_UP_BAR)),
        PullUpAnswer.YES,
    )
    val session = checkNotNull(Coach().recommend(model, nowEpochMillis = 0L).session)
    LevitxTheme {
        WorkoutScreen(
            uiState = WorkoutUiState.Active(
                WorkoutRun("preview", session, startedAtEpochMillis = 0L),
                bodyweightKg = 74.2,
                bands = emptyList(),
            ),
            now = { 0L },
            actions = WorkoutActions({ _, _, _, _ -> }, {}, {}, {}, {}),
        )
    }
}
