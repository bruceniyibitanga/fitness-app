package app.levitx.phone.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.coach.PlannedSession
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Photo
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.patternName
import app.levitx.phone.ui.patternPhoto

/** How tall the photo is: about two fifths of the screen, as on the Today artboard. */
private val HeroHeight = 340.dp

/** Where the photo starts fading into the background, as a fraction of its height. */
private const val FADE_START = 0.3f

/**
 * Today's session at a glance: a photo of its main movement, and over the
 * photo's foot what kind of work it is, the movement's name, and how long it
 * is. Everything else about the session is in the rows under it.
 *
 * @param session The session shown: the coach's, or the lighter one if that was picked.
 * @param doneThisWeek Sessions finished in the last seven days.
 * @param plannedPerWeek Sessions a week the person said they'd train.
 */
@Composable
internal fun TodayHero(session: PlannedSession, doneThisWeek: Int, plannedPerWeek: Int) {
    val lead = session.plans.first()
    val kind = stringResource(
        when {
            lead.isGateAttempt -> R.string.today_kind_test
            lead.gateId != null -> R.string.today_kind_skill
            !lead.isHard -> R.string.today_kind_light
            else -> R.string.today_kind_training
        },
    )
    Box(modifier = Modifier.fillMaxWidth().height(HeroHeight)) {
        Photo(patternPhoto(lead.exercise.pattern), modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(FADE_START to Color.Transparent, 1f to LevitxColors.Background)),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = ScreenPadding),
        ) {
            Caption(
                // Past the week's plan there is no "4 of 3": it says how many are done instead.
                text = if (doneThisWeek < plannedPerWeek) {
                    stringResource(R.string.today_kicker_next, kind, doneThisWeek + 1, plannedPerWeek)
                } else {
                    stringResource(R.string.today_kicker_extra, kind, doneThisWeek)
                },
                color = LevitxColors.Accent,
            )
            Text(
                text = patternName(lead.exercise.pattern),
                style = LevitxType.Poster,
                color = LevitxColors.Text,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = pluralStringResource(
                    R.plurals.today_facts,
                    session.plans.size,
                    session.plans.size,
                    session.estimatedMinutes,
                ),
                fontSize = 15.sp,
                color = LevitxColors.TextSoft,
            )
        }
    }
}
