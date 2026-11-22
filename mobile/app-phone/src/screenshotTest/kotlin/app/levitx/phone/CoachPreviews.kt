package app.levitx.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import app.levitx.core.coach.MovementPattern
import app.levitx.core.history.Noticed
import app.levitx.core.history.PatternSets
import app.levitx.core.history.WeekNote
import app.levitx.core.history.WeekReview
import app.levitx.design.LevitxColors
import app.levitx.phone.review.WeekReviewScreen
import app.levitx.phone.review.WeekReviewUiState
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.today.WeighInAsk
import app.levitx.phone.today.WeighInSheet
import app.levitx.phone.ui.WeekBounds
import com.android.tools.screenshot.PreviewTest

/*
 * The coach's prompts, rendered by CI to check against the "Coach" artboards.
 */

/** 6 Nov 2026, 07:00 UTC. */
private const val LAST_READING_AT = 1_794_034_800_000L

private const val WEEK = 7L * 24 * 60 * 60 * 1_000

@PreviewTest
@Preview(name = "41 Weigh-in", widthDp = 390, heightDp = 844)
@Composable
fun WeighInScreenshot() {
    LevitxTheme {
        Box(modifier = Modifier.fillMaxSize().background(LevitxColors.Background)) {
            WeighInSheet(WeighInAsk(lastKg = 74.2, lastAtEpochMillis = LAST_READING_AT), onSave = {
            }, onSkip = {}, onDismiss = {})
        }
    }
}

@PreviewTest
@Preview(name = "42 Weigh-in, first time", widthDp = 390, heightDp = 844)
@Composable
fun FirstWeighInScreenshot() {
    LevitxTheme {
        Box(modifier = Modifier.fillMaxSize().background(LevitxColors.Background)) {
            WeighInSheet(WeighInAsk(lastKg = null, lastAtEpochMillis = null), onSave = {}, onSkip = {}, onDismiss = {})
        }
    }
}

@PreviewTest
@Preview(name = "43 Week review", widthDp = 390, heightDp = 1080)
@Composable
fun WeekReviewScreenshot() {
    val review = WeekReview(
        sessionsDone = 3,
        sessionsPlanned = 3,
        newBests = 2,
        gatesPassed = 1,
        hardSets = listOf(
            PatternSets(MovementPattern.VERTICAL_PULL, done = 7.0, target = 6.0),
            PatternSets(MovementPattern.HORIZONTAL_PULL, done = 5.0, target = 6.0),
            PatternSets(MovementPattern.HORIZONTAL_PUSH, done = 4.0, target = 6.0),
            PatternSets(MovementPattern.SQUAT, done = 4.0, target = 6.0),
            PatternSets(MovementPattern.CORE, done = 6.0, target = 6.0),
        ),
        notes = listOf(
            WeekNote.ShortOf(MovementPattern.HORIZONTAL_PUSH, 2),
            WeekNote.ShortOf(MovementPattern.SQUAT, 2),
            WeekNote.LighterWeekDue(3),
        ),
        noticed = Noticed.FeltEasy("horizontal_push.push_up"),
    )
    LevitxTheme {
        WeekReviewScreen(
            WeekReviewUiState.Ready(
                review,
                WeekBounds(LAST_READING_AT, LAST_READING_AT + WEEK),
            ),
            onClose = {
            },
        )
    }
}
