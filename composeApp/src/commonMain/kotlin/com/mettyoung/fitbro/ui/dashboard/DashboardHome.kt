package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.model.BodyMetric
import com.mettyoung.fitbro.data.model.DailyBalance
import com.mettyoung.fitbro.data.model.EE_MIN_LOGGED_DAYS
import com.mettyoung.fitbro.data.model.EE_MIN_SPAN_DAYS
import com.mettyoung.fitbro.data.model.ExpenditureEstimate
import com.mettyoung.fitbro.data.model.TrendSummary
import com.mettyoung.fitbro.data.model.WeekGoals
import com.mettyoung.fitbro.data.model.WeekMetric
import com.mettyoung.fitbro.data.model.WeekSummary
import com.mettyoung.fitbro.data.model.WeekToDateBalance
import com.mettyoung.fitbro.ui.ColorBodyFat
import com.mettyoung.fitbro.ui.ColorBurn
import com.mettyoung.fitbro.ui.ColorCardio
import com.mettyoung.fitbro.ui.ColorLeanMass
import com.mettyoung.fitbro.ui.ColorWeight
import com.mettyoung.fitbro.ui.TextSecondary
import com.mettyoung.fitbro.ui.NegativeColor
import com.mettyoung.fitbro.ui.PositiveColor
import com.mettyoung.fitbro.ui.components.MetricTileCard
import com.mettyoung.fitbro.ui.components.MiniLine
import androidx.compose.ui.draw.clip
import com.mettyoung.fitbro.ui.components.TrendChart
import com.mettyoung.fitbro.ui.components.TwoColumnTiles
import com.mettyoung.fitbro.ui.components.formatOneDecimal
import com.mettyoung.fitbro.util.toShortDate
import kotlin.math.roundToInt

/** Where a Dashboard tile drills in to. */
sealed interface DashboardPage {
    data class Lens(val mode: DashboardViewMode) : DashboardPage
    data class Body(val metric: BodyMetric) : DashboardPage
    data object Estimate : DashboardPage
    /** Week's strength sessions (from the Resistance card). */
    data object Resistance : DashboardPage
    /** One workout's Health Connect detail; back returns to [Resistance]. */
    data object Workout : DashboardPage
}

internal class BodyStyle(val icon: ImageVector, val color: Color)

private fun bodyStyle(metric: BodyMetric) = when (metric) {
    BodyMetric.WEIGHT -> BodyStyle(Icons.Filled.MonitorWeight, ColorWeight)
    BodyMetric.LEAN_MASS -> BodyStyle(Icons.Filled.FitnessCenter, ColorLeanMass)
    BodyMetric.BODY_FAT -> BodyStyle(Icons.Filled.Opacity, ColorBodyFat)
}

private fun kg(v: Double?) = v?.let { "${formatOneDecimal(it)} kg" } ?: "—"

private fun kgDelta(v: Double?): String = when {
    v == null -> "—"
    kotlin.math.abs(v) < 0.05 -> "same"
    v > 0 -> "▲ ${formatOneDecimal(v)} kg"
    else -> "▼ ${formatOneDecimal(-v)} kg"
}

