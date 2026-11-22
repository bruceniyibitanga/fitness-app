package app.levitx.phone.exercise

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.coach.Exercise
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.OutlineBar
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.kitShort

/** Height of a row in the swap list: a comfortable touch target. */
private val SwapRowHeight = 56.dp

/** The "IN YOUR PLAN" marker. */
private val PlanMarkSize = 8.dp

/**
 * Swapping, as the "Exercise detail: swap" artboard: on the exercise they
 * train, the others at the same level and a marker that it's in their plan;
 * on another exercise at that level, a bar to train it instead.
 */
@Composable
internal fun Swap(state: ExerciseUiState.Ready, onOpenExercise: (String) -> Unit, onTrainInstead: () -> Unit) {
    when (state.swap) {
        SwapState.NONE -> Unit

        SwapState.CURRENT -> {
            Alternatives(state.alternatives, onOpenExercise)
            InYourPlan()
        }

        SwapState.AVAILABLE -> OutlineBar(
            text = stringResource(R.string.exercise_train_instead),
            onClick = onTrainInstead,
            modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 20.dp, bottom = 20.dp),
        )
    }
}

/** "SWAP · SAME LEVEL", then a row per other exercise, each opening its page. */
@Composable
private fun Alternatives(alternatives: List<Exercise>, onOpenExercise: (String) -> Unit) {
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 18.dp)) {
        Row(modifier = Modifier.padding(bottom = 8.dp)) {
            Caption(
                stringResource(R.string.exercise_swap_label),
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
            )
            Caption(stringResource(R.string.exercise_swap_remembered), color = LevitxColors.TextFaint)
        }
        Rule()
        alternatives.forEach { exercise ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = SwapRowHeight)
                    .clickable(role = Role.Button) { onOpenExercise(exercise.id) },
            ) {
                Text(exercise.name, fontSize = 16.sp, color = LevitxColors.Text, modifier = Modifier.weight(1f))
                Caption(kitShort(exercise.equipment), color = LevitxColors.TextFaint)
                Icon(
                    painterResource(R.drawable.ic_chevron_forward),
                    contentDescription = null,
                    tint = LevitxColors.TextFaint,
                )
            }
            Rule()
        }
    }
}

/** An orange square and "IN YOUR PLAN". */
@Composable
private fun InYourPlan() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 18.dp, bottom = 24.dp),
    ) {
        Box(
            Modifier
                .size(PlanMarkSize)
                .background(LevitxColors.Accent),
        )
        Caption(stringResource(R.string.exercise_in_plan), color = LevitxColors.Text)
    }
}
