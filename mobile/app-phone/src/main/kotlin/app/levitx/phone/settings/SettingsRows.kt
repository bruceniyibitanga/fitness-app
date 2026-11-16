package app.levitx.phone.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding

/* The rows Settings is made of. */

@Composable
internal fun Group(title: String) {
    Caption(
        text = title,
        modifier = Modifier
            .padding(start = ScreenPadding, end = ScreenPadding, top = 24.dp, bottom = 4.dp)
            .semantics { heading() },
    )
}

@Composable
internal fun RowText(title: String, note: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = LevitxColors.Text)
        Text(note, fontSize = 13.sp, lineHeight = 18.sp, color = LevitxColors.TextMuted)
    }
}

/** A setting that is on or off, with the design's square switch. The whole row toggles it. */
@Composable
internal fun SwitchRow(title: String, note: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = ScreenPadding, vertical = 10.dp),
    ) {
        RowText(title, note, Modifier.weight(1f))
        Box(
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
            modifier = Modifier
                .padding(start = 16.dp)
                .width(44.dp)
                .height(24.dp)
                .background(if (checked) LevitxColors.Accent else LevitxColors.Background)
                .border(1.dp, if (checked) LevitxColors.Accent else LevitxColors.Outline)
                .padding(3.dp),
        ) {
            Box(
                Modifier
                    .size(16.dp)
                    .background(if (checked) LevitxColors.Background else LevitxColors.TextFaint),
            )
        }
    }
    Rule(Modifier.padding(horizontal = ScreenPadding))
}

/** One of [choices] as a row of square boxes, the [selected] one filled. */
@Composable
internal fun <T> ChoiceBoxes(choices: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 12.dp).selectableGroup(),
    ) {
        choices.forEach { choice ->
            val isSelected = choice == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(if (isSelected) LevitxColors.Text else LevitxColors.Background)
                    .border(1.dp, if (isSelected) LevitxColors.Text else LevitxColors.Outline)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(choice) }),
            ) {
                Text(
                    text = label(choice),
                    style = LevitxType.Caption,
                    color = if (isSelected) LevitxColors.Background else LevitxColors.Text,
                )
            }
        }
    }
}

/** A row that opens somewhere else, with where it stands ([status]) on the right. */
@Composable
internal fun LinkRow(title: String, note: String, status: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = ScreenPadding, vertical = 10.dp),
    ) {
        RowText(title, note, Modifier.weight(1f))
        Caption(status, color = LevitxColors.TextFaint, modifier = Modifier.padding(start = 16.dp))
    }
    Rule(Modifier.padding(horizontal = ScreenPadding))
}

/** A data action: its name and, on the right, what kind of thing it is. */
@Composable
internal fun DataButton(title: String, kind: String, enabled: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = ScreenPadding, vertical = 4.dp)
            .fillMaxWidth()
            .height(52.dp)
            .border(1.dp, LevitxColors.Outline)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = LevitxColors.Text,
            modifier = Modifier.weight(1f),
        )
        Caption(kind, color = LevitxColors.TextFaint)
    }
}

@Composable
internal fun taskMessage(task: DataTask): String? = when (task) {
    DataTask.IDLE, DataTask.DELETED -> null
    DataTask.EXPORTING -> stringResource(R.string.settings_exporting)
    DataTask.EXPORTED -> stringResource(R.string.settings_exported)
    DataTask.EXPORT_FAILED -> stringResource(R.string.settings_export_failed)
    DataTask.DELETING -> stringResource(R.string.settings_deleting)
    DataTask.DELETE_FAILED -> stringResource(R.string.settings_delete_failed)
}
