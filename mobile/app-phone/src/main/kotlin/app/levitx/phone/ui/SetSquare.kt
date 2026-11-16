package app.levitx.phone.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.session.LoggedSet
import app.levitx.core.skills.GateMeasure
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R

/** The last "reps left" cell, shown as "4+". Four or more is Easy to the coach. */
const val MAX_REPS_LEFT = 4

/** "4+" for the last cell, else the number. */
@Composable
fun repsLeftLabel(left: Int): String =
    if (left >= MAX_REPS_LEFT) stringResource(R.string.workout_reps_left_max, MAX_REPS_LEFT) else left.toString()

/** One set: "8" over "2" (reps left), or "30s" over a dot for a hold. */
@Composable
fun SetSquare(measure: GateMeasure, set: LoggedSet) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(38.dp)
            .border(1.dp, LevitxColors.Tick),
    ) {
        Text(
            text = if (measure == GateMeasure.HOLD_SECONDS) {
                stringResource(R.string.workout_summary_seconds, set.value)
            } else {
                set.value.toString()
            },
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = LevitxColors.Text,
        )
        Text(
            text = set.repsInReserve?.let { repsLeftLabel(it) } ?: stringResource(R.string.workout_summary_no_left),
            style = LevitxType.Micro,
            color = LevitxColors.TextFaint,
        )
    }
}
