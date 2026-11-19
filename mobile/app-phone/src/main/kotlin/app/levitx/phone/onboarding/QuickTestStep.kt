package app.levitx.phone.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.coach.QuickTest
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.AccentBar
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.ScreenTitle

/**
 * One movement of the optional quick test, as the Assess artboard: the
 * question, a big rep count with − and +, where that count would start them,
 * and what counts as a rep.
 */
@Composable
internal fun QuickTestStep(state: OnboardingUiState, actions: OnboardingActions) {
    val test = state.test ?: return
    val words = test.words
    Column(modifier = Modifier.padding(horizontal = ScreenPadding)) {
        Caption(stringResource(R.string.test_label, state.testIndex + 1, state.tests.size))
        ScreenTitle(stringResource(words.question), modifier = Modifier.padding(top = 14.dp))
        Lede(stringResource(R.string.test_lede), modifier = Modifier.padding(top = 10.dp))
    }
    Counter(state.testCount, onCount = actions.onCountTest, modifier = Modifier.padding(top = 28.dp))
    val start = state.testStart
    if (start == null) {
        Text(
            text = stringResource(R.string.test_none),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = LevitxColors.Text,
            modifier = Modifier
                .padding(start = ScreenPadding, end = ScreenPadding, top = 16.dp)
                .fillMaxWidth()
                .background(LevitxColors.Surface)
                .border(1.dp, LevitxColors.Ice)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    } else {
        val (exercise, target) = start
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = ScreenPadding),
        ) {
            Caption(stringResource(R.string.test_start), modifier = Modifier.weight(1f))
            Caption(
                text = (target?.let { stringResource(R.string.test_start_target, exercise.name, it) } ?: exercise.name)
                    .uppercase(),
                color = LevitxColors.Accent,
            )
        }
        Rule()
    }
    Column(modifier = Modifier.padding(top = 8.dp)) {
        RuleRow(R.string.test_counts, words.counts)
        RuleRow(R.string.test_doesnt, words.doesnt)
        RuleRow(R.string.test_rest, R.string.test_rest_note)
    }
}

/** The bottom of a quick test screen: on to the next movement or the plan, or skip the rest. */
@Composable
internal fun QuickTestBar(state: OnboardingUiState, actions: OnboardingActions, modifier: Modifier = Modifier) {
    val next = state.tests.getOrNull(state.testIndex + 1)
    Row(modifier = modifier.height(BAR_HEIGHT)) {
        Box(modifier = Modifier.weight(2f)) {
            AccentBar(
                text = next?.let { stringResource(R.string.test_next, stringResource(it.words.short)) }
                    ?: stringResource(R.string.test_done),
                onClick = actions.onNext,
                height = BAR_HEIGHT,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(LevitxColors.Surface)
                .clickable(role = Role.Button, onClick = actions.onSkip),
        ) {
            Text(stringResource(R.string.test_skip), fontSize = 15.sp, color = LevitxColors.Text)
        }
    }
}

/** The count, huge, between a − and a + that fill the row's height. */
@Composable
private fun Counter(count: Int, onCount: (Int) -> Unit, modifier: Modifier = Modifier) {
    val reps = stringResource(R.string.test_reps)
    Column(modifier = modifier) {
        Rule()
        Row(modifier = Modifier.height(COUNTER_HEIGHT)) {
            CounterButton(stringResource(R.string.decrease), stringResource(R.string.test_fewer)) { onCount(count - 1) }
            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(LevitxColors.Line))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = "$count $reps"
                    },
            ) {
                Text(
                    text = count.toString().padStart(2, '0'),
                    style = LevitxType.Hero.copy(fontSize = 136.sp, lineHeight = 112.sp),
                    color = LevitxColors.Text,
                )
                Caption(reps, modifier = Modifier.padding(top = 8.dp))
            }
            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(LevitxColors.Line))
            CounterButton(stringResource(R.string.increase), stringResource(R.string.test_more)) { onCount(count + 1) }
        }
        Rule()
    }
}

/** A full-height − or + beside the count. */
@Composable
private fun CounterButton(symbol: String, label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(80.dp)
            .fillMaxHeight()
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        Text(symbol, fontSize = 28.sp, color = LevitxColors.Text)
    }
}

/** One "COUNTS / DOESN'T / REST" line: a mono label and a sentence. */
@Composable
private fun RuleRow(@StringRes label: Int, @StringRes text: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(horizontal = ScreenPadding, vertical = 14.dp),
    ) {
        Text(
            text = stringResource(label),
            style = LevitxType.Micro,
            color = LevitxColors.TextFaint,
            modifier = Modifier.width(64.dp).padding(top = 3.dp),
        )
        Text(stringResource(text), fontSize = 15.sp, lineHeight = 22.sp, color = LevitxColors.Text)
    }
    Rule()
}

/** What a test is called and what counts as a rep, in words. */
private data class TestWords(
    @StringRes val question: Int,
    @StringRes val short: Int,
    @StringRes val counts: Int,
    @StringRes val doesnt: Int,
)

private val QuickTest.words: TestWords
    get() = when (this) {
        QuickTest.PUSH_UPS -> TestWords(
            R.string.test_push_ups,
            R.string.test_push_ups_short,
            R.string.test_push_ups_counts,
            R.string.test_push_ups_doesnt,
        )

        QuickTest.PULL_UPS -> TestWords(
            R.string.test_pull_ups,
            R.string.test_pull_ups_short,
            R.string.test_pull_ups_counts,
            R.string.test_pull_ups_doesnt,
        )

        QuickTest.DIPS -> TestWords(
            R.string.test_dips,
            R.string.test_dips_short,
            R.string.test_dips_counts,
            R.string.test_dips_doesnt,
        )

        QuickTest.PIKE_PUSH_UPS -> TestWords(
            R.string.test_pike,
            R.string.test_pike_short,
            R.string.test_pike_counts,
            R.string.test_pike_doesnt,
        )

        QuickTest.SQUATS -> TestWords(
            R.string.test_squats,
            R.string.test_squats_short,
            R.string.test_squats_counts,
            R.string.test_squats_doesnt,
        )
    }

private val COUNTER_HEIGHT = 168.dp
private val BAR_HEIGHT = 64.dp
