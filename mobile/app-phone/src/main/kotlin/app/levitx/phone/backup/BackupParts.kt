package app.levitx.phone.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.ScreenPadding

/** Height of a word tile and a word field. */
internal val WordHeight = 56.dp

/** The square beside a fact or a matched word. */
private val MarkSize = 8.dp

/**
 * Words typed here are secrets: no autocorrect, no suggestions and nothing
 * the keyboard should learn. The password keyboard type asks for that; the
 * text still shows, so the person can check what they typed.
 */
internal val SecretWordsKeyboard = KeyboardOptions(
    capitalization = KeyboardCapitalization.None,
    autoCorrectEnabled = false,
    keyboardType = KeyboardType.Password,
    imeAction = ImeAction.Done,
)

/** A step label and the step's title, as on the backup and restore artboards. */
@Composable
internal fun StepTitle(label: String, title: String, labelColor: Color = LevitxColors.Accent) {
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 22.dp)) {
        Caption(label, color = labelColor)
        Text(
            text = title,
            style = LevitxType.Poster.copy(fontSize = 34.sp, lineHeight = 34.sp),
            color = LevitxColors.Text,
            modifier = Modifier
                .padding(top = 10.dp)
                .semantics { heading() },
        )
    }
}

/** A small square, then a line in capitals. */
@Composable
internal fun Fact(text: String, color: Color = LevitxColors.Ice) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .size(MarkSize)
                .background(color),
        )
        Caption(text, color = LevitxColors.Text)
    }
}

/**
 * A field for secret words, bordered orange when [wrong]. [trailing] sits at
 * the end of a one-line field (a mark, or "NOT IT").
 */
@Composable
internal fun SecretField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    wrong: Boolean,
    modifier: Modifier = Modifier,
    minHeight: Dp = WordHeight,
    singleLine: Boolean = true,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .border(1.dp, if (wrong) LevitxColors.Accent else LevitxColors.Tick)
            .padding(14.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            keyboardOptions = SecretWordsKeyboard,
            textStyle = TextStyle(
                color = LevitxColors.Text,
                fontSize = 20.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
            ),
            cursorBrush = SolidColor(LevitxColors.Accent),
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = label },
        )
        trailing()
    }
}

/** The mark beside a word typed right. */
@Composable
internal fun MatchMark() {
    Box(
        Modifier
            .size(MarkSize)
            .background(LevitxColors.Text),
    )
}
