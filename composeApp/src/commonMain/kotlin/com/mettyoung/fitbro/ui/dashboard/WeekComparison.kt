package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.model.DailyBalance
import com.mettyoung.fitbro.data.model.MetricComparison
import com.mettyoung.fitbro.data.model.WeekMetric
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

// US-002: week-over-week comparison. Deltas are neutral-coloured (FitBro doesn't know the goal direction).

/** Like-for-like caption shared by the teaser and the page. */
private fun comparisonCaption(loggedDays: Int, comparedDays: Int, partialSpan: String?): String = when {
    comparedDays == 0 -> "Starts after today — compares completed days only"
    partialSpan != null -> "$partialSpan vs $partialSpan last week · $loggedDays of $comparedDays days logged"
    else -> "Full week · $loggedDays of 7 days logged"
}

/** Compact entry point on Trends; tap opens [WeekComparisonPage]. */
@Composable
fun WeekComparisonTeaser(
    rows: List<MetricComparison>,
    loggedDays: Int,
    comparedDays: Int,
    partialSpan: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val headline = listOf(WeekMetric.AVG_INTAKE, WeekMetric.AVG_BALANCE, WeekMetric.CARDIO_MINUTES)
        .mapNotNull { m -> rows.firstOrNull { it.metric == m } }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("VS LAST WEEK", style = MaterialTheme.typography.labelSmall, color = MiTextSecondary)
                    Text(
                        comparisonCaption(loggedDays, comparedDays, partialSpan),
                        style = MaterialTheme.typography.bodySmall,
                        color = MiTextSecondary
                    )
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Open comparison", tint = MiTextSecondary)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                headline.forEach { row ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.metric.label, style = MaterialTheme.typography.labelSmall, color = MiTextSecondary)
                        Text(
                            formatDelta(row.delta, row.metric.unit),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/** Full week-over-week page: day-by-day paired bars + per-metric this/last bars. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekComparisonPage(
    rows: List<MetricComparison>,
    loggedDays: Int,
    comparedDays: Int,
    partialSpan: String?,
    showCarbsFat: Boolean,
    onToggleCarbsFat: () -> Unit,
    days: List<String>,
    balances: List<DailyBalance>,
    /** Current-week days that count (today excluded); other days show no current bar. */
    averagingDays: Set<String>,
    previousBalances: List<DailyBalance>,
    previousDays: List<String>,
    modifier: Modifier = Modifier
) {
    var lens by remember { mutableStateOf(DashboardViewMode.INTAKE) }
    val visible = rows.filter { !it.metric.optional || showCarbsFat }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            comparisonCaption(loggedDays, comparedDays, partialSpan),
            style = MaterialTheme.typography.bodySmall,
            color = MiTextSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        ComparisonCard(title = "DAY BY DAY") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DashboardViewMode.entries.forEach { mode ->
                    FilterChip(
                        selected = lens == mode,
                        onClick = { lens = mode },
                        label = { Text(mode.label) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            DailyPairedChart(
                current = days.map { d -> if (d in averagingDays) balances.dayValue(d, lens) else null },
                previous = previousDays.map { d -> previousBalances.dayValue(d, lens) },
                labels = days.map { it.toDayAbbr().take(1) },
                diverging = lens.isDiverging
            )
            Spacer(Modifier.height(8.dp))
            Legend()
        }

        ComparisonCard(title = "AVERAGES") {
            visible.forEachIndexed { index, row ->
                MetricBars(row)
                if (index < visible.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
            TextButton(onClick = onToggleCarbsFat, modifier = Modifier.align(Alignment.End)) {
                Text(if (showCarbsFat) "Hide carbs & fat" else "Show carbs & fat")
            }
        }
    }
}

@Composable
private fun ComparisonCard(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MiTextSecondary)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** Unlogged days (no intake) have no intake/balance; burn shows whenever recorded. */
private fun List<DailyBalance>.dayValue(date: String, lens: DashboardViewMode): Double? {
    val b = firstOrNull { it.date == date } ?: return null
    return when (lens) {
        DashboardViewMode.EXPENDITURE -> b.burn.takeIf { it > 0 }
        else -> if (b.intake > 0) lens.valueOf(b) else null
    }
}

@Composable
private fun DailyPairedChart(current: List<Double?>, previous: List<Double?>, labels: List<String>, diverging: Boolean) {
    val currentColor = MiOrange
    val previousColor = MaterialTheme.colorScheme.outlineVariant
    val axisColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val maxAbs = (current + previous).filterNotNull().maxOfOrNull { abs(it) }?.takeIf { it > 0 } ?: 1.0
    val slots = labels.size

    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        val slotW = size.width / slots
        val barW = slotW * 0.3f
        val gap = slotW * 0.06f
        val zeroY = if (diverging) size.height / 2 else size.height
        val scale = (if (diverging) size.height / 2 else size.height) / maxAbs.toFloat()

        fun bar(v: Double?, left: Float, color: Color) {
            if (v == null) return
            val h = (abs(v).toFloat() * scale).coerceAtLeast(2f)
            val top = if (v >= 0) zeroY - h else zeroY
            drawRoundRect(color, Offset(left, top), Size(barW, h), CornerRadius(barW / 3))
        }

        for (i in 0 until slots) {
            val cx = slotW * i + slotW / 2
            bar(previous.getOrNull(i), cx - gap / 2 - barW, previousColor)
            bar(current.getOrNull(i), cx + gap / 2, currentColor)
        }
        drawLine(axisColor, Offset(0f, zeroY), Offset(size.width, zeroY), 1.dp.toPx())
    }
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        labels.forEach {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MiTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(MiOrange, "This week")
        LegendItem(MaterialTheme.colorScheme.outlineVariant, "Last week")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MiTextSecondary)
    }
}

@Composable
private fun MetricBars(row: MetricComparison) {
    val signed = row.metric.isSigned
    val maxAbs = max(abs(row.current ?: 0.0), abs(row.previous ?: 0.0)).takeIf { it > 0 } ?: 1.0
    Column(modifier = Modifier.padding(vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.metric.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                formatDelta(row.delta, row.metric.unit),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        BarLine("This week", row.current, row.metric, maxAbs, signed, MiOrange)
        Spacer(Modifier.height(6.dp))
        BarLine("Last week", row.previous, row.metric, maxAbs, signed, MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun BarLine(label: String, value: Double?, metric: WeekMetric, maxAbs: Double, signed: Boolean, color: Color) {
    val track = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val axis = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MiTextSecondary, modifier = Modifier.width(64.dp))
        Canvas(modifier = Modifier.weight(1f).height(10.dp)) {
            val h = size.height
            drawRoundRect(track, Offset.Zero, size, CornerRadius(h / 2))
            if (value != null) {
                val frac = (abs(value) / maxAbs).toFloat()
                val (left, len) = if (signed) {
                    val center = size.width / 2
                    val len = frac * center
                    (if (value >= 0) center else center - len) to len
                } else {
                    0f to frac * size.width
                }
                if (len > 0f) drawRoundRect(color, Offset(left, 0f), Size(len.coerceAtLeast(h), h), CornerRadius(h / 2))
            }
            if (signed) drawLine(axis, Offset(size.width / 2, -2f), Offset(size.width / 2, h + 2f), 1.dp.toPx())
        }
        Text(
            formatValue(value, metric, metric.unit),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.width(88.dp)
        )
    }
}

private val WeekMetric.isSigned: Boolean
    get() = this == WeekMetric.AVG_BALANCE || this == WeekMetric.NET_BALANCE

private fun formatValue(value: Double?, metric: WeekMetric, unit: String): String {
    if (value == null) return "—"
    val v = value.roundToInt()
    val sign = if (metric.isSigned && v > 0) "+" else ""
    return "$sign${groupThousands(v)} $unit"
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

private fun groupThousands(v: Int): String {
    val digits = abs(v).toString().reversed().chunked(3).joinToString(",").reversed()
    return if (v < 0) "-$digits" else digits
}
