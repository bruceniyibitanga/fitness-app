package app.levitx.phone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.levitx.core.coach.Equipment
import app.levitx.phone.R

/** Kit in the order it is offered: the most useful first. */
private val KitOrder = listOf(
    Equipment.PULL_UP_BAR,
    Equipment.DIP_BARS,
    Equipment.RINGS,
    Equipment.LOW_BAR,
    Equipment.BAND,
    Equipment.BOX,
)

/** Kit choices side by side. Two fit the longest name at any font size. */
private const val KIT_PER_ROW = 2

/**
 * "Nothing yet" and each piece of kit as square bars, two to a row. Used in
 * setup and on the Body tab, so the kit reads the same in both.
 *
 * @param equipment What is picked now.
 * @param onChooseNone Clears everything.
 * @param onToggle Adds or takes away one piece.
 */
@Composable
fun KitPicker(
    equipment: Set<Equipment>,
    onChooseNone: () -> Unit,
    onToggle: (Equipment) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = modifier) {
        ChoiceRow {
            ChoiceBar(text = stringResource(R.string.kit_none), selected = equipment.isEmpty(), onClick = onChooseNone)
        }
        KitOrder.chunked(KIT_PER_ROW).forEach { row ->
            ChoiceRow {
                row.forEach { kit ->
                    ChoiceBar(
                        text = kitName(kit),
                        selected = kit in equipment,
                        onClick = { onToggle(kit) },
                        multiple = true,
                    )
                }
            }
        }
    }
}
