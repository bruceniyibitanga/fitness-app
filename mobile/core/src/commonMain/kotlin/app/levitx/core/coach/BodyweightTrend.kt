package app.levitx.core.coach

import app.levitx.core.load.LoadLimits
import app.levitx.core.requireInRange

/**
 * One bodyweight reading, typed in or read from Health Connect.
 *
 * Bodyweight is health data. It lives in the encrypted database on the phone
 * and is never synced to the watch (docs/app/SECURITY.md).
 *
 * @property atEpochMillis When it was taken, in UTC milliseconds.
 * @property kg The reading.
 * @throws IllegalArgumentException if [kg] is outside [LoadLimits].
 */
public data class WeighIn(val atEpochMillis: Long, val kg: Double) {
    init {
        requireInRange(kg, LoadLimits.MIN_BODYWEIGHT_KG, LoadLimits.MAX_BODYWEIGHT_KG, "kg")
    }
}

/**
 * Where someone's bodyweight is and where it is heading.
 *
 * @property currentKg The weight to use for load per rep: the average of the
 *   last week's readings, or the latest reading when there are none that recent.
 * @property weeklyChange Change per week as a fraction of bodyweight, negative
 *   for loss. Null until there are enough readings over enough days to trust.
 * @property isFastLoss True when weight has been falling faster than
 *   [CoachTuning.FAST_LOSS_WEEKLY_FRACTION] a week for two weeks or more.
 */
public data class BodyweightTrend(val currentKg: Double, val weeklyChange: Double?, val isFastLoss: Boolean)

/**
 * Works out the trend from [weighIns] as of [nowEpochMillis], or null when
 * there are no readings.
 *
 * A day's weight moves by a kilogram or more with food and water, so the trend
 * is a straight line fitted through the last three weeks of readings, never
 * the gap between two of them.
 */
public fun bodyweightTrend(weighIns: List<WeighIn>, nowEpochMillis: Long): BodyweightTrend? {
    val past = weighIns.filter { it.atEpochMillis <= nowEpochMillis }.sortedBy { it.atEpochMillis }
    val latest = past.lastOrNull() ?: return null
    val lastWeek = past.filter { it.atEpochMillis > nowEpochMillis - DAYS_PER_WEEK * MILLIS_PER_DAY }
    val current = if (lastWeek.isEmpty()) latest.kg else lastWeek.map { it.kg }.average()
    val window = past.filter {
        it.atEpochMillis > nowEpochMillis - CoachTuning.WEIGHT_TREND_WINDOW_DAYS * MILLIS_PER_DAY
    }
    val spanDays = window.spanDays()
    val enough =
        window.size >= CoachTuning.WEIGHT_TREND_MIN_READINGS && spanDays >= CoachTuning.WEIGHT_TREND_MIN_SPAN_DAYS
    val weeklyChange = if (enough) weeklyChangeFraction(window) else null
    val fast = weeklyChange != null &&
        weeklyChange <= -CoachTuning.FAST_LOSS_WEEKLY_FRACTION &&
        spanDays >= CoachTuning.FAST_LOSS_MIN_SPAN_DAYS
    return BodyweightTrend(currentKg = current, weeklyChange = weeklyChange, isFastLoss = fast)
}

private fun List<WeighIn>.spanDays(): Double =
    if (isEmpty()) 0.0 else (last().atEpochMillis - first().atEpochMillis).toDouble() / MILLIS_PER_DAY

/** Least-squares slope through the readings, as a fraction of the average weight per week. */
private fun weeklyChangeFraction(readings: List<WeighIn>): Double {
    val start = readings.first().atEpochMillis
    val days = readings.map { (it.atEpochMillis - start).toDouble() / MILLIS_PER_DAY }
    val meanDay = days.average()
    val meanKg = readings.map { it.kg }.average()
    val spread = days.sumOf { (it - meanDay) * (it - meanDay) }
    val together = readings.indices.sumOf { (days[it] - meanDay) * (readings[it].kg - meanKg) }
    val kgPerDay = if (spread == 0.0) 0.0 else together / spread
    return kgPerDay * DAYS_PER_WEEK / meanKg
}