/** US-002..007: Dashboard home. */
@Composable
internal fun DashboardHome(
    balances: List<DailyBalance>,
    days: List<String>,
    averagingDays: Set<String>,
    weekToDate: WeekToDateBalance?,
    balanceCaption: String,
    summary: WeekSummary,
    goals: WeekGoals?,
    resistanceCount: Int,
    resistanceTarget: Int,
    cardioMinutes: Int,
    cardioSessions: Int,
    cardioTarget: Int,
    body: BodyTrends,
    onOpen: (DashboardPage) -> Unit,
    onOpenCardio: () -> Unit,
    onBarClick: (DailyBalance) -> Unit,
    modifier: Modifier = Modifier
) {
    val styles = metricStyles()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WeekToDateBalanceCard(
            balances = balances.filter { it.date in averagingDays },
            days = days,
            weekToDate = weekToDate,
            caption = balanceCaption,
            onBarClick = onBarClick,
            modifier = Modifier.weight(1f)
        )

        TwoColumnTiles(listOf(WeekMetric.AVG_INTAKE, WeekMetric.AVG_BURN)) { m, tileModifier ->
            val style = styles.getValue(m)
            val isIntake = m == WeekMetric.AVG_INTAKE
            // Both bars measure against the calorie goal: intake over it = surplus, burn over it = deficit.
            val avg = m.valueOf(summary)
            val goal = goals?.caloriesKcal
            val overColor = if (isIntake) MaterialTheme.colorScheme.error else NegativeColor
            MetricTileCard(
                icon = style.icon,
                color = style.color,
                title = if (isIntake) "Average intake" else "Average burn",
                value = avg?.grouped() ?: "—",
                unit = "kcal",
                subtitle = goalSubtitle(avg, goal, isIntake),
                modifier = tileModifier,
                onClick = {
                    onOpen(DashboardPage.Lens(if (m == WeekMetric.AVG_INTAKE) DashboardViewMode.INTAKE else DashboardViewMode.EXPENDITURE))
                }
            ) {
                GoalBar(value = avg, goal = goal, color = style.color, overColor = overColor)
            }
        }

        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GoalCard(
                icon = Icons.Filled.FitnessCenter,
                color = ColorBurn,
                title = "Resistance",
                value = "$resistanceCount",
                target = "/ $resistanceTarget",
                subtitle = when {
                    resistanceCount > resistanceTarget -> "Target hit · +${resistanceCount - resistanceTarget}"
                    resistanceCount == resistanceTarget -> "Target hit"
                    else -> "${resistanceTarget - resistanceCount} to go"
                },
                onClick = { onOpen(DashboardPage.Resistance) },
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) { SessionSegments(resistanceCount, resistanceTarget, ColorBurn) }
            GoalCard(
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                color = ColorCardio,
                title = "Cardio",
                value = "$cardioMinutes",
                target = "/ $cardioTarget min",
                subtitle = if (cardioMinutes >= cardioTarget) "Target hit"
                else "${cardioTarget - cardioMinutes} min to go",
                onClick = onOpenCardio,
                modifier = Modifier.weight(1f).fillMaxHeight()
            ) { ProgressBar(cardioMinutes.toFloat() / cardioTarget.coerceAtLeast(1), ColorCardio) }
        }

    }
}

/** Insights tab: estimated expenditure + body trends (28-day window ending the last completed day). */
@Composable
internal fun InsightsHome(
    body: BodyTrends,
    onOpen: (DashboardPage) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val insights: List<DashboardPage> = listOf(DashboardPage.Estimate) + BodyMetric.entries.map { DashboardPage.Body(it) }
        TwoColumnTiles(insights) { page, tileModifier ->
            when (page) {
                DashboardPage.Estimate -> MetricTileCard(
                    icon = Icons.Filled.Calculate,
                    color = ColorBurn,
                    title = "Estimated expenditure",
                    value = body.estimate?.kcalPerDay?.grouped() ?: "—",
                    unit = "kcal",
                    subtitle = if (body.estimate != null) "intake + weight trend" else "Needs $EE_MIN_LOGGED_DAYS logged days + 2 weigh-ins",
                    modifier = tileModifier,
                    onClick = { onOpen(page) }
                ) { MiniLine(body.estimateWindowSeries, ColorBurn) }
                is DashboardPage.Body -> {
                    val trend = body.trends[page.metric]
                    val style = bodyStyle(page.metric)
                    MetricTileCard(
                        icon = style.icon,
                        color = style.color,
                        title = page.metric.label,
                        value = trend?.value?.let { formatOneDecimal(it) } ?: "—",
                        unit = "kg",
                        subtitle = bodySubtitle(page.metric, trend, body.access),
                        modifier = tileModifier,
                        onClick = { onOpen(page) }
                    ) {
                        MiniLine(
                            values = trend?.trend ?: emptyList(),
                            color = style.color,
                            secondary = if (page.metric == BodyMetric.WEIGHT) trend?.scale else null
                        )
                    }
                }
                else -> Unit
            }
        }
    }
}

