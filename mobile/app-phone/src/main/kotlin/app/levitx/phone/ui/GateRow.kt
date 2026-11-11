package app.levitx.phone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.levitx.core.skills.Gate
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxSpacing
import app.levitx.design.LevitxType
import app.levitx.phone.R

private val GateNumberWidth = 40.dp

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
 * One gate of a skill path: its number, title, status and the person's best.
 * Shared by the Plan tab and the program screen.
 */
@Composable
fun GateRow(number: Int, gate: Gate, best: Int, status: GateStatus) {
    val statusText = stringResource(
        when (status) {
            GateStatus.PASSED -> R.string.path_status_passed
            GateStatus.CURRENT -> R.string.path_status_current
            GateStatus.LOCKED -> R.string.path_status_locked
        },
    )
    val titleColor = if (status == GateStatus.LOCKED) LevitxColors.TextMuted else LevitxColors.Text
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = LevitxSpacing.Medium)
            .semantics(mergeDescendants = true) { stateDescription = statusText },
    ) {
        Text(
            text = number.toString().padStart(2, '0'),
            style = LevitxType.Label,
            color = if (status == GateStatus.CURRENT) LevitxColors.Accent else LevitxColors.TextFaint,
            modifier = Modifier.width(GateNumberWidth),
        )
        Column(verticalArrangement = Arrangement.spacedBy(LevitxSpacing.XSmall)) {
            Text(gate.title, style = MaterialTheme.typography.titleMedium, color = titleColor)
            SectionLabel(
                text = statusText,
                color = if (status == GateStatus.CURRENT) LevitxColors.Accent else LevitxColors.TextMuted,
            )
            if (status == GateStatus.CURRENT) {
                Text(gate.trains, style = MaterialTheme.typography.bodyMedium, color = LevitxColors.TextSoft)
            }
            if (best > 0) {
                Text(
                    text = stringResource(R.string.path_best, measureText(gate.measure, best)),
                    style = MaterialTheme.typography.bodySmall,
                    color = LevitxColors.TextMuted,
                )
            }
        }
    }
}
