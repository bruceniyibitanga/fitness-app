package app.levitx.phone.progress

import app.levitx.core.history.WorkoutRecord
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * One cell of the month.
 *
 * @property date The day, or null for a blank before the 1st or after the last day.
 * @property trained True when a workout finished that day.
 * @property planned True for a training day still to come.
 * @property isToday True for today.
 */
data class CalendarDay(val date: LocalDate?, val trained: Boolean, val planned: Boolean, val isToday: Boolean)

/** History: every workout, a month at a time. */
sealed interface HistoryUiState {
    /** Reading the history. */
    data object Loading : HistoryUiState

    /** The data can't be read on this device right now. */
    data object Unavailable : HistoryUiState

    /**
     * Ready to show.
     *
     * @property month The month shown.
     * @property days The month's cells, Monday first, in whole weeks.
     * @property workouts The month's workouts, newest first.
     * @property canGoBack True when there is an earlier month with workouts.
     * @property canGoForward True when the month shown is before this one.
     */
    data class Ready(
        val month: YearMonth,
        val days: List<CalendarDay>,
        val workouts: List<WorkoutRecord>,
        val canGoBack: Boolean,
        val canGoForward: Boolean,
    ) : HistoryUiState {
        /** Minutes trained in the month. */
        val minutes: Int
            get() = workouts.sumOf { it.minutes }

        /** Sets done in the month. */
        val sets: Int
            get() = workouts.sumOf { it.sets }
    }
}

/**
 * [month] of [workouts] in [zone], with [trainingDays] still to come marked
 * as planned, as of [today].
 */
fun historyState(
    month: YearMonth,
    workouts: List<WorkoutRecord>,
    trainingDays: Set<DayOfWeek>,
    today: LocalDate,
    zone: ZoneId,
): HistoryUiState.Ready {
    fun dayOf(workout: WorkoutRecord): LocalDate =
        Instant.ofEpochMilli(workout.finishedAtEpochMillis).atZone(zone).toLocalDate()
    val inMonth = workouts.filter { YearMonth.from(dayOf(it)) == month }
    val trained = inMonth.map(::dayOf).toSet()
    // Blanks before the 1st so Monday is the first column, and after the last day to finish the week.
    val leading = month.atDay(1).dayOfWeek.value - 1
    val cells = List(leading) { null } + (1..month.lengthOfMonth()).map(month::atDay)
    val trailing = (DAYS_PER_WEEK - cells.size % DAYS_PER_WEEK) % DAYS_PER_WEEK
    return HistoryUiState.Ready(
        month = month,
        days = (cells + List(trailing) { null }).map { date ->
            CalendarDay(
                date = date,
                trained = date in trained,
                planned = date != null && date.isAfter(today) && date.dayOfWeek in trainingDays,
                isToday = date == today,
            )
        },
        workouts = inMonth.sortedByDescending { it.finishedAtEpochMillis },
        canGoBack = workouts.any { YearMonth.from(dayOf(it)) < month },
        canGoForward = month < YearMonth.from(today),
    )
}

private const val DAYS_PER_WEEK = 7
