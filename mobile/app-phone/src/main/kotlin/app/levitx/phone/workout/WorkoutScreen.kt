package app.levitx.phone.workout

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.coach.AthleteModel
import app.levitx.core.coach.Coach
import app.levitx.core.coach.CoachProfile
import app.levitx.core.coach.Effort
import app.levitx.core.coach.Equipment
import app.levitx.core.coach.PullUpAnswer
import app.levitx.core.coach.WorkoutRun
import app.levitx.core.load.AddedWeight
import app.levitx.core.load.BandAssistance
import app.levitx.core.load.LoadLimits
import app.levitx.core.load.LoadModifier
import app.levitx.core.load.calculateEffectiveLoad
import app.levitx.core.load.formatKg
import app.levitx.core.skills.GateMeasure
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxSpacing
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.ChoiceButton
import app.levitx.phone.ui.DecimalInput
import app.levitx.phone.ui.NumberInput
import app.levitx.phone.ui.PrimaryButton
import app.levitx.phone.ui.SecondaryButton
import app.levitx.phone.ui.SectionLabel
import app.levitx.phone.ui.measureText
import kotlinx.coroutines.delay

/** How often the rest countdown redraws. A quarter second keeps the seconds honest. */
private const val COUNTDOWN_TICK_MILLIS = 250L
private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60

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
 * @property onLogSet Records a set: reps or seconds, effort (null for holds), added weight and band help in kg.
 * @property onEndRest Ends rest early, or when it runs out.
 * @property onSkipExercise Moves on to the next exercise.
 * @property onFinish Ends the workout and saves what was done.
 * @property onDone Leaves the workout screen.
 */
data class WorkoutActions(
    val onLogSet: (value: Int, effort: Effort?, addedKg: Double, assistanceKg: Double) -> Unit,
    val onEndRest: () -> Unit,
    val onSkipExercise: () -> Unit,
    val onFinish: () -> Unit,
    val onDone: () -> Unit,
)

/** The workout in progress: the next set, the rest countdown, and a summary at the end. */
@Composable
fun WorkoutScreen(uiState: WorkoutUiState, now: () -> Long, actions: WorkoutActions, modifier: Modifier = Modifier) {
    // Nobody wants the screen to lock between sets.
    Scaffold(modifier = modifier.fillMaxSize().keepScreenOn()) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(LevitxSpacing.Medium),
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LevitxSpacing.Medium, vertical = LevitxSpacing.Large),
        ) {
            when (uiState) {
                WorkoutUiState.Loading, WorkoutUiState.Saving -> Unit

                WorkoutUiState.Unavailable -> Text(stringResource(R.string.data_unavailable))

                WorkoutUiState.NoWorkout -> {
                    Text(stringResource(R.string.workout_nothing), style = MaterialTheme.typography.titleLarge)
                    PrimaryButton(text = stringResource(R.string.workout_back), onClick = actions.onDone)
                }

                is WorkoutUiState.Active -> {
                    val restEndsAt = uiState.run.restEndsAtElapsedMillis
                    if (restEndsAt != null) Rest(uiState, restEndsAt, now, actions) else NextSet(uiState, actions)
                }

                is WorkoutUiState.Finished -> Summary(uiState, actions.onDone)
            }
        }
    }
}

/** Which exercise and set this is, its target, and how hard to push. */
@Composable
private fun SetHeader(state: WorkoutUiState.Active) {
    val run = state.run
    val plan = state.plan
    SectionLabel(
        stringResource(R.string.workout_exercise_label, run.exerciseIndex + 1, run.session.plans.size),
        color = LevitxColors.Accent,
    )
    Text(plan.exercise.name, style = MaterialTheme.typography.headlineSmall)
    SectionLabel(stringResource(R.string.workout_set_label, run.nextSetIndex + 1, plan.sets.size))
    Text(text = measureText(plan.exercise.measure, state.nextSet.target), style = LevitxType.Display)
    Text(
        text = when {
            !plan.isHard -> stringResource(R.string.workout_light_instructions)
            state.nextSet.isBestEffort -> stringResource(R.string.workout_best_effort_instructions)
            else -> pluralStringResource(R.plurals.workout_work_instructions, plan.repsInReserve, plan.repsInReserve)
        },
        style = MaterialTheme.typography.bodyLarge,
        color = LevitxColors.TextSoft,
    )
}

