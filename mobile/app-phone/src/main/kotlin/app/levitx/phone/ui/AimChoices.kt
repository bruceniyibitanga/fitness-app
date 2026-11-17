package app.levitx.phone.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.levitx.core.coach.TrainingAim
import app.levitx.phone.R

/** The four training aims as two rows of two, the [selected] one filled. Used in setup and on Body. */
@Composable
fun AimChoices(selected: TrainingAim, onChoose: (TrainingAim) -> Unit, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = modifier) {
        TrainingAim.entries.chunked(2).forEach { pair ->
            ChoiceRow {
                pair.forEach { aim -> ChoiceBar(stringResource(aimName(aim)), aim == selected, { onChoose(aim) }) }
            }
        }
    }
}

/** "Balanced", "Muscle", "Strength" or "Conditioning". */
@StringRes
fun aimName(aim: TrainingAim): Int = when (aim) {
    TrainingAim.BALANCED -> R.string.aim_balanced
    TrainingAim.MUSCLE -> R.string.aim_muscle
    TrainingAim.STRENGTH -> R.string.aim_strength
    TrainingAim.CONDITIONING -> R.string.aim_conditioning
}

/** One line on what [aim] changes. */
@StringRes
fun aimNote(aim: TrainingAim): Int = when (aim) {
    TrainingAim.BALANCED -> R.string.aim_balanced_note
    TrainingAim.MUSCLE -> R.string.aim_muscle_note
    TrainingAim.STRENGTH -> R.string.aim_strength_note
    TrainingAim.CONDITIONING -> R.string.aim_conditioning_note
}
