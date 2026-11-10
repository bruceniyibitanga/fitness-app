package app.levitx.phone.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.session.LoggedSet
import app.levitx.core.session.SessionPlanner
import app.levitx.core.skills.GateMeasure
import app.levitx.core.skills.SkillCatalogue
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxSpacing
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.NumberInput
import app.levitx.phone.ui.PrimaryButton
import app.levitx.phone.ui.SecondaryButton
import app.levitx.phone.ui.SectionLabel
import app.levitx.phone.ui.measureText
import kotlinx.coroutines.delay

/** Reps-in-reserve choices. The last one means "4 or more". */
private val ReserveChoices = 0..4

/** How often the rest countdown redraws. A quarter second keeps the seconds honest. */
private const val COUNTDOWN_TICK_MILLIS = 250L
private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60

/** Session destination: connects [SessionViewModel] to [SessionScreen]. */
@Composable
fun SessionRoute(onDone: () -> Unit, viewModel: SessionViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SessionScreen(
        uiState = uiState,
        now = viewModel::elapsedRealtimeMillis,
        onLogSet = viewModel::logSet,
        onEndRest = viewModel::endRest,
        onFinish = viewModel::finish,
        onDone = onDone,
    )
}

/** The live session: the next set, the rest countdown, and a summary at the end. */
@Composable
fun SessionScreen(
    uiState: SessionUiState,
    now: () -> Long,
    onLogSet: (value: Int, repsInReserve: Int?) -> Unit,
    onEndRest: () -> Unit,
    onFinish: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Nobody wants the screen to lock between sets.
    Scaffold(modifier = modifier.fillMaxSize().keepScreenOn()) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(LevitxSpacing.Medium),
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LevitxSpacing.Medium, vertical = LevitxSpacing.XLarge),
        ) {
            when (uiState) {
                SessionUiState.Loading -> Unit
                SessionUiState.Unavailable -> Text(stringResource(R.string.data_unavailable))
                SessionUiState.NothingToTrain -> {
                    Text(stringResource(R.string.session_nothing), style = MaterialTheme.typography.titleLarge)
                    PrimaryButton(stringResource(R.string.session_back_home), onDone)
                }
                is SessionUiState.Active -> if (uiState.restEndsAtElapsedMillis != null) {
                    Rest(uiState, uiState.restEndsAtElapsedMillis, now, onEndRest, onFinish)
                } else {
                    NextSet(uiState, onLogSet, onFinish)
                }
                is SessionUiState.Finished -> Summary(uiState, onDone)
            }
        }
    }
}

@Composable
private fun NextSet(state: SessionUiState.Active, onLogSet: (Int, Int?) -> Unit, onFinish: () -> Unit) {
    val set = state.nextSet
    val measure = state.plan.gate.measure
    // Keyed on the set so each set starts at its target.
    var value by rememberSaveable(state.setIndex) { mutableIntStateOf(set.target) }
    val defaultReserve = if (set.isBestEffort) 0 else state.plan.repsInReserve
    var reserve by rememberSaveable(state.setIndex) { mutableIntStateOf(defaultReserve) }

    SectionLabel(stringResource(R.string.session_set_label, state.setIndex + 1, state.plan.sets.size))
    Text(text = measureText(measure, set.target), style = LevitxType.Display)
    Text(text = state.plan.gate.title, style = MaterialTheme.typography.titleMedium)
    Text(
        text = if (set.isBestEffort) {
            stringResource(R.string.session_best_effort_instructions)
        } else {
            pluralStringResource(R.plurals.session_work_instructions, state.plan.repsInReserve, state.plan.repsInReserve)
        },
        style = MaterialTheme.typography.bodyLarge,
        color = LevitxColors.TextSoft,
    )

    NumberInput(
        value = value,
        onValueChange = { value = it },
        label = stringResource(if (measure == GateMeasure.REPS) R.string.input_reps_done else R.string.input_seconds_held),
    )
    if (measure == GateMeasure.REPS) {
        ReserveChooser(selected = reserve, onSelect = { reserve = it })
    }

    Spacer(Modifier.height(LevitxSpacing.Small))
    PrimaryButton(
        text = stringResource(R.string.session_log_set),
        onClick = { onLogSet(value, if (measure == GateMeasure.REPS) reserve else null) },
    )
    LoggedSoFar(state)
    if (state.logged.isNotEmpty()) {
        TextButton(onClick = onFinish) { Text(stringResource(R.string.session_end_early)) }
    }
}

