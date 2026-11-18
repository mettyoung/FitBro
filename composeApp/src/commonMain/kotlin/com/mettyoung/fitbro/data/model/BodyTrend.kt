package com.mettyoung.fitbro.data.model

import com.mettyoung.fitbro.util.daysUntil
import com.mettyoung.fitbro.util.plusDays
import kotlinx.serialization.Serializable

/** One day's body-composition readings from Health Connect / HealthKit (last reading of the day per field). */
@Serializable
data class BodyMeasurement(
    val date: String,
    val weightKg: Double? = null,
    val bodyFatPct: Double? = null,
    val leanMassKg: Double? = null
)

/** Smoothed body-composition series shown on the Dashboard. */
enum class BodyMetric(val label: String, val shortLabel: String) {
    WEIGHT("Weight trend", "Weight"),
    LEAN_MASS("Lean body mass", "Lean mass"),
    BODY_FAT("Body fat trend", "Body fat");

    fun valueOf(m: BodyMeasurement): Double? = when (this) {
        WEIGHT -> m.weightKg
        LEAN_MASS -> m.leanMassKg
        BODY_FAT -> bodyFatKg(m)
    }
}

/** Body fat in kg = weight × body fat %; null unless both were measured that day. */
fun bodyFatKg(m: BodyMeasurement): Double? {
    val w = m.weightKg ?: return null
    val pct = m.bodyFatPct ?: return null
    return w * pct / 100.0
}

const val TREND_ALPHA = 0.1
const val TREND_WINDOW_DAYS = 28
const val KCAL_PER_KG = 7700.0
const val EE_MIN_LOGGED_DAYS = 14
const val EE_MIN_SPAN_DAYS = 14

/**
 * Exponentially smoothed trend over [days]: seeded with the first reading, moves α toward each
 * new reading, carries forward on days without one. Null before the first reading.
 */
fun trendSeries(points: Map<String, Double>, days: List<String>, alpha: Double = TREND_ALPHA): List<Double?> {
    var trend: Double? = null
    return days.map { d ->
        points[d]?.let { v -> trend = trend?.let { it + alpha * (v - it) } ?: v }
        trend
    }
}

/**
 * The 28-day window ending on [week]'s last completed day (current week: yesterday; past week:
 * its last day). On the current week's first day it ends on the day before the week.
 */
fun trendWindow(week: Week, today: String): List<String> {
    val end = week.averagingDays(today).lastOrNull() ?: week.start.plusDays(-1)
    return (0 until TREND_WINDOW_DAYS).map { end.plusDays(it - (TREND_WINDOW_DAYS - 1)) }
}

/** Days to read so every trend window shown for [week] has ≥ 28 days of EMA warm-up. */
fun trendReadDays(week: Week, today: String): List<String> {
    val start = week.start.plusDays(-2 * TREND_WINDOW_DAYS)
    val end = trendWindow(week, today).last()
    return (0..start.daysUntil(end)).map { start.plusDays(it) }
}

data class TrendSummary(
    val metric: BodyMetric,
    val windowDays: List<String>,
    /** Raw readings in the window (null on days without one). */
    val scale: List<Double?>,
    val trend: List<Double?>,
    /** Trend at window end. */
    val value: Double?,
    /** Trend at window end − trend at window start. */
    val delta: Double?,
    /** Mean of non-null trend values in the window. */
    val average: Double?
) {
    val weighIns: List<Pair<String, Double>>
        get() = windowDays.zip(scale).mapNotNull { (d, v) -> v?.let { d to it } }
}

fun summarizeTrend(
    measurements: List<BodyMeasurement>,
    metric: BodyMetric,
    readDays: List<String>,
    windowDays: List<String>
): TrendSummary {
    val points = metric.points(measurements)
    val byDate = readDays.zip(trendSeries(points, readDays)).toMap()
    val trend = windowDays.map { byDate[it] }
    val first = trend.firstOrNull()
    val last = trend.lastOrNull()
    return TrendSummary(
        metric = metric,
        windowDays = windowDays,
        scale = windowDays.map { points[it] },
        trend = trend,
        value = last,
        delta = if (first != null && last != null) last - first else null,
        average = trend.filterNotNull().takeIf { it.isNotEmpty() }?.average()
    )
}

fun BodyMetric.points(measurements: List<BodyMeasurement>): Map<String, Double> =
    measurements.mapNotNull { m -> valueOf(m)?.let { m.date to it } }.toMap()

/** Inputs behind an estimated-expenditure figure, for the detail page. */
data class ExpenditureEstimate(
    val kcalPerDay: Double,
    val avgIntake: Double,
    val loggedDays: Int,
    /** Trend weight change between the first and last weigh-in in the window. */
    val trendChangeKg: Double,
    val spanDays: Int
)

/**
 * Energy-balance expenditure over [windowDays]:
 * `avgIntake − Δtrend × 7700 / spanDays`, where Δtrend is the trend-weight change between the
 * window's first and last weigh-in. Null with < 14 logged days or weigh-ins < 14 days apart.
 */
fun estimateExpenditure(
    intakeByDate: Map<String, Double>,
    weightPoints: Map<String, Double>,
    trendByDate: Map<String, Double?>,
    windowDays: List<String>
): ExpenditureEstimate? {
    val logged = windowDays.mapNotNull { d -> intakeByDate[d]?.takeIf { it > 0 } }
    if (logged.size < EE_MIN_LOGGED_DAYS) return null
    val weighDays = windowDays.filter { it in weightPoints }
    if (weighDays.size < 2) return null
    val span = weighDays.first().daysUntil(weighDays.last())
    if (span < EE_MIN_SPAN_DAYS) return null
    val startTrend = trendByDate[weighDays.first()] ?: return null
    val endTrend = trendByDate[weighDays.last()] ?: return null
    val change = endTrend - startTrend
    val avgIntake = logged.average()
    return ExpenditureEstimate(avgIntake - change * KCAL_PER_KG / span, avgIntake, logged.size, change, span)
}

/** Rolling estimate for each of [days] (window = 28 days ending that day); null where not computable. */
fun rollingExpenditure(
    intakeByDate: Map<String, Double>,
    weightPoints: Map<String, Double>,
    trendByDate: Map<String, Double?>,
    days: List<String>,
    include: (String) -> Boolean
): List<Double?> = days.map { d ->
    if (!include(d)) null
    else estimateExpenditure(
        intakeByDate, weightPoints, trendByDate,
        (0 until TREND_WINDOW_DAYS).map { d.plusDays(it - (TREND_WINDOW_DAYS - 1)) }
    )?.kcalPerDay
}

/** FR-3: sum and average of balance over logged [days]. Null when none logged. */
data class WeekToDateBalance(val total: Double, val average: Double, val loggedDays: Int)

fun weekToDateBalance(balances: List<DailyBalance>, days: Collection<String>): WeekToDateBalance? {
    val set = days.toSet()
    val logged = balances.filter { it.date in set && it.intake > 0 }
    if (logged.isEmpty()) return null
    val total = logged.sumOf { it.balance }
    return WeekToDateBalance(total, total / logged.size, logged.size)
}

/** FR-4: strength sessions in [week], regardless of the Cardio tab's hidden types. */
fun resistanceCount(sessions: List<CardioSession>, week: Week, excludedSources: Set<String> = emptySet()): Int =
    sessions.count { week.contains(it.date) && it.type.category == WorkoutCategory.STRENGTH && it.source !in excludedSources }
