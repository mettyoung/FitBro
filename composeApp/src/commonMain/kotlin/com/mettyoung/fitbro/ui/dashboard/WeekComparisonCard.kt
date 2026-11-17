package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.model.MetricComparison
import com.mettyoung.fitbro.data.model.WeekMetric
import com.mettyoung.fitbro.ui.MiTextSecondary
import kotlin.math.abs
import kotlin.math.roundToInt

/** US-002: week-over-week comparison. Deltas are neutral-coloured (FitBro doesn't know the goal direction). */
@Composable
fun WeekComparisonCard(
    rows: List<MetricComparison>,
    loggedDays: Int,
    /** Completed days compared (current week excludes today); 0 on the week's first day. */
    comparedDays: Int,
    /** e.g. "Mon–Wed" for a partial current week; null for a full week. */
    partialSpan: String?,
    showCarbsFat: Boolean,
    onToggleCarbsFat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visible = rows.filter { !it.metric.optional || showCarbsFat }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("VS LAST WEEK", style = MaterialTheme.typography.labelSmall, color = MiTextSecondary)
            Text(
                text = when {
                    comparedDays == 0 -> "Starts after today — compares completed days only"
                    partialSpan != null -> "$partialSpan vs $partialSpan last week · $loggedDays of $comparedDays days logged"
                    else -> "Full week · $loggedDays of 7 days logged"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MiTextSecondary
            )
            Spacer(Modifier.height(8.dp))
            visible.forEachIndexed { index, row ->
                ComparisonRow(row)
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
private fun ComparisonRow(row: MetricComparison) {
    val unit = row.metric.unit
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = row.metric.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatValue(row.current, row.metric, unit),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "was ${formatValue(row.previous, row.metric, unit)}",
                style = MaterialTheme.typography.labelSmall,
                color = MiTextSecondary
            )
        }
        Text(
            text = formatDelta(row.delta, unit),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

private fun formatValue(value: Double?, metric: WeekMetric, unit: String): String {
    if (value == null) return "—"
    val v = value.roundToInt()
    val signed = metric == WeekMetric.AVG_BALANCE || metric == WeekMetric.NET_BALANCE
    val sign = if (signed && v > 0) "+" else ""
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
