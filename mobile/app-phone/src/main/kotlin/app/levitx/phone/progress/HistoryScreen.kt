package app.levitx.phone.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.levitx.core.history.WorkoutRecord
import app.levitx.design.LevitxColors
import app.levitx.design.LevitxType
import app.levitx.phone.R
import app.levitx.phone.ui.BackHeader
import app.levitx.phone.ui.Caption
import app.levitx.phone.ui.Lede
import app.levitx.phone.ui.Rule
import app.levitx.phone.ui.ScreenPadding
import app.levitx.phone.ui.StatStrip
import app.levitx.phone.ui.dayDate
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private const val MINUTES_PER_HOUR = 60
private const val HALF_TURN = 180f

/** History destination: connects [HistoryViewModel] to [HistoryScreen]. */
@Composable
fun HistoryRoute(onBack: () -> Unit, onOpenWorkout: (String) -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryScreen(uiState, onBack, onOpenWorkout, onMoveMonth = viewModel::moveMonth)
}

/**
 * History, as the History artboard: the month with trained days filled and
 * training days still to come outlined, the month's figures, and every
 * workout in it.
 */
@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onBack: () -> Unit,
    onOpenWorkout: (String) -> Unit,
    onMoveMonth: (forward: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LevitxColors.Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        BackHeader(stringResource(R.string.history_title), onBack) {
            if (uiState is HistoryUiState.Ready) MonthSwitch(uiState, onMoveMonth)
        }
        when (uiState) {
            HistoryUiState.Loading -> Unit

            HistoryUiState.Unavailable -> Lede(
                stringResource(R.string.data_unavailable),
                Modifier.padding(ScreenPadding),
            )

            is HistoryUiState.Ready -> Column(Modifier.verticalScroll(rememberScrollState())) {
                Calendar(uiState)
                Rule()
                StatStrip(
                    figures = listOf(
                        uiState.workouts.size.toString() to stringResource(R.string.history_workouts),
                        duration(uiState.minutes) to stringResource(R.string.history_time),
                        uiState.sets.toString() to stringResource(R.string.history_sets),
                    ),
                    modifier = Modifier.padding(horizontal = ScreenPadding),
                )
                Rule()
                SectionTitle(stringResource(R.string.history_list))
                if (uiState.workouts.isEmpty()) {
                    Lede(stringResource(R.string.history_none), Modifier.padding(horizontal = ScreenPadding))
                }
                uiState.workouts.forEach { WorkoutRow(it, onOpenWorkout) }
            }
        }
    }
}

/** "‹ NOV 2026 ›". */
@Composable
private fun MonthSwitch(state: HistoryUiState.Ready, onMoveMonth: (forward: Boolean) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("MMM yyyy", locale) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        MonthButton(stringResource(R.string.history_previous), state.canGoBack, pointsBack = true) {
            onMoveMonth(false)
        }
        Caption(formatter.format(state.month), color = LevitxColors.Text)
        MonthButton(stringResource(R.string.history_next), state.canGoForward) { onMoveMonth(true) }
    }
}

@Composable
private fun MonthButton(label: String, enabled: Boolean, pointsBack: Boolean = false, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
    ) {
        Icon(
            painterResource(R.drawable.ic_chevron_forward),
            contentDescription = null,
            tint = if (enabled) LevitxColors.Text else LevitxColors.Tick,
            modifier = if (pointsBack) Modifier.rotate(HALF_TURN) else Modifier,
        )
    }
}

/** The month, Monday first: trained days filled, training days to come outlined, today in orange. */
@Composable
private fun Calendar(state: HistoryUiState.Ready) {
    val locale = LocalConfiguration.current.locales[0]
    Column(modifier = Modifier.padding(start = ScreenPadding, end = ScreenPadding, top = 18.dp, bottom = 18.dp)) {
        Row {
            DayOfWeek.entries.forEach {
                Caption(
                    text = it.getDisplayName(TextStyle.NARROW, locale),
                    color = LevitxColors.TextFaint,
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 8.dp),
                )
            }
        }
        state.days.chunked(DayOfWeek.entries.size).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                week.forEach { day -> DayCell(day, Modifier.weight(1f)) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 10.dp)) {
            LegendKey(stringResource(R.string.history_key_trained), filled = true)
            LegendKey(stringResource(R.string.history_key_planned), filled = false)
        }
    }
}

@Composable
private fun DayCell(day: CalendarDay, modifier: Modifier) {
    val date = day.date
    val description = date?.let {
        stringResource(
            when {
                day.trained -> R.string.history_day_trained
                day.planned -> R.string.history_day_planned
                else -> R.string.history_day_plain
            },
            it.dayOfMonth,
        )
    }.orEmpty()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f)
            .background(if (day.trained) LevitxColors.Text else LevitxColors.Background)
            .border(
                width = if (day.isToday) 2.dp else 1.dp,
                color = when {
                    day.isToday -> LevitxColors.Accent
                    day.planned -> LevitxColors.Outline
                    else -> LevitxColors.Background
                },
            )
            .clearAndSetSemantics { if (date != null) contentDescription = description },
    ) {
        if (date != null) {
            Text(
                text = date.dayOfMonth.toString(),
                fontSize = 13.sp,
                fontWeight = if (day.trained) FontWeight.Bold else FontWeight.Normal,
                color = if (day.trained) LevitxColors.Background else LevitxColors.TextMuted,
            )
        }
    }
}

@Composable
private fun LegendKey(label: String, filled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .background(if (filled) LevitxColors.Text else LevitxColors.Background)
                .border(1.dp, if (filled) LevitxColors.Text else LevitxColors.Outline),
        )
        Caption(label, color = LevitxColors.TextFaint, modifier = Modifier.padding(start = 6.dp))
    }
}

/** A workout: the day, what was trained, how long and how many sets. */
@Composable
private fun WorkoutRow(workout: WorkoutRecord, onOpenWorkout: (String) -> Unit) {
    val day = Instant.ofEpochMilli(workout.finishedAtEpochMillis).atZone(ZoneId.systemDefault())
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClickLabel = dayDate(workout.finishedAtEpochMillis), onClick = {
                onOpenWorkout(workout.id)
            })
            .padding(horizontal = ScreenPadding, vertical = 10.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(44.dp)) {
            Text(day.dayOfMonth.toString(), style = LevitxType.Figure, color = LevitxColors.Text)
            Caption(
                day.dayOfWeek.getDisplayName(TextStyle.SHORT, LocalConfiguration.current.locales[0]),
                color = LevitxColors.TextFaint,
            )
        }
        Column(
            modifier = Modifier
                .padding(start = 14.dp)
                .weight(1f),
        ) {
            Text(workoutTitle(workout), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = LevitxColors.Text)
            Caption(workoutMeta(workout), color = LevitxColors.TextFaint)
        }
        Icon(painterResource(R.drawable.ic_chevron_forward), contentDescription = null, tint = LevitxColors.TextFaint)
    }
    Rule(Modifier.padding(horizontal = ScreenPadding))
}

/** "48 min", or "3 h 12" past an hour. */
@Composable
internal fun duration(minutes: Int): String = if (minutes < MINUTES_PER_HOUR) {
    stringResource(R.string.minutes_short, minutes)
} else {
    stringResource(R.string.history_hours, minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
}
