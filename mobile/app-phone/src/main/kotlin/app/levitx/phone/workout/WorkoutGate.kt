package app.levitx.phone.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.coach.ExerciseGuides
import app.levitx.core.skills.Gate
import app.levitx.core.skills.GateMeasure
import app.levitx.core.skills.SkillCatalogue
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.QuietBar
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.SquareNote
import app.levitx.phone.ui.measureText

/** How much the scrim dims the set behind the check. */
private const val SCRIM_ALPHA = 0.72f

/**
 * Before a gate attempt: what the gate asks for, the standard a rep has to
 * meet, and a reminder to warm up. "Not today" moves the attempt to the end
 * of the exercise as an ordinary best effort, so nothing is lost.
 */
@Composable
internal fun ColumnScope.GateBrief(state: WorkoutUiState.Active, onReady: () -> Unit, onGate: (GateAction) -> Unit) {
    val gate = state.gate ?: return
    val best = state.gateBests[gate.id] ?: 0
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(start = ScreenPadding, end = ScreenPadding, top = 22.dp, bottom = 20.dp),
    ) {
        Caption(gatePlace(gate))
        Caption(
            stringResource(R.string.gate_test_label),
            color = LevitxColors.Accent,
            modifier = Modifier.padding(top = 18.dp),
        )
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 10.dp)) {
            Text(
                text = gate.target.toString(),
                style = LevitxType.Hero.copy(fontSize = 124.sp, lineHeight = 100.sp),
                color = LevitxColors.Text,
            )
            Column(modifier = Modifier.padding(start = 14.dp, bottom = 8.dp)) {
                Text(
                    text = state.plan.exercise.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = LevitxColors.Text,
                    modifier = Modifier.semantics { heading() },
                )
                Caption(stringResource(R.string.gate_test_best, measureText(gate.measure, best)))
            }
        }
        Text(
            text = stringResource(R.string.gate_test_body),
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = LevitxColors.TextSoft,
            modifier = Modifier.padding(top = 18.dp),
        )
        Rule(Modifier.padding(top = 22.dp))
        Standard(gate, state.plan.exercise.id, modifier = Modifier.padding(top = 16.dp))
        SquareNote(
            title = stringResource(R.string.gate_test_warm_up_title),
            text = stringResource(R.string.gate_test_warm_up),
            squareColor = LevitxColors.Ice,
            modifier = Modifier.padding(top = 22.dp),
        )
    }
    BriefButtons(onReady, onGate)
}

/** Warm up, then test; or put the attempt off. */
@Composable
private fun BriefButtons(onReady: () -> Unit, onGate: (GateAction) -> Unit) {
    Row(modifier = Modifier.height(BottomBarHeight)) {
        AccentBar(
            text = stringResource(R.string.gate_test_ready),
            onClick = onReady,
            height = BottomBarHeight,
            modifier = Modifier.weight(2f),
        )
        QuietBar(
            text = stringResource(R.string.gate_test_not_today),
            onClick = { onGate(GateAction.DEFER) },
            modifier = Modifier
                .weight(1f)
                .height(BottomBarHeight),
        )
    }
}

/**
 * Over the set, when it would pass a gate: did every rep meet the standard?
 * Yes logs it and passes the gate; no goes back to correct the count.
 */
@Composable
internal fun BoxScope.StrictCheck(state: WorkoutUiState.Active, onGate: (GateAction) -> Unit) {
    val gate = state.gate ?: return
    val isHold = gate.measure == GateMeasure.HOLD_SECONDS
    val title = stringResource(if (isHold) R.string.gate_check_title_hold else R.string.gate_check_title_reps)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LevitxColors.Background.copy(alpha = SCRIM_ALPHA))
            .clickable(interactionSource = null, indication = null, onClick = { onGate(GateAction.RECOUNT) }),
    )
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(LevitxColors.Surface)
            .semantics {
                paneTitle = title
                isTraversalGroup = true
            },
    ) {
        Rule()
        Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 18.dp, bottom = 20.dp)) {
            Caption(stringResource(R.string.gate_check_label, gate.title), color = LevitxColors.Accent)
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = LevitxColors.Text,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .semantics { heading() },
            )
            Standard(gate, state.plan.exercise.id, modifier = Modifier.padding(top = 14.dp))
        }
        Row(modifier = Modifier.height(BottomBarHeight)) {
            QuietBar(
                text = stringResource(R.string.gate_check_no),
                onClick = { onGate(GateAction.RECOUNT) },
                modifier = Modifier
                    .weight(1f)
                    .height(BottomBarHeight),
            )
            AccentBar(
                text = stringResource(R.string.gate_check_yes),
                onClick = { onGate(GateAction.CONFIRM) },
                height = BottomBarHeight,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * The standard a rep (or a hold) has to meet: the exercise's form cues,
 * numbered, and where the count stops.
 */
@Composable
private fun Standard(gate: Gate, exerciseId: String, modifier: Modifier = Modifier) {
    val isHold = gate.measure == GateMeasure.HOLD_SECONDS
    val cues = ExerciseGuides.forExercise(exerciseId)?.cues.orEmpty() +
        stringResource(if (isHold) R.string.gate_standard_stop_hold else R.string.gate_standard_stop_reps)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        Caption(stringResource(R.string.gate_standard))
        cues.forEachIndexed { index, cue ->
            Row {
                Caption(
                    text = (index + 1).toString().padStart(2, '0'),
                    color = LevitxColors.Accent,
                    modifier = Modifier
                        .width(32.dp)
                        .padding(top = 2.dp),
                )
                Text(text = cue, fontSize = 14.sp, lineHeight = 20.sp, color = LevitxColors.Text)
            }
        }
    }
}

/** "GATE 3 OF 6 · MUSCLE-UP", or just the gate's title when it isn't on a known path. */
@Composable
internal fun gatePlace(gate: Gate): String {
    val path = SkillCatalogue.all.firstOrNull { gate in it.gates }
    return if (path == null) {
        gate.title
    } else {
        stringResource(R.string.gate_place, path.gates.indexOf(gate) + 1, path.gates.size, path.name)
    }
}
