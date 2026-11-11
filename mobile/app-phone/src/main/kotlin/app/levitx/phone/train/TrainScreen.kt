package app.levitx.phone.train

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxSpacing
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.theme.LevitxTheme
import app.levitx.phone.ui.SectionLabel

/**
 * Train tab. For now it says what is coming and points back to Today; the
 * workout library replaces it in the next update.
 */
@Composable
fun TrainScreen(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(LevitxSpacing.Small),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LevitxSpacing.Medium, vertical = LevitxSpacing.Large),
    ) {
        SectionLabel(stringResource(R.string.train_label), color = LevitxColors.Accent)
        Text(stringResource(R.string.train_title), style = LevitxType.Display)
        Text(
            text = stringResource(R.string.train_body),
            style = MaterialTheme.typography.bodyLarge,
            color = LevitxColors.TextSoft,
        )
    }
}

@Preview
@Composable
private fun TrainScreenPreview() {
    LevitxTheme { TrainScreen() }
}
