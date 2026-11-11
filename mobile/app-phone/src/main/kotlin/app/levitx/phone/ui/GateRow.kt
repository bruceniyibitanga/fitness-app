package app.levitx.phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.skills.Gate
import app.levitx.core.skills.GateMeasure
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R

/*
 * A skill path drawn as a rail, as on the "Goal path" artboard: a line down
 * the left joining a square for each gate. Passed gates are filled, the one
 * being worked on is a larger orange square, and the goal is an outline.
 */

private val RowHeight = 60.dp
private val RailWidth = 28.dp
private val RailLine = 2.dp
private val MarkSize = 10.dp
private val CurrentMarkSize = 14.dp
private val MarkBorder = 2.dp
private val SelectedEdge = 2.dp

/** Where a gate sits on the person's path. */
enum class GateStatus {
    /** Passed. */
    PASSED,

    /** Being worked on. */
    CURRENT,

    /** Still to come. */
    LOCKED,
}

/** The status of the gate at [index] when the person is on the gate at [currentIndex] (null once all are passed). */
fun gateStatus(index: Int, currentIndex: Int?): GateStatus = when {
    currentIndex == null || index < currentIndex -> GateStatus.PASSED
    index == currentIndex -> GateStatus.CURRENT
    else -> GateStatus.LOCKED
}

/**
 * One gate on the rail: its square, title, a line on where the person is
 * with it, and its target on the right. Tapping selects it, and the selected
 * row gets a lighter background and a cream edge. Shared by the Plan tab and
 * the program screen.
 *
 * @param isFirst True for the first gate, where the rail starts.
 * @param isLast True for the last gate, the skill itself, where the rail ends.
 */
@Composable
fun GateRow(
    gate: Gate,
    best: Int,
    status: GateStatus,
    isFirst: Boolean,
    isLast: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val statusText = gateStatusText(status, best, gate, isLast)
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = RowHeight)
            .background(if (selected) LevitxColors.Surface else Color.Transparent)
            .drawBehind {
                if (selected) drawRect(LevitxColors.Text, size = size.copy(width = SelectedEdge.toPx()))
            }
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics(mergeDescendants = true) { stateDescription = statusText }
            .padding(start = 16.dp, end = ScreenPadding),
    ) {
        Rail(status, isFirst, isLast)
        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.weight(1f).padding(vertical = 10.dp),
        ) {
            Text(gate.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = LevitxColors.Text)
            Text(
                text = statusText.uppercase(),
                style = LevitxType.Micro,
                color = if (status == GateStatus.CURRENT) LevitxColors.Accent else LevitxColors.TextFaint,
            )
        }
        Text(
            text = gateTarget(gate, best, status),
            style = LevitxType.Figure.copy(fontSize = 17.sp),
            color = when (status) {
                GateStatus.CURRENT -> LevitxColors.Accent
                GateStatus.PASSED -> LevitxColors.Text
                GateStatus.LOCKED -> if (isLast) LevitxColors.Text else LevitxColors.TextFaint
            },
        )
    }
}

/** The rail beside a gate: the line through it and its square. */
@Composable
private fun Rail(status: GateStatus, isFirst: Boolean, isLast: Boolean) {
    val lineColor = if (status == GateStatus.PASSED) LevitxColors.Text else LevitxColors.Line
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(RailWidth)
            .fillMaxHeight()
            .drawBehind {
                val x = size.width / 2
                val top = if (isFirst) size.height / 2 else 0f
                val bottom = if (isLast) size.height / 2 else size.height
                drawLine(lineColor, Offset(x, top), Offset(x, bottom), strokeWidth = RailLine.toPx())
            },
    ) {
        when {
            status == GateStatus.PASSED -> Box(Modifier.size(MarkSize).background(LevitxColors.Text))

            status == GateStatus.CURRENT -> Box(Modifier.size(CurrentMarkSize).background(LevitxColors.Accent))

            else -> Box(
                Modifier
                    .size(MarkSize)
                    .background(LevitxColors.Background)
                    .border(MarkBorder, if (isLast) LevitxColors.Text else LevitxColors.Tick),
            )
        }
    }
}

/** "Passed · best 45 s", "Best 7 · training now", "Later" or "Your goal". */
@Composable
private fun gateStatusText(status: GateStatus, best: Int, gate: Gate, isLast: Boolean): String = when {
    status == GateStatus.PASSED -> stringResource(R.string.gate_passed_best, measureText(gate.measure, best))
    status == GateStatus.CURRENT && best > 0 -> stringResource(R.string.gate_current_best, best)
    status == GateStatus.CURRENT -> stringResource(R.string.gate_current)
    isLast -> stringResource(R.string.gate_goal)
    else -> stringResource(R.string.gate_later)
}

/** "×10" or "45 s"; for the gate being worked on, "7/10". */
@Composable
private fun gateTarget(gate: Gate, best: Int, status: GateStatus): String {
    val unit = if (gate.measure == GateMeasure.HOLD_SECONDS) stringResource(R.string.seconds_unit) else ""
    return when {
        status == GateStatus.CURRENT -> stringResource(R.string.gate_progress, best, gate.target, unit)
        gate.measure == GateMeasure.REPS -> stringResource(R.string.gate_reps_target, gate.target)
        else -> stringResource(R.string.seconds_short, gate.target)
    }
}