private fun goalSubtitle(avg: Double?, goal: Double?, isIntake: Boolean): String = when {
    goal == null || goal <= 0 -> if (isIntake) "Set a calorie goal" else "Health Connect burn"
    avg == null -> "goal ${goal.grouped()} kcal"
    avg > goal -> "${(avg - goal).grouped()} over ${goal.grouped()} goal"
    else -> "${(goal - avg).grouped()} under ${goal.grouped()} goal"
}

/**
 * Single bar against [goal]. Under goal: [color] fills value/goal. Over goal: the bar is
 * scaled to the value, the goal part stays [color] and the overshoot is [overColor].
 */
@Composable
private fun GoalBar(value: Double?, goal: Double?, color: Color, overColor: Color) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val shape = RoundedCornerShape(5.dp)
    Row(Modifier.fillMaxWidth().height(10.dp).clip(shape).background(track)) {
        if (value == null || value <= 0 || goal == null || goal <= 0) return@Row
        val base = minOf(value, goal).toFloat()
        val over = (value - goal).coerceAtLeast(0.0).toFloat()
        val rest = (goal - value).coerceAtLeast(0.0).toFloat()
        Box(Modifier.weight(base).fillMaxHeight().background(color))
        if (over > 0f) {
            Spacer(Modifier.width(2.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surface))
            Box(Modifier.weight(over).fillMaxHeight().background(overColor))
        }
        if (rest > 0f) Spacer(Modifier.weight(rest))
    }
}

private fun bodySubtitle(metric: BodyMetric, trend: TrendSummary?, access: BodyAccess): String = when {
    access == BodyAccess.NO_PERMISSION && trend?.value == null -> "Allow body data in Health Connect"
    trend?.value == null && metric == BodyMetric.WEIGHT -> "No weigh-ins"
    trend?.value == null -> "Not synced from your scale"
    trend.delta == null -> "Trend building"
    else -> "${kgDelta(trend.delta)} vs 4 wks ago"
}

@Composable
private fun WeekToDateBalanceCard(
    balances: List<DailyBalance>,
    days: List<String>,
    weekToDate: WeekToDateBalance?,
    caption: String,
    onBarClick: (DailyBalance) -> Unit,
    modifier: Modifier = Modifier
) {
    val total = weekToDate?.total
    // Not clickable as a whole: each bar opens that day's breakdown.
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxHeight().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Balance, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Week-to-date balance", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = total?.let { (if (it > 0) "+" else "") + it.grouped() } ?: "—",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = when {
                        total == null -> MaterialTheme.colorScheme.onSurface
                        total >= 0 -> PositiveColor
                        else -> NegativeColor
                    }
                )
                Spacer(Modifier.width(6.dp))
                Text("kcal", style = MaterialTheme.typography.bodyLarge, color = TextSecondary, modifier = Modifier.padding(bottom = 6.dp))
            }
            Text(caption, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(12.dp))
            CalorieBalanceChart(
                balances = balances,
                onBarClick = onBarClick,
                modifier = Modifier.fillMaxWidth().weight(1f).heightIn(min = 120.dp),
                valueSelector = { it.balance },
                diverging = true,
                showDayLabels = true,
                showValues = true,
                days = days
            )
        }
    }
}

@Composable
private fun GoalCard(
    icon: ImageVector,
    color: Color,
    title: String,
    value: String,
    target: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    indicator: @Composable () -> Unit
) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val content: @Composable () -> Unit = {
        Column(Modifier.fillMaxHeight().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(4.dp))
                Text(target, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(10.dp))
            indicator()
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
    if (onClick != null) Card(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.large, colors = colors) { content() }
    else Card(modifier = modifier, shape = MaterialTheme.shapes.large, colors = colors) { content() }
}