@Composable
private fun NextSet(state: WorkoutUiState.Active, actions: WorkoutActions) {
    val run = state.run
    val set = state.nextSet
    val isReps = state.plan.exercise.measure == GateMeasure.REPS
    // Keyed on the exercise and set, so each set starts at its target.
    val key = "${run.exerciseIndex}/${run.nextSetIndex}"
    var value by rememberSaveable(key) { mutableIntStateOf(set.target) }
    val defaultEffort = when {
        !state.plan.isHard -> Effort.EASY
        set.isBestEffort -> Effort.HARD
        else -> Effort.RIGHT
    }
    var effort by rememberSaveable(key) { mutableStateOf(defaultEffort) }
    // The run carries the load between sets of an exercise: most people keep the same vest or band.
    var addedKg by rememberSaveable(key) { mutableDoubleStateOf(run.load.addedKg) }
    var bandId by rememberSaveable(key) {
        mutableStateOf(state.bands.firstOrNull { it.assistanceKg == run.load.assistanceKg }?.id)
    }
    val assistanceKg = state.bands.firstOrNull { it.id == bandId }?.assistanceKg ?: 0.0

    SetHeader(state)
    NumberInput(
        value = value,
        onValueChange = { value = it },
        label = stringResource(if (isReps) R.string.input_reps_done else R.string.input_seconds_held),
    )
    if (isReps) EffortChooser(effort) { effort = it }
    if (state.tracksLoad) {
        LoadChooser(
            state = state,
            addedKg = addedKg,
            onAddedKgChange = { addedKg = it },
            bandId = bandId,
            onBandChange = { bandId = it },
            assistanceKg = assistanceKg,
        )
    }
    Spacer(Modifier.height(LevitxSpacing.Small))
    PrimaryButton(
        text = stringResource(R.string.workout_log_set),
        // Holds have no reps left in the tank, so no effort is recorded for them.
        onClick = { actions.onLogSet(value, effort.takeIf { isReps }, addedKg, assistanceKg) },
    )
    val nothingDone = run.logged[run.exerciseIndex].isEmpty()
    SecondaryButton(
        text = stringResource(if (nothingDone) R.string.workout_skip_exercise else R.string.workout_next_exercise),
        onClick = actions.onSkipExercise,
    )
    if (run.hasStarted) TextButton(onClick = actions.onFinish) { Text(stringResource(R.string.workout_finish)) }
}

/** How the set felt, as one tap. Stored as reps left in the tank. */
@Composable
private fun EffortChooser(selected: Effort, onSelect: (Effort) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(LevitxSpacing.Small)) {
        SectionLabel(stringResource(R.string.workout_effort_label))
        Row(horizontalArrangement = Arrangement.spacedBy(LevitxSpacing.Small), modifier = Modifier.fillMaxWidth()) {
            ChoiceButton(stringResource(R.string.workout_effort_easy), selected == Effort.EASY) {
                onSelect(Effort.EASY)
            }
            ChoiceButton(stringResource(R.string.workout_effort_right), selected == Effort.RIGHT) {
                onSelect(Effort.RIGHT)
            }
            ChoiceButton(stringResource(R.string.workout_effort_hard), selected == Effort.HARD) {
                onSelect(Effort.HARD)
            }
        }
    }
}

/**
 * Vest or belt weight, and which band (if any), with the load per rep they
 * add up to. The load is the number that makes progress visible when the reps
 * stay the same but the help goes down or the weight goes up.
 */
