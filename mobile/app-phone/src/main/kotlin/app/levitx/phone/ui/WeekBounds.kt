package app.levitx.phone.ui

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Where last week and this week start, Monday to Sunday in the phone's time
 * zone, as the week review counts them.
 *
 * @property lastWeekFromEpochMillis Midnight on the Monday a week before [thisWeekFromEpochMillis].
 * @property thisWeekFromEpochMillis Midnight on this week's Monday.
 */
data class WeekBounds(val lastWeekFromEpochMillis: Long, val thisWeekFromEpochMillis: Long)

/** The week bounds around [nowEpochMillis] in [zone]. Daylight saving changes are handled by [ZoneId]. */
fun weekBounds(nowEpochMillis: Long, zone: ZoneId): WeekBounds {
    val monday = Instant.ofEpochMilli(nowEpochMillis).atZone(zone).toLocalDate()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return WeekBounds(
        lastWeekFromEpochMillis = monday.minusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli(),
        thisWeekFromEpochMillis = monday.atStartOfDay(zone).toInstant().toEpochMilli(),
    )
}
