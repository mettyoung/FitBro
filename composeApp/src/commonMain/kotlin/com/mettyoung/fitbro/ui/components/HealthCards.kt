package com.mettyoung.fitbro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.ui.TextSecondary
import kotlin.math.abs
import kotlin.math.max

// Health-app-style building blocks for the Dashboard: 2-column metric tiles, sparklines, trend chart.

/** Lays [items] out two per row with equal-height tiles. */
@Composable
fun <T> TwoColumnTiles(items: List<T>, tile: @Composable (item: T, modifier: Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { tile(it, Modifier.weight(1f).fillMaxHeight()) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/**
 * Dashboard tile, same anatomy as the goal cards: tinted icon + title row, big value,
 * subtitle, then [chart] pinned to the bottom.
 */
@Composable
fun MetricTileCard(
    icon: ImageVector,
    color: Color,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    chart: @Composable ColumnScope.() -> Unit
) {
    val content: @Composable ColumnScope.() -> Unit = {
        Column(modifier = Modifier.fillMaxHeight().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(10.dp))
            chart()
            Spacer(Modifier.height(8.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.large, colors = colors, content = content)
    } else {
        Card(modifier = modifier, shape = MaterialTheme.shapes.large, colors = colors, content = content)
    }
}

/** Sparkline for trends; [secondary] (e.g. raw scale readings) is drawn faint underneath. Nulls break the line. */
@Composable
fun MiniLine(
    values: List<Double?>,
    color: Color,
    secondary: List<Double?>? = null,
    modifier: Modifier = Modifier.fillMaxWidth().height(40.dp)
) {
    val faint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val all = (values + (secondary ?: emptyList())).filterNotNull()
    Canvas(modifier = modifier) {
        if (all.isEmpty()) return@Canvas
        val lo = all.min()
        val hi = all.max()
        val span = (hi - lo).takeIf { it > 1e-9 } ?: 1.0
        fun y(v: Double) = (size.height - ((v - lo) / span).toFloat() * size.height * 0.8f - size.height * 0.1f)
        fun path(series: List<Double?>, c: Color, width: Float) {
            val step = if (series.size > 1) size.width / (series.size - 1) else 0f
            var prev: Offset? = null
            series.forEachIndexed { i, v ->
                if (v == null) { prev = null; return@forEachIndexed }
                val pt = Offset(i * step, y(v))
                prev?.let { drawLine(c, it, pt, width, cap = StrokeCap.Round) }
                prev = pt
            }
        }
        secondary?.let { path(it, faint, 1.dp.toPx()) }
        path(values, color, 2.dp.toPx())
    }
}

/**
 * Detail chart: raw readings as faint dots, smoothed trend as a line with small circles
 * (MacroFactor-style). Y range auto-fits with a little padding; min/max labelled on the right.
 */
@Composable
fun TrendChart(scale: List<Double?>, trend: List<Double?>, color: Color, unit: String, modifier: Modifier = Modifier) {
    val dot = MaterialTheme.colorScheme.outline
    val grid = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val all = (scale + trend).filterNotNull()
    val lo = (all.minOrNull() ?: 0.0) - 0.3
    val hi = (all.maxOrNull() ?: 1.0) + 0.3
    Row(modifier = modifier.fillMaxWidth()) {
        Canvas(modifier = Modifier.weight(1f).height(200.dp)) {
            if (all.isEmpty()) return@Canvas
            val span = (hi - lo).takeIf { it > 1e-9 } ?: 1.0
            fun y(v: Double) = size.height - ((v - lo) / span).toFloat() * size.height
            val n = maxOf(scale.size, trend.size)
            val step = if (n > 1) size.width / (n - 1) else 0f
            listOf(0f, size.height / 2, size.height).forEach {
                drawLine(grid, Offset(0f, it), Offset(size.width, it), 1.dp.toPx())
            }
            scale.forEachIndexed { i, v -> if (v != null) drawCircle(dot, 2.5.dp.toPx(), Offset(i * step, y(v))) }
            var prev: Offset? = null
            trend.forEachIndexed { i, v ->
                if (v == null) { prev = null; return@forEachIndexed }
                val pt = Offset(i * step, y(v))
                prev?.let { drawLine(color, it, pt, 2.dp.toPx(), cap = StrokeCap.Round) }
                prev = pt
            }
            trend.forEachIndexed { i, v ->
                if (v != null && i % 3 == 0) drawCircle(color, 3.dp.toPx(), Offset(i * step, y(v)), style = Stroke(1.5.dp.toPx()))
            }
        }
        Column(
            modifier = Modifier.height(200.dp).padding(start = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(hi, (hi + lo) / 2, lo).forEach {
                Text(formatOneDecimal(it), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

/** "63.8" / "-0.4" without platform String.format. */
fun formatOneDecimal(v: Double): String {
    val r = kotlin.math.round(abs(v) * 10).toLong()
    val sign = if (v < 0 && r != 0L) "-" else ""
    return "$sign${r / 10}.${r % 10}"
}
