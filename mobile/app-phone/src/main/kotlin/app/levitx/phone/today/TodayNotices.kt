package app.levitx.phone.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.QuietBar
import app.levitx.phone.ui.ScreenPadding

/*
 * Cards that sit at the top of Today now and then: the week review and the weight notice.
 */

/** Said once per spell of fast weight loss, plainly and without judgement. */
@Composable
internal fun WeightNotice(onAcknowledge: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(LevitxColors.Surface)
            .padding(ScreenPadding),
    ) {
        Caption(stringResource(R.string.today_weight_label), color = LevitxColors.Ice)
        Text(
            stringResource(R.string.today_weight_body),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = LevitxColors.Text,
        )
        QuietBar(stringResource(R.string.today_weight_ok), onAcknowledge)
    }
}

/** Offers last week's review, at the top of Today until it's opened. */
@Composable
internal fun WeekReviewCard(onOpen: () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(LevitxColors.Surface)
            .padding(ScreenPadding),
    ) {
        Caption(stringResource(R.string.today_review_label), color = LevitxColors.Accent)
        Text(
            stringResource(R.string.today_review_body),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = LevitxColors.Text,
        )
        QuietBar(stringResource(R.string.today_review_open), onOpen)
    }
}
