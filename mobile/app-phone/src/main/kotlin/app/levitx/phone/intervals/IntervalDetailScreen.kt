package app.levitx.phone.intervals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.intervals.IntervalBlock
import app.levitx.core.intervals.IntervalCatalogue
import app.levitx.core.intervals.IntervalWorkout
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.BackButton
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Help
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.PhotoHeader
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding

/**
 * A cardio or mobility workout's destination. The workout comes straight
 * from the catalogue: nothing personal is read, so there is no ViewModel.
 *
 * @param workoutId The workout's catalogue id from the back stack.
 */
@Composable
fun IntervalDetailRoute(workoutId: String, navigation: IntervalDetailNavigation) {
    IntervalDetailScreen(IntervalCatalogue.byId(workoutId), navigation)
}

/**
 * Where a cardio or mobility workout's page can lead.
 *
 * @property onBack Goes back to the Train tab.
 * @property onStart Starts the timer.
 * @property onOpenMove Opens a move's how-to, by exercise id.
 */
data class IntervalDetailNavigation(val onBack: () -> Unit, val onStart: () -> Unit, val onOpenMove: (String) -> Unit)

private val TitleOverlap = (-20).dp
private val PhotoHeight = 240.dp
private val StartBarHeight = 64.dp
private val MoveRowHeight = 58.dp

/**
 * A cardio or mobility workout, laid out as the "Cardio and mobility
 * workout" artboard: photo, name, size, what it is, the moves with their
 * work and rest, and Start pinned to the bottom.
 *
 * @param workout The workout, or null when the id isn't in the catalogue.
 */
@Composable
fun IntervalDetailScreen(
    workout: IntervalWorkout?,
    navigation: IntervalDetailNavigation,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(LevitxColors.Background).navigationBarsPadding()) {
        if (workout == null) {
            BackButton(navigation.onBack, modifier = Modifier.statusBarsPadding().padding(4.dp))
            Lede(stringResource(R.string.interval_not_found), modifier = Modifier.padding(ScreenPadding))
            return@Column
        }
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            PhotoHeader(intervalPhoto(workout.kind), navigation.onBack, height = PhotoHeight)
            Heading(workout)
            Moves(workout.blocks, navigation.onOpenMove)
        }
        Help(
            text = stringResource(R.string.interval_note),
            modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 10.dp, bottom = 10.dp),
        )
        AccentBar(text = stringResource(R.string.interval_start), onClick = navigation.onStart, height = StartBarHeight)
    }
}

/** The name, its size and what it is. */
@Composable
private fun Heading(workout: IntervalWorkout) {
    Column(modifier = Modifier.offset(y = TitleOverlap).padding(horizontal = ScreenPadding)) {
        Text(
            text = workout.name,
            style = LevitxType.Poster.copy(fontSize = 40.sp, lineHeight = 38.sp),
            color = LevitxColors.Text,
            modifier = Modifier.semantics { heading() },
        )
        Caption(text = intervalDetailMeta(workout), modifier = Modifier.padding(top = 10.dp))
        Text(
            text = workout.about,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = LevitxColors.TextMuted,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

/** "MOVES" and "WORK · REST", then a row per move. */
@Composable
private fun Moves(blocks: List<IntervalBlock>, onOpenMove: (String) -> Unit) {
    Rule()
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenPadding, vertical = 12.dp),
    ) {
        Caption(stringResource(R.string.interval_moves))
        Caption(stringResource(R.string.interval_work_rest), color = LevitxColors.TextFaint)
    }
    Rule()
    blocks.forEach { block ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MoveRowHeight)
                .clickable(role = Role.Button, onClick = { onOpenMove(block.move.id) })
                .padding(horizontal = ScreenPadding),
        ) {
            Text(
                text = block.move.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = LevitxColors.Text,
                modifier = Modifier.weight(1f),
            )
            Text(blockText(block), fontSize = 13.sp, color = LevitxColors.TextMuted)
        }
        Rule()
    }
}

@Preview
@Composable
private fun IntervalDetailScreenPreview() {
    LevitxTheme {
        IntervalDetailScreen(
            workout = IntervalCatalogue.byId("tabata"),
            navigation = IntervalDetailNavigation(onBack = {}, onStart = {}, onOpenMove = {}),
        )
    }
}
