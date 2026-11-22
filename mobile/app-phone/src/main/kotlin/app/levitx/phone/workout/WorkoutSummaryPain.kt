package app.levitx.phone.workout

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.core.coach.BodyArea
import app.levitx.design.LevitxColors
import app.levitx.phone.R
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.OutlineBar
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.areaName

private val AnswerHeight = 48.dp

/**
 * The end of the summary: one question for each sore area this session
 * loaded on light work, answered in one tap, then the way into the pain check
 * for anything new. Fine puts the area back to normal, still sore keeps it
 * light, and worse opens the full check.
 *
 * Which questions have been answered is remembered here and not in saved
 * state: it is about the person's health, and saved state goes to disk.
 */
@Composable
internal fun PainQuestions(eased: List<BodyArea>, onSettled: (BodyArea) -> Unit, onOpenPainCheck: () -> Unit) {
    var answered by remember { mutableStateOf(emptySet<BodyArea>()) }
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 18.dp, bottom = 20.dp)) {
        eased.filterNot { it in answered }.forEach { area ->
            Caption(
                text = stringResource(R.string.workout_pain_question, areaName(area)),
                color = LevitxColors.Ice,
                modifier = Modifier.semantics { heading() },
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) {
                Answer(
                    text = stringResource(R.string.workout_pain_fine),
                    onClick = {
                        onSettled(area)
                        answered = answered + area
                    },
                )
                Answer(stringResource(R.string.workout_pain_sore), onClick = { answered = answered + area })
                Answer(stringResource(R.string.workout_pain_worse), onClick = onOpenPainCheck)
            }
            Spacer(Modifier.height(18.dp))
        }
        OutlineBar(text = stringResource(R.string.workout_pain_open), onClick = onOpenPainCheck)
    }
}

/** One answer: an outlined square button, a third of the row. */
@Composable
private fun RowScope.Answer(text: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .weight(1f)
            .heightIn(min = AnswerHeight)
            .border(1.dp, LevitxColors.Tick)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Text(text = text, fontSize = 15.sp, color = LevitxColors.Text)
    }
}
