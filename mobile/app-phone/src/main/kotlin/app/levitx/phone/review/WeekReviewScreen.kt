package app.levitx.phone.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.history.PatternSets
import app.levitx.core.history.WeekReview
import app.levitx.core.load.formatKg
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.WeekBounds
import app.levitx.phone.ui.patternName
import app.levitx.phone.ui.shortDate

/**
 * The week review's destination: connects [WeekReviewViewModel] to [WeekReviewScreen].
 *
 * @param onClose Leaves the review.
 */
@Composable
fun WeekReviewRoute(onClose: () -> Unit, viewModel: WeekReviewViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    WeekReviewScreen(uiState, onClose)
}

private const val DAY_MILLIS = 24L * 60 * 60 * 1_000
private val BarHeight = 8.dp
private val NameWidth = 120.dp

/**
 * Last week, laid out as the "Coach: week review" artboard: sessions done
 * against the plan, new bests, gates and hard sets, hard sets by movement
 * against their targets, and what's worth knowing. Facts only: the coach
 * plans each session as it comes, so nothing here promises next week.
 */
@Composable
fun WeekReviewScreen(uiState: WeekReviewUiState, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LevitxColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Header((uiState as? WeekReviewUiState.Ready)?.week, onClose)
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when (uiState) {
                WeekReviewUiState.Loading -> Unit

                WeekReviewUiState.Unavailable ->
                    Lede(stringResource(R.string.data_unavailable), Modifier.padding(ScreenPadding))

                is WeekReviewUiState.Ready -> {
                    Sessions(uiState.review)
                    Highlights(uiState.review)
                    HardSets(uiState.review.hardSets)
                    Notes(uiState.review)
                }
            }
        }
        AccentBar(text = stringResource(R.string.review_done), onClick = onClose, height = 64.dp)
    }
}

/** Close on the left, the week's dates on the right. */
@Composable
private fun Header(week: WeekBounds?, onClose: () -> Unit) {
    val label = stringResource(R.string.review_close)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(48.dp)
                .clickable(role = Role.Button, onClick = onClose)
                .semantics { contentDescription = label },
        ) { Icon(painterResource(R.drawable.ic_close), contentDescription = null, tint = LevitxColors.Text) }
        Box(modifier = Modifier.weight(1f))
        if (week != null) {
            Caption(
                stringResource(
                    R.string.review_dates,
                    shortDate(week.lastWeekFromEpochMillis),
                    // The Sunday: a day before this week's Monday.
                    shortDate(week.thisWeekFromEpochMillis - DAY_MILLIS),
                ),
                modifier = Modifier.padding(end = ScreenPadding),
            )
        }
    }
    Rule()
}

/** "YOUR WEEK", sessions done out of planned in big type, and a line about it. */
@Composable
private fun Sessions(review: WeekReview) {
    val description = pluralStringResource(
        R.plurals.review_sessions,
        review.sessionsPlanned,
        review.sessionsDone,
        review.sessionsPlanned,
    )
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 22.dp, bottom = 18.dp)) {
        Caption(stringResource(R.string.review_label), color = LevitxColors.Accent)
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(top = 12.dp).clearAndSetSemantics { contentDescription = description },
        ) {
            Text(
                "${review.sessionsDone}",
                style = LevitxType.Hero.copy(fontSize = 84.sp, lineHeight = 70.sp),
                color = LevitxColors.Text,
            )
            Text(
                "/${review.sessionsPlanned}",
                style = LevitxType.Hero.copy(fontSize = 84.sp, lineHeight = 70.sp),
                color = LevitxColors.TextFaint,
            )
        }
        Text(
            text = when {
                review.sessionsDone > review.sessionsPlanned -> stringResource(R.string.review_more_done)
                review.sessionsDone == review.sessionsPlanned -> stringResource(R.string.review_all_done)
                else -> stringResource(R.string.review_short, review.sessionsPlanned - review.sessionsDone)
            },
            fontSize = 15.sp,
            lineHeight = 20.sp,
            color = LevitxColors.TextMuted,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
    Rule()
}

/** New bests, gates passed and hard sets, side by side. */
@Composable
private fun Highlights(review: WeekReview) {
    Row(modifier = Modifier.fillMaxWidth().height(80.dp)) {
        Highlight(review.newBests, stringResource(R.string.review_bests), accent = review.newBests > 0)
        Highlight(review.gatesPassed, stringResource(R.string.review_gates), accent = false)
        Highlight(review.totalHardSets, stringResource(R.string.review_hard_sets), accent = false)
    }
    Rule()
}

/** One figure over its label. */
@Composable
private fun RowScope.Highlight(value: Int, label: String, accent: Boolean) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .padding(horizontal = ScreenPadding)
            .clearAndSetSemantics { contentDescription = "$value $label" },
    ) {
        Text("$value", style = LevitxType.Figure, color = if (accent) LevitxColors.Accent else LevitxColors.Text)
        Caption(label, modifier = Modifier.padding(top = 4.dp))
    }
}

/** A bar per movement: hard sets done against its weekly target, orange once it's met. */
@Composable
private fun HardSets(rows: List<PatternSets>) {
    if (rows.isEmpty()) return
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 16.dp, bottom = 18.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
            Caption(stringResource(R.string.review_by_movement), modifier = Modifier.weight(1f))
            Caption(stringResource(R.string.review_done_target), color = LevitxColors.TextFaint)
        }
        rows.forEach { row -> HardSetsRow(row) }
    }
    Rule()
}

@Composable
private fun HardSetsRow(row: PatternSets) {
    val name = patternName(row.pattern)
    val description = stringResource(R.string.review_sets_description, name, formatKg(row.done), formatKg(row.target))
    val share = if (row.target <= 0.0) 1f else (row.done / row.target).toFloat().coerceIn(0f, 1f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        Text(name, fontSize = 13.sp, color = LevitxColors.TextSoft, modifier = Modifier.width(NameWidth))
        Box(modifier = Modifier.weight(1f).height(BarHeight).background(LevitxColors.Track)) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(share)
                    .background(if (row.done >= row.target) LevitxColors.Accent else LevitxColors.Text),
            )
        }
        Text(
            text = "${formatKg(row.done)}/${formatKg(row.target)}",
            style = LevitxType.Caption,
            color = LevitxColors.Text,
            textAlign = TextAlign.End,
            modifier = Modifier.width(56.dp),
        )
    }
}

/** "WORTH KNOWING" and a bullet per note. Nothing when there's nothing to say. */
@Composable
private fun Notes(review: WeekReview) {
    if (review.notes.isEmpty()) return
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 16.dp, bottom = 24.dp),
    ) {
        Caption(stringResource(R.string.review_notes), modifier = Modifier.semantics { heading() })
        review.notes.forEach { note ->
            Row {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp, end = 10.dp)
                        .size(6.dp)
                        .background(noteColor(note)),
                )
                Text(noteText(note), fontSize = 14.sp, lineHeight = 19.sp, color = LevitxColors.Text)
            }
        }
    }
}