@Composable
private fun LoadChooser(
    state: WorkoutUiState.Active,
    addedKg: Double,
    onAddedKgChange: (Double) -> Unit,
    bandId: String?,
    onBandChange: (String?) -> Unit,
    assistanceKg: Double,
) {
    DecimalInput(
        value = addedKg,
        onValueChange = onAddedKgChange,
        label = stringResource(R.string.workout_added_weight_label),
        max = LoadLimits.MAX_MODIFIER_KG,
    )
    if (state.bands.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(LevitxSpacing.Small)) {
            SectionLabel(stringResource(R.string.workout_band_label))
            Row(
                horizontalArrangement = Arrangement.spacedBy(LevitxSpacing.Small),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                FilterChip(
                    selected = bandId == null,
                    onClick = { onBandChange(null) },
                    label = { Text(stringResource(R.string.workout_band_none)) },
                )
                state.bands.forEach { band ->
                    FilterChip(
                        selected = bandId == band.id,
                        onClick = { onBandChange(band.id) },
                        label = {
                            Text(stringResource(R.string.workout_band_choice, band.name, formatKg(band.assistanceKg)))
                        },
                    )
                }
            }
        }
    }
    val profile = state.plan.exercise.loadProfile
    val bodyweight = state.bodyweightKg
    if (profile != null && bodyweight != null) {
        val modifiers = buildList<LoadModifier> {
            if (addedKg > 0.0) add(AddedWeight(addedKg))
            if (assistanceKg > 0.0) add(BandAssistance(assistanceKg))
        }
        val load = calculateEffectiveLoad(bodyweight, profile, modifiers)
        Text(
            text = if (load.isEstimate || assistanceKg > 0.0) {
                stringResource(R.string.workout_load_estimate, formatKg(load.totalKg))
            } else {
                stringResource(R.string.workout_load, formatKg(load.totalKg))
            },
            style = MaterialTheme.typography.titleMedium,
        )
    }
    if (assistanceKg > 0.0 && state.plan.gateId != null) {
        Text(
            text = stringResource(R.string.workout_band_note),
            style = MaterialTheme.typography.bodyMedium,
            color = LevitxColors.TextMuted,
        )
    }
}

@Composable
private fun Rest(state: WorkoutUiState.Active, endsAt: Long, now: () -> Long, actions: WorkoutActions) {
    val haptics = LocalHapticFeedback.current
    var remainingMillis by remember(endsAt) { mutableLongStateOf((endsAt - now()).coerceAtLeast(0)) }
    // Counts against the device's elapsed clock, not by adding up ticks, so it
    // stays right even if the app was in the background for part of the rest.
    LaunchedEffect(endsAt) {
        while (true) {
            remainingMillis = (endsAt - now()).coerceAtLeast(0)
            if (remainingMillis == 0L) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                actions.onEndRest()
                break
            }
            delay(COUNTDOWN_TICK_MILLIS)
        }
    }
    val totalSeconds = ((remainingMillis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
    val plan = state.plan
    SectionLabel(stringResource(R.string.workout_rest_label), color = LevitxColors.Ice)
    Text(
        text = stringResource(
            R.string.clock_format,
            totalSeconds / SECONDS_PER_MINUTE,
            totalSeconds % SECONDS_PER_MINUTE,
        ),
        style = LevitxType.Numeral.copy(fontSize = LevitxType.Display.fontSize),
        // A polite live region lets screen readers hear the countdown without
        // it interrupting whatever they are reading.
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
    Text(
        text = stringResource(
            R.string.workout_rest_next,
            plan.exercise.name,
            state.run.nextSetIndex + 1,
            plan.sets.size,
            measureText(plan.exercise.measure, state.nextSet.target),
        ),
        style = MaterialTheme.typography.bodyLarge,
        color = LevitxColors.TextSoft,
    )
    Spacer(Modifier.height(LevitxSpacing.Small))
    SecondaryButton(text = stringResource(R.string.workout_skip_rest), onClick = actions.onEndRest)
    TextButton(onClick = actions.onFinish) { Text(stringResource(R.string.workout_finish)) }
}

@Composable
private fun Summary(state: WorkoutUiState.Finished, onDone: () -> Unit) {
    SectionLabel(stringResource(R.string.workout_saved_label), color = LevitxColors.Accent)
    Text(text = pluralStringResource(R.plurals.workout_saved_sets, state.sets, state.sets), style = LevitxType.Display)
    state.newBest?.let { best ->
        Text(
            text = stringResource(R.string.workout_new_best, best.gateTitle, measureText(best.measure, best.value)),
            style = MaterialTheme.typography.titleMedium,
        )
    }
    Text(
        text = stringResource(R.string.workout_saved_body),
        style = MaterialTheme.typography.bodyLarge,
        color = LevitxColors.TextSoft,
    )
    Spacer(Modifier.height(LevitxSpacing.Small))
    PrimaryButton(text = stringResource(R.string.workout_done), onClick = onDone)
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
