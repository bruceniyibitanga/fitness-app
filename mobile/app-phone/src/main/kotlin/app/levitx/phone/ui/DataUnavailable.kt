package app.levitx.phone.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
 * artboard: what happened, that nothing is lost, and the one thing to do.
 * Nothing here blames the person.
 */
@Composable
fun DataUnavailable(modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(ScreenPadding)) {
        Caption(stringResource(R.string.data_unavailable_label), color = LevitxColors.Accent)
        Text(
            text = stringResource(R.string.data_unavailable_title),
            fontSize = 26.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
            color = LevitxColors.Text,
            modifier = Modifier
                .padding(top = 10.dp)
                .semantics { heading() },
        )
        Lede(stringResource(R.string.data_unavailable), Modifier.padding(top = 10.dp))
    }
}
