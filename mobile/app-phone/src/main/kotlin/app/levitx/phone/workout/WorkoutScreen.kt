package app.levitx.phone.workout

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.WorkoutRun
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding

/** The bottom bars, as on the artboard. */
internal val BottomBarHeight = 64.dp

/** Workout destination: connects [WorkoutViewModel] to [WorkoutScreen]. */
@Composable
fun WorkoutRoute(onDone: () -> Unit, viewModel: WorkoutViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WorkoutScreen(
        uiState = uiState,
        now = viewModel.elapsedRealtimeMillis,
        actions = WorkoutActions(
            onLogSet = { value, repsInReserve, addedKg, assistanceKg ->
                viewModel.logSet(value, repsInReserve, addedKg, assistanceKg)
            },
            onChooseLoad = viewModel::chooseLoad,
            onRest = viewModel::rest,
            onTimer = viewModel::timer,
            onGate = viewModel::gate,
            onToggleSounds = viewModel::toggleSounds,
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
 * @property onChooseLoad Sets the vest or belt and band help, in kg, for the sets to come.
 * @property onRest Ends rest, changes its length, or takes back the set that started it.
 * @property onTimer Starts, stops or cancels the hold or negative timer.
 * @property onGate Puts off a gate attempt, confirms or takes back a gate-passing set, or carries on after a pass.
 * @property onToggleSounds Turns timer beeps on or off.
 * @property onSkipExercise Moves on to the next exercise.
 * @property onFinish Ends the workout and saves what was done.
 * @property onDone Leaves the workout screen. The workout stays saved as a draft to come back to.
 */
data class WorkoutActions(
    val onLogSet: (value: Int, repsInReserve: Int?, addedKg: Double, assistanceKg: Double) -> Unit,
    val onChooseLoad: (addedKg: Double, assistanceKg: Double) -> Unit,
    val onRest: (RestAction) -> Unit,
    val onTimer: (TimerAction) -> Unit,
    val onGate: (GateAction) -> Unit,
    val onToggleSounds: () -> Unit,
    val onSkipExercise: () -> Unit,
    val onFinish: () -> Unit,
    val onDone: () -> Unit,
) {
    /** Ready-made actions. */
    companion object {
        /** Actions that do nothing, for previews and screenshots. */
        val None = WorkoutActions({ _, _, _, _ -> }, { _, _ -> }, {}, {}, {}, {}, {}, {}, {})
    }
}

/**
 * The workout in progress, laid out as the Live set artboard: the set as one
 * huge number, how many were left in the tank, what comes next, and Log set
 * pinned to the bottom. Then the hold and negative timers, the rest
 * countdown, and a summary at the end.
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
            is WorkoutUiState.Active -> ActiveWorkout(uiState, now, actions)
            is WorkoutUiState.Finished -> Summary(uiState.report, actions.onDone)
        }
    }
}

/**
 * The set, timer, rest or gate screen, with the session menu or the gate
 * check over it when open.
 */
@Composable
private fun ColumnScope.ActiveWorkout(state: WorkoutUiState.Active, now: () -> Long, actions: WorkoutActions) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    // The gate brief shows once for each attempt.
    var briefSeen by rememberSaveable(state.run.exerciseIndex) { mutableStateOf(false) }
    val timer = state.timer
    val passed = state.passed
    val restEndsAt = state.run.restEndsAtElapsedMillis
    val showBrief = state.plan.isGateAttempt && state.run.logged[state.run.exerciseIndex].isEmpty() && !briefSeen
    // Back closes whatever is over the set, then stops a running timer, before it leaves the workout.
    BackHandler(enabled = menuOpen) { menuOpen = false }
    BackHandler(enabled = !menuOpen && state.pending != null) { actions.onGate(GateAction.RECOUNT) }
    BackHandler(enabled = !menuOpen && timer != null) { actions.onTimer(TimerAction.CANCEL) }
    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SetBar(state, onMenu = { menuOpen = true })
            when {
                passed != null -> GatePassed(state, passed, actions.onGate)
                timer != null -> TimerPanel(state, timer, now, actions.onTimer)
                restEndsAt != null -> Rest(state, restEndsAt, now, actions)
                showBrief -> GateBrief(state, onReady = { briefSeen = true }, onGate = actions.onGate)
                else -> NextSet(state, actions)
            }
        }
        if (state.pending != null) StrictCheck(state, actions.onGate)
        if (menuOpen) SessionMenu(state, actions, onClose = { menuOpen = false })
    }
}

/**
 * The top bar: the session menu, the sets of this exercise as short segments
 * (done in orange, this one in white), "SET 2/3", and where this exercise
 * sits in the session.
 */
@Composable
private fun SetBar(state: WorkoutUiState.Active, onMenu: () -> Unit) {
    val run = state.run
    val plan = state.plan
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(start = 4.dp, end = ScreenPadding),
    ) {
        MenuButton(onMenu)
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
        Caption(stringResource(R.string.workout_exercise_short, run.exerciseIndex + 1, run.session.plans.size))
    }
    Rule()
}

/** Opens the session menu: skip, sounds, leave, or end and save. */
@Composable
private fun MenuButton(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.workout_menu), onClick = onClick),
    ) {
        Icon(
            painterResource(R.drawable.ic_menu),
            contentDescription = stringResource(R.string.workout_menu),
            tint = LevitxColors.Text,
        )
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
            actions = WorkoutActions.None,
        )
    }
}
