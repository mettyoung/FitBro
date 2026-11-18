package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.model.DailyBalance
import com.mettyoung.fitbro.data.model.WeekMetric
import com.mettyoung.fitbro.ui.MiTextSecondary
import com.mettyoung.fitbro.util.toShortDate
import kotlin.math.ceil

/**
 * Intake / Expenditure drill-in, health-app layout: big value (week average, or the tapped
 * day), day-by-day bar chart with a gridded axis, summary card (highest / lowest / average).
 * The caller renders the history list below.
 */
@Composable
internal fun MetricDetailHeader(
    balances: List<DailyBalance>,
    days: List<String>,
    averagingDays: Set<String>,
    excludesToday: Boolean,
    viewMode: DashboardViewMode,
    modifier: Modifier = Modifier
) {
    val metric = if (viewMode == DashboardViewMode.EXPENDITURE) WeekMetric.AVG_BURN else WeekMetric.AVG_INTAKE
    val style = metricStyles().getValue(metric)
    val values = remember(balances, days, metric) { dailySeries(metric, days, balances, emptyList()) { true } }
    // Stats over completed (averaging) days only, matching the Dashboard tiles.
    val stats = remember(values, days, averagingDays) {
        days.indices.mapNotNull { i -> values[i]?.takeIf { days[i] in averagingDays }?.let { days[i] to it } }
    }
    val average = stats.takeIf { it.isNotEmpty() }?.map { it.second }?.average()
    var selected by remember(days, viewMode) { mutableStateOf<Int?>(null) }

    Column(modifier = modifier) {
        // Hero value
        val sel = selected
        val heroValue = if (sel != null) values[sel] else average
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = heroValue.grouped(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(6.dp))
            Text("kcal", style = MaterialTheme.typography.titleMedium, color = MiTextSecondary, modifier = Modifier.padding(bottom = 10.dp))
        }
        Text(
            text = if (sel != null) "${days[sel].toDayAbbr().titleCase()}, ${days[sel].toShortDate()}"
                else heroCaption(stats.map { it.first }, excludesToday),
            style = MaterialTheme.typography.bodyLarge,
            color = MiTextSecondary
        )

        Spacer(Modifier.height(24.dp))
        DailyBarChart(
            values = values,
            labels = days.map { it.toDayAbbr().titleCase() },
            color = style.color,
            selected = selected,
            onSelect = { i -> selected = if (selected == i) null else i },
            modifier = Modifier.fillMaxWidth().height(240.dp)
        )

        Spacer(Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(style.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(style.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Weekly summary", style = MaterialTheme.typography.titleMedium)
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                val high = stats.maxByOrNull { it.second }
                val low = stats.minByOrNull { it.second }
                Row(Modifier.fillMaxWidth()) {
                    SummaryStat(high?.second, "Highest" + (high?.let { " · ${it.first.toDayAbbr().titleCase()}" } ?: ""), Modifier.weight(1f))
                    SummaryStat(low?.second, "Lowest" + (low?.let { " · ${it.first.toDayAbbr().titleCase()}" } ?: ""), Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth()) {
                    SummaryStat(average, "Average", Modifier.weight(1f))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${stats.size} of ${averagingDays.size}",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text("Days logged", style = MaterialTheme.typography.bodyMedium, color = MiTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(value: Double?, label: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value.grouped(), style = MaterialTheme.typography.headlineMedium)
            if (value != null) {
                Spacer(Modifier.width(4.dp))
                Text("kcal", style = MaterialTheme.typography.bodySmall, color = MiTextSecondary, modifier = Modifier.padding(bottom = 4.dp))
            }
        }
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MiTextSecondary)
    }
}

private fun heroCaption(statDays: List<String>, excludesToday: Boolean): String {
    if (statDays.isEmpty()) return if (excludesToday) "Daily average · no completed days yet" else "Daily average · no logged days"
    val range = statDays.first().toDayAbbr().titleCase().let { first ->
        val last = statDays.last().toDayAbbr().titleCase()
        if (first == last) first else "$first–$last"
    }
    return "Daily average · $range" + if (excludesToday) " · today excluded" else ""
}

private fun String.titleCase() = lowercase().replaceFirstChar { it.uppercase() }

/**
 * Bars on a gridded axis (0 / mid / max, labels on the right). Tapping a bar selects it
 * and dims the others; [values] null = no bar for that slot.
 */
@Composable
private fun DailyBarChart(
    values: List<Double?>,
    labels: List<String>,
    color: Color,
    selected: Int?,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val max = values.filterNotNull().maxOrNull() ?: 0.0
    val axisMax = (ceil(max / 500.0) * 500.0).coerceAtLeast(500.0)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    val axisWidth = 44.dp

    Column(modifier) {
        Row(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp)) {
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Canvas(Modifier.fillMaxSize()) {
                    listOf(0f, 0.5f, 1f).forEach { f ->
                        val y = size.height * f
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                    }
                }
                Row(Modifier.fillMaxSize()) {
                    values.forEachIndexed { i, v ->
                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    enabled = v != null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onSelect(i) },
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            if (v != null) {
                                val alpha = if (selected == null || selected == i) 1f else 0.35f
                                Box(
                                    Modifier
                                        .width(14.dp)
                                        .height(maxHeight * (v / axisMax).toFloat().coerceIn(0.02f, 1f))
                                        .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                                        .background(color.copy(alpha = alpha))
                                )
                            }
                        }
                    }
                }
            }
            Box(Modifier.width(axisWidth).fillMaxHeight()) {
                val axisText: @Composable (String, Alignment, Int) -> Unit = { t, a, dy ->
                    Text(
                        t,
                        style = MaterialTheme.typography.labelMedium,
                        color = MiTextSecondary,
                        modifier = Modifier.align(a).offset(y = dy.dp)
                    )
                }
                axisText(groupThousands(axisMax.toInt()), Alignment.TopEnd, -8)
                axisText(groupThousands((axisMax / 2).toInt()), Alignment.CenterEnd, 0)
                axisText("0", Alignment.BottomEnd, 8)
            }
        }
        Row(Modifier.fillMaxWidth().padding(end = axisWidth)) {
            labels.forEachIndexed { i, l ->
                Text(
                    l,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected == i) MaterialTheme.colorScheme.onSurface else MiTextSecondary,
                    fontWeight = if (selected == i) FontWeight.Bold else FontWeight.Normal,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}
