package app.levitx.phone.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.design.LevitxColors
import app.levitx.phone.R

/**
 * The data can't be opened, as the Data locked state on the States
 * artboard: what happened and that nothing is lost, in two short lines.
 * Nothing here blames the person.
 *
 * @param onBack Shown as a back arrow above, on screens with no header of their own.
 */
@Composable
fun DataUnavailable(modifier: Modifier = Modifier, onBack: (() -> Unit)? = null) {
    Column(modifier = modifier) {
        onBack?.let { BackButton(it, Modifier.statusBarsPadding().padding(4.dp)) }
        Column(modifier = Modifier.padding(ScreenPadding)) {
            Caption(stringResource(R.string.data_unavailable_label), color = LevitxColors.Ice)
            Text(
                text = stringResource(R.string.data_unavailable_title),
                fontSize = 22.sp,
                lineHeight = 27.sp,
                fontWeight = FontWeight.Bold,
                color = LevitxColors.Text,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .semantics { heading() },
            )
            Lede(stringResource(R.string.data_unavailable), Modifier.padding(top = 8.dp))
        }
    }
}