/** One pill per target session; extra sessions beyond the target add filled pills (max 14). */
@Composable
private fun SessionSegments(done: Int, target: Int, color: Color) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val slots = maxOf(done, target).coerceIn(1, 14)
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(slots) { i ->
            Box(Modifier.weight(1f).height(8.dp).background(if (i < done) color else track, RoundedCornerShape(4.dp)))
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(8.dp).background(color, RoundedCornerShape(4.dp)))
    }
}

/** US-006 detail: Average + Difference header, scale dots + trend line, weigh-in list. */
@Composable
internal fun BodyTrendDetail(trend: TrendSummary?, metric: BodyMetric, access: BodyAccess) {
    val style = bodyStyle(metric)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DetailCard {
            Row {
                HeaderStat("Average", kg(trend?.average), Modifier.weight(1f))
                HeaderStat("Difference", kgDelta(trend?.delta), Modifier.weight(1f))
            }
            trend?.windowDays?.let {
                Text("${it.first().toShortDate()} – ${it.last().toShortDate()}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Spacer(Modifier.height(16.dp))
            if (trend == null || trend.weighIns.isEmpty()) {
                Text(
                    bodySubtitle(metric, trend, access),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 40.dp)
                )
            } else {
                TrendChart(scale = trend.scale, trend = trend.trend, color = style.color, unit = "kg")
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(trend.windowDays.first().toShortDate(), style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.weight(1f))
                    Text(trend.windowDays.last().toShortDate(), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendDot(MaterialTheme.colorScheme.outline, if (metric == BodyMetric.BODY_FAT) "Measured" else "Scale")
                    LegendDot(style.color, "Trend")
                }
            }
        }
        if (trend != null && trend.weighIns.isNotEmpty()) {
            DetailCard {
                Text("Weigh-ins", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                val trendByDate = trend.windowDays.zip(trend.trend).toMap()
                trend.weighIns.reversed().forEachIndexed { i, (date, v) ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${date.toDayAbbr()} ${date.toShortDate()}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Text(kg(v), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(12.dp))
                        Text("trend ${kg(trendByDate[date])}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
            }
        }
    }
}

/** US-007 detail. */
@Composable
internal fun EstimateDetail(body: BodyTrends, healthAvgBurn: Double?) {
    val estimate = body.estimate
    val days = body.estimateWindowDays
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DetailCard {
            HeaderStat("Estimated expenditure", estimate?.let { "${it.kcalPerDay.grouped()} kcal/day" } ?: "—", Modifier)
            if (days.isNotEmpty()) {
                Text("${days.first().toShortDate()} – ${days.last().toShortDate()}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Spacer(Modifier.height(16.dp))
            if (body.estimateWindowSeries.none { it != null }) {
                Text(
                    "Needs at least $EE_MIN_LOGGED_DAYS logged days and two weigh-ins $EE_MIN_SPAN_DAYS+ days apart in the last 28 days.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                // Dots above the line = surplus days, below = deficit days.
                TrendChart(
                    scale = body.windowIntake,
                    trend = body.estimateWindowSeries,
                    color = ColorBurn,
                    unit = "kcal",
                    pad = 100.0,
                    axisLabel = { it.grouped() }
                )
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(days.first().toShortDate(), style = MaterialTheme.typography.labelSmall, color = TextSecondary, modifier = Modifier.weight(1f))
                    Text(days.last().toShortDate(), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendDot(MaterialTheme.colorScheme.outline, "Intake")
                    LegendDot(ColorBurn, "Estimated expenditure")
                }
            }
        }
        if (estimate != null) {
            DetailCard {
                EstimateRow("Average intake", "${estimate.avgIntake.grouped()} kcal", "${estimate.loggedDays} logged days")
                EstimateRow("Trend weight change", kgDelta(estimate.trendChangeKg), "over ${estimate.spanDays} days")
                EstimateRow("Health Connect average", healthAvgBurn?.let { "${it.grouped()} kcal" } ?: "—", "this week's burn")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Average intake − trend change × 7,700 kcal/kg ÷ days between weigh-ins, over the 28 days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun EstimateRow(label: String, value: String, note: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(note, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DetailCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) { Column(Modifier.padding(20.dp)) { content() } }
}

@Composable
private fun HeaderStat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}

