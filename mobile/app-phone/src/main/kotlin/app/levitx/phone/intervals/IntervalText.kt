package app.levitx.phone.intervals

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.levitx.core.coach.MovementPattern
import app.levitx.core.intervals.IntervalBlock
import app.levitx.core.intervals.IntervalKind
import app.levitx.core.intervals.IntervalWorkout
import app.levitx.phone.R
import app.levitx.phone.ui.kitShort
import app.levitx.phone.ui.patternPhoto

/*
 * How an interval workout is described, so the Train row, the detail page
 * and the timer all say the same thing: "17 MIN · 4 MOVES" and "8 × 20 s · 10 s".
 */

/** "17 MIN · 4 MOVES", for a Train row. */
@Composable
fun intervalRowMeta(workout: IntervalWorkout): String = listOf(
    pluralStringResource(R.plurals.interval_moves_count, workout.blocks.size, workout.minutes, workout.blocks.size),
    kitShort(workout.equipment),
).joinToString(SEPARATOR)

/** "17 MIN · CARDIO · NO KIT", for the detail page. */
@Composable
fun intervalDetailMeta(workout: IntervalWorkout): String = listOf(
    stringResource(R.string.minutes_short, workout.minutes),
    stringResource(kindName(workout.kind)),
    kitShort(workout.equipment),
).joinToString(SEPARATOR)

/** "8 × 20 s · 10 s", "2 × 45 s each side" or "1 × 60 s". */
@Composable
fun blockText(block: IntervalBlock): String = when {
    block.move.isPerSide -> stringResource(R.string.interval_block_each_side, block.rounds, block.workSeconds)
    block.restSeconds == 0 -> stringResource(R.string.interval_block_no_rest, block.rounds, block.workSeconds)
    else -> stringResource(R.string.interval_block, block.rounds, block.workSeconds, block.restSeconds)
}

/** The photo for a kind of interval workout. */
@DrawableRes
fun intervalPhoto(kind: IntervalKind): Int = patternPhoto(
    when (kind) {
        IntervalKind.CARDIO -> MovementPattern.CARDIO
        IntervalKind.MOBILITY -> MovementPattern.MOBILITY
    },
)

private fun kindName(kind: IntervalKind): Int = when (kind) {
    IntervalKind.CARDIO -> R.string.interval_kind_cardio
    IntervalKind.MOBILITY -> R.string.interval_kind_mobility
}

private const val SEPARATOR = " · "
