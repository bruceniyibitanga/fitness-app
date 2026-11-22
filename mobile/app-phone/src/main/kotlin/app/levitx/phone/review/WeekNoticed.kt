package app.levitx.phone.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.coach.ExerciseLookup
import app.levitx.core.history.Noticed
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.areaName

/** The square beside "THE COACH NOTICED". */
private val NoticedMark = 8.dp

/**
 * "THE COACH NOTICED" and one line, as on the week review artboard: what
 * stood out in the week and what the coach did about it. Nothing when
 * nothing stood out.
 */
@Composable
internal fun CoachNoticed(noticed: Noticed?) {
    val line = noticed?.let { noticedText(it) } ?: return
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ScreenPadding, end = ScreenPadding, top = 14.dp, bottom = 16.dp)
            // Read as one: the label, then the line.
            .semantics(mergeDescendants = true) {},
    ) {
        Box(
            Modifier
                .padding(top = 4.dp)
                .size(NoticedMark)
                .background(LevitxColors.Ice),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Caption(stringResource(R.string.review_noticed_label), color = LevitxColors.Ice)
            Text(
                line,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = LevitxColors.Text,
            )
        }
    }
    Rule()
}

/** What was noticed, in a sentence. */
@Composable
private fun noticedText(noticed: Noticed): String? = when (noticed) {
    is Noticed.WorkingAround -> stringResource(R.string.review_noticed_area, areaName(noticed.area))

    Noticed.TookEasierDay -> stringResource(R.string.review_noticed_easier)

    // An exercise no longer in the app has no name to show, so the line is left out.
    is Noticed.FeltEasy ->
        ExerciseLookup.byId(noticed.exerciseId)?.let { stringResource(R.string.review_noticed_easy, it.name) }

    is Noticed.HitTargets ->
        pluralStringResource(R.plurals.review_noticed_targets, noticed.trained, noticed.hit, noticed.trained)
}
