package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.mettyoung.fitbro.data.model.CardioSession
import com.mettyoung.fitbro.data.model.DailyBalance
import com.mettyoung.fitbro.data.model.WeekMetric
import com.mettyoung.fitbro.ui.ColorBurn
import com.mettyoung.fitbro.ui.ColorCardio
import com.mettyoung.fitbro.ui.ColorCarbs
import com.mettyoung.fitbro.ui.ColorFat
import com.mettyoung.fitbro.ui.ColorProtein
import com.mettyoung.fitbro.ui.BrandPrimary
import com.mettyoung.fitbro.ui.PositiveColor
import kotlin.math.abs
import kotlin.math.roundToInt

/** Icon, colour and short label per metric — shared by Trends and Compare tiles. */
internal class MetricStyle(val icon: ImageVector, val color: Color, val shortLabel: String)

@Composable
internal fun metricStyles(): Map<WeekMetric, MetricStyle> {
    val positive = PositiveColor
    val orange = BrandPrimary
    return WeekMetric.entries.associateWith { m ->
        when (m) {
            WeekMetric.AVG_INTAKE -> MetricStyle(Icons.Filled.Restaurant, positive, "Intake")
            WeekMetric.AVG_BURN -> MetricStyle(Icons.Filled.LocalFireDepartment, ColorBurn, "Burn")
            WeekMetric.AVG_BALANCE -> MetricStyle(Icons.Filled.Balance, orange, "Balance")
            WeekMetric.NET_BALANCE -> MetricStyle(Icons.AutoMirrored.Filled.TrendingUp, orange, "Net")
            WeekMetric.AVG_PROTEIN -> MetricStyle(Icons.Filled.FitnessCenter, ColorProtein, "Protein")
            WeekMetric.AVG_CARBS -> MetricStyle(Icons.Filled.BakeryDining, ColorCarbs, "Carbs")
            WeekMetric.AVG_FAT -> MetricStyle(Icons.Filled.WaterDrop, ColorFat, "Fat")
            WeekMetric.CARDIO_MINUTES -> MetricStyle(Icons.AutoMirrored.Filled.DirectionsRun, ColorCardio, "Cardio")
        }
    }
}

/**
 * Per-day values for [metric]'s mini chart. Unlogged days (no intake) have no diet values;
 * NET_BALANCE is the running total; days failing [include] (today/future) are null.
 */
internal fun dailySeries(
    metric: WeekMetric,
    days: List<String>,
    balances: List<DailyBalance>,
    sessions: List<CardioSession>,
    include: (String) -> Boolean
): List<Double?> {
    var running = 0.0
    return days.map { d ->
        if (!include(d)) return@map null
        val b = balances.firstOrNull { it.date == d }
        val logged = b != null && b.intake > 0
        when (metric) {
            WeekMetric.AVG_INTAKE -> b?.intake?.takeIf { logged }
            WeekMetric.AVG_BURN -> b?.burn?.takeIf { it > 0 }
            WeekMetric.AVG_BALANCE -> b?.balance?.takeIf { logged }
            WeekMetric.NET_BALANCE -> { if (logged) running += b!!.balance; running }
            WeekMetric.AVG_PROTEIN -> b?.proteinG?.takeIf { logged }
            WeekMetric.AVG_CARBS -> b?.carbG?.takeIf { logged }
            WeekMetric.AVG_FAT -> b?.fatG?.takeIf { logged }
            WeekMetric.CARDIO_MINUTES -> sessions.filter { it.date == d }.sumOf { it.minutes }.toDouble()
        }
    }
}

/** "2,100 kcal", "+300 kcal" (signed metrics), or "—". */
internal val WeekMetric.isSigned: Boolean
    get() = this == WeekMetric.AVG_BALANCE || this == WeekMetric.NET_BALANCE

internal fun formatValue(value: Double?, metric: WeekMetric): String {
    if (value == null) return "—"
    val v = value.roundToInt()
    val sign = if (metric.isSigned && v > 0) "+" else ""
    return "$sign${groupThousands(v)} ${metric.unit}"
}

/** "▲ 120 kcal", "▼ 8 g", "same", or "—" when either week has no data. */
internal fun formatDelta(delta: Double?, unit: String): String {
    if (delta == null) return "—"
    val v = delta.roundToInt()
    return when {
        v > 0 -> "▲ ${groupThousands(v)} $unit"
        v < 0 -> "▼ ${groupThousands(abs(v))} $unit"
        else -> "same"
    }
}

internal fun groupThousands(v: Int): String {
    val digits = abs(v).toString().reversed().chunked(3).joinToString(",").reversed()
    return if (v < 0) "-$digits" else digits
}

internal fun Double?.grouped(): String = this?.let { groupThousands(it.roundToInt()) } ?: "—"
