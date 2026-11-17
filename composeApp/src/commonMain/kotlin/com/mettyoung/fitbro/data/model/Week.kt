package com.mettyoung.fitbro.data.model

import com.mettyoung.fitbro.util.MONTH_ABBR
import com.mettyoung.fitbro.util.dayOfWeekMonBased
import com.mettyoung.fitbro.util.plusDays
import com.mettyoung.fitbro.util.toShortDate
import com.mettyoung.fitbro.util.toYMD

/** First day of the week. Entry names are persisted — don't rename. Ordinal matches `dayOfWeekMonBased`. */
enum class WeekStart(val label: String, val short: String) {
    MONDAY("Monday", "Mon"),
    TUESDAY("Tuesday", "Tue"),
    WEDNESDAY("Wednesday", "Wed"),
    THURSDAY("Thursday", "Thu"),
    FRIDAY("Friday", "Fri"),
    SATURDAY("Saturday", "Sat"),
    SUNDAY("Sunday", "Sun");

    companion object {
        /** FR-1a: missing/unknown → Monday. */
        fun parse(stored: String?): WeekStart = entries.firstOrNull { it.name == stored } ?: MONDAY
    }
}

/** A 7-day calendar week identified by its first date ("YYYY-MM-DD"). */
data class Week(val start: String) {
    val end: String get() = start.plusDays(6)

    fun days(): List<String> = (0..6).map { start.plusDays(it) }
    fun previous(): Week = Week(start.plusDays(-7))
    fun next(): Week = Week(start.plusDays(7))
    fun contains(date: String): Boolean = date in start..end

    /** Days of this week up to and including [today] (7 for past weeks, 0 for future weeks). */
    fun elapsedDays(today: String): Int = days().count { it <= today }

    /** FR-4 */
    fun label(today: String, weekStart: WeekStart): String {
        val current = of(today, weekStart)
        return when (this) {
            current -> "This week"
            current.previous() -> "Last week"
            else -> {
                val year = today.toYMD().first
                val (sy, _, _) = start.toYMD()
                val (ey, em, ed) = end.toYMD()
                val range = "${start.toShortDate()} – ${MONTH_ABBR[em]} $ed"
                if (sy == year && ey == year) range else "$range, $ey"
            }
        }
    }

    companion object {
        /** FR-1: the week containing [date] (latest [weekStart] day on or before it). */
        fun of(date: String, weekStart: WeekStart): Week {
            val (y, m, d) = date.toYMD()
            val offset = (dayOfWeekMonBased(y, m, d) - weekStart.ordinal).mod(7)
            return Week(date.plusDays(-offset))
        }
    }
}

/** FR-5: one week's numbers. Averages are over logged days (intake > 0); null when none logged. */
data class WeekSummary(
    val loggedDays: Int,
    val avgIntake: Double?,
    val avgBurn: Double?,
    val avgBalance: Double?,
    val netBalance: Double?,
    val avgProteinG: Double?,
    val avgCarbG: Double?,
    val avgFatG: Double?,
    val cardioMinutes: Int
) {
    companion object {
        val EMPTY = WeekSummary(0, null, null, null, null, null, null, null, 0)
    }
}

fun summarizeWeek(balances: List<DailyBalance>, days: List<String>, cardioMinutes: Int = 0): WeekSummary {
    val daySet = days.toSet()
    val logged = balances.filter { it.date in daySet && it.intake > 0 }
    if (logged.isEmpty()) return WeekSummary.EMPTY.copy(cardioMinutes = cardioMinutes)
    fun avg(f: (DailyBalance) -> Double) = logged.sumOf(f) / logged.size
    return WeekSummary(
        loggedDays = logged.size,
        avgIntake = avg { it.intake },
        avgBurn = avg { it.burn },
        avgBalance = avg { it.balance },
        netBalance = logged.sumOf { it.balance },
        avgProteinG = avg { it.proteinG },
        avgCarbG = avg { it.carbG },
        avgFatG = avg { it.fatG },
        cardioMinutes = cardioMinutes
    )
}

/** Week-over-week metrics in display order. [optional] rows (carbs, fat) are hidden unless the user opts in. */
enum class WeekMetric(val label: String, val unit: String, val optional: Boolean = false) {
    AVG_INTAKE("Avg intake", "kcal"),
    AVG_BURN("Avg burn", "kcal"),
    AVG_BALANCE("Avg balance", "kcal"),
    NET_BALANCE("Net balance", "kcal"),
    AVG_PROTEIN("Avg protein", "g"),
    AVG_CARBS("Avg carbs", "g", optional = true),
    AVG_FAT("Avg fat", "g", optional = true),
    CARDIO_MINUTES("Cardio", "min");

    fun valueOf(s: WeekSummary): Double? = when (this) {
        AVG_INTAKE -> s.avgIntake
        AVG_BURN -> s.avgBurn
        AVG_BALANCE -> s.avgBalance
        NET_BALANCE -> s.netBalance
        AVG_PROTEIN -> s.avgProteinG
        AVG_CARBS -> s.avgCarbG
        AVG_FAT -> s.avgFatG
        CARDIO_MINUTES -> s.cardioMinutes.toDouble()
    }
}

data class MetricComparison(val metric: WeekMetric, val current: Double?, val previous: Double?) {
    /** Null when either side has no data. */
    val delta: Double? get() = if (current != null && previous != null) current - previous else null
}

/** FR-6 */
fun compareWeeks(current: WeekSummary, previous: WeekSummary): List<MetricComparison> =
    WeekMetric.entries.map { MetricComparison(it, it.valueOf(current), it.valueOf(previous)) }

/**
 * FR-7: the days of [previous] to compare against. Like-for-like for a partial [current] week
 * (first `elapsedDays` days only), the full week otherwise.
 */
fun comparisonDays(current: Week, today: String): List<String> {
    val elapsed = current.elapsedDays(today).coerceIn(1, 7)
    return current.previous().days().take(elapsed)
}

/** "Mon–Wed" style span for the like-for-like caption; null for full weeks. */
fun partialWeekSpan(current: Week, today: String, weekStart: WeekStart): String? {
    val elapsed = current.elapsedDays(today)
    if (elapsed >= 7) return null
    val first = weekStart.short
    val last = WeekStart.entries[(weekStart.ordinal + elapsed - 1).mod(7)].short
    return if (elapsed <= 1) first else "$first–$last"
}