/** "Reps left in the tank" as one tap: 0, 1, 2, 3 or 4+. */
@Composable
private fun ReserveChooser(selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(LevitxSpacing.Small)) {
        SectionLabel(stringResource(R.string.session_reserve_label))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ReserveChoices.forEachIndexed { index, choice ->
                SegmentedButton(
                    selected = selected == choice,
                    onClick = { onSelect(choice) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ReserveChoices.count()),
                    label = {
                        Text(if (choice == ReserveChoices.last) stringResource(R.string.session_reserve_max, choice) else "$choice")
                    },
                )
            }
        }
    }
}

@Composable
private fun Rest(
    state: SessionUiState.Active,
    endsAt: Long,
    now: () -> Long,
    onEndRest: () -> Unit,
    onFinish: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var remainingMillis by remember(endsAt) { mutableLongStateOf((endsAt - now()).coerceAtLeast(0)) }
    // Counts against the device's elapsed clock, not by adding up ticks, so it
    // stays right even if the app was in the background for part of the rest.
    LaunchedEffect(endsAt) {
        while (true) {
            remainingMillis = (endsAt - now()).coerceAtLeast(0)
            if (remainingMillis == 0L) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onEndRest()
                break
            }
            delay(COUNTDOWN_TICK_MILLIS)
        }
    }
    val totalSeconds = ((remainingMillis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()

    SectionLabel(stringResource(R.string.session_rest_label), color = LevitxColors.Ice)
    Text(
        text = stringResource(R.string.clock_format, totalSeconds / SECONDS_PER_MINUTE, totalSeconds % SECONDS_PER_MINUTE),
        style = LevitxType.Numeral.copy(fontSize = LevitxType.Display.fontSize),
        // A polite live region lets screen readers hear the countdown without
        // it interrupting whatever they are reading.
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
    Text(
        text = stringResource(
            R.string.session_rest_next,
            state.setIndex + 1,
            state.plan.sets.size,
            measureText(state.plan.gate.measure, state.nextSet.target),
        ),
        style = MaterialTheme.typography.bodyLarge,
        color = LevitxColors.TextSoft,
    )
    Spacer(Modifier.height(LevitxSpacing.Small))
    SecondaryButton(stringResource(R.string.session_skip_rest), onEndRest)
    LoggedSoFar(state)
    TextButton(onClick = onFinish) { Text(stringResource(R.string.session_end_early)) }
}

/** The sets done so far, one line each. */
@Composable
private fun LoggedSoFar(state: SessionUiState.Active) {
    if (state.logged.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(LevitxSpacing.XSmall)) {
        SectionLabel(stringResource(R.string.session_done_so_far))
        state.logged.forEachIndexed { index, set ->
            val amount = measureText(state.plan.gate.measure, set.value)
            Text(
                text = set.repsInReserve?.let { stringResource(R.string.session_logged_with_reserve, index + 1, amount, it) }
                    ?: stringResource(R.string.session_logged, index + 1, amount),
                style = MaterialTheme.typography.bodyMedium,
                color = LevitxColors.TextMuted,
            )
        }
    }
}

@Composable
private fun Summary(state: SessionUiState.Finished, onDone: () -> Unit) {
    val measure = state.gate.measure
    SectionLabel(stringResource(R.string.session_summary_label))
    Text(
        text = if (state.isNewBest) {
            stringResource(R.string.session_new_best, measureText(measure, state.sessionBest))
        } else {
            stringResource(R.string.session_best_today, measureText(measure, state.sessionBest))
        },
        style = LevitxType.Display,
    )
    Text(
        text = when {
            state.passedGate -> stringResource(R.string.session_gate_passed, state.gate.title)
            state.isNewBest -> stringResource(R.string.session_progress, measureText(measure, state.gate.target))
            else -> stringResource(R.string.session_keep_going, measureText(measure, state.previousBest))
        },
        style = MaterialTheme.typography.bodyLarge,
        color = LevitxColors.TextSoft,
    )
    Spacer(Modifier.height(LevitxSpacing.Small))
    PrimaryButton(stringResource(R.string.session_done), onDone)
}

@Preview
@Composable
private fun SessionScreenPreview() {
    val gate = SkillCatalogue.muscleUp.gates[2]
    LevitxTheme {
        SessionScreen(
            uiState = SessionUiState.Active(
                plan = SessionPlanner.planFor(gate, best = 7),
                setIndex = 1,
                logged = listOf(LoggedSet(5, 2, isBestEffort = false)),
                restEndsAtElapsedMillis = null,
            ),
            now = { 0L },
            onLogSet = { _, _ -> },
            onEndRest = {},
            onFinish = {},
            onDone = {},
        )
    }
}
