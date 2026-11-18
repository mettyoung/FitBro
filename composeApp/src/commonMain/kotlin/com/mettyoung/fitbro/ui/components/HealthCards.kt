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
import com.mettyoung.fitbro.ui.MiTextSecondary
import kotlin.math.abs
import kotlin.math.max

// Health-app-style building blocks (rings hero, 3-up stat card, 2-column metric tiles)
// shared by Trends and Compare.

/** Concentric progress rings, outermost first. 1.0 = full ring; values up to 2.0 draw a darker second lap. */
@Composable
fun ActivityRings(progress: List<Float>, colors: List<Color>, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(220.dp)) {
            val stroke = 22.dp.toPx()
            val gap = 5.dp.toPx()
            progress.forEachIndexed { i, raw ->
                val p = raw.coerceIn(0f, 2f)
                val color = colors[i]
                val inset = stroke / 2 + i * (stroke + gap)
                val topLeft = Offset(inset, inset)
                val arcSize = Size(size.width - 2 * inset, size.height - 2 * inset)
                val style = Stroke(width = stroke, cap = StrokeCap.Round)
                drawArc(color.copy(alpha = 0.18f), 0f, 360f, false, topLeft, arcSize, style = style)
                if (p > 0f) drawArc(color, -90f, 360f * p.coerceAtMost(1f), false, topLeft, arcSize, style = style)
                if (p > 1f) drawArc(lerp(color, Color.Black, 0.25f), -90f, 360f * (p - 1f), false, topLeft, arcSize, style = style)
            }
        }
    }
}

/** One column of [StatTripletCard]: "value" over "/target unit". */
class StatItem(
    val icon: ImageVector,
    val color: Color,
    val label: String,
    val value: String,
    val target: String,
    val onClick: (() -> Unit)? = null
)

/** 3-up stat card with a divider and a footer row (icon + text, optional chevron). */
@Composable
fun StatTripletCard(
    items: List<StatItem>,
    footerIcon: ImageVector,
    footerIconTint: Color,
    footerText: String,
    onFooterClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items.forEach { item ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .then(if (item.onClick != null) Modifier.clickable(onClick = item.onClick) else Modifier)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(item.icon, contentDescription = null, tint = item.color, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                item.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (item.onClick != null) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MiTextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            item.value,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(item.target, style = MaterialTheme.typography.bodySmall, color = MiTextSecondary, maxLines = 1)
                    }
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = if (onFooterClick != null) Modifier.fillMaxWidth().clickable(onClick = onFooterClick) else Modifier.fillMaxWidth()
            ) {
                Icon(footerIcon, contentDescription = null, tint = footerIconTint, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    footerText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (onFooterClick != null) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MiTextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

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

/** Tile: coloured icon badge, title, big value, subtitle, then [chart] pinned to the bottom. */
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
            Box(
                modifier = Modifier.size(32.dp).background(color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MiTextSecondary)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(16.dp))
            chart()
        }
    }
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.large, colors = colors, content = content)
    } else {
        Card(modifier = modifier, shape = MaterialTheme.shapes.large, colors = colors, content = content)
    }
}

/**
 * Heart-rate-style tick chart, one slot per day. [previous] (optional) draws a muted tick beside
 * each coloured [current] tick. Null values leave the slot empty.
 */
@Composable
fun MiniTicks(
    current: List<Double?>,
    color: Color,
    firstLabel: String,
    lastLabel: String,
    previous: List<Double?>? = null,
    diverging: Boolean = false
) {
    val muted = MaterialTheme.colorScheme.outlineVariant
    val axis = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val all = current + (previous ?: emptyList())
    val maxAbs = all.filterNotNull().maxOfOrNull { abs(it) }?.takeIf { it > 0 } ?: 1.0
    Canvas(modifier = Modifier.fillMaxWidth().height(56.dp)) {
        val slots = max(current.size, previous?.size ?: 0).coerceAtLeast(1)
        val slotW = size.width / slots
        val paired = previous != null
        val barW = (slotW * if (paired) 0.22f else 0.4f).coerceAtMost((if (paired) 6 else 10).dp.toPx())
        val zeroY = if (diverging) size.height / 2 else size.height
        val scale = (if (diverging) size.height / 2 else size.height) / maxAbs.toFloat()
        fun tick(v: Double?, left: Float, c: Color) {
            if (v == null) return
            val h = (abs(v).toFloat() * scale).coerceAtLeast(2f)
            val top = if (v >= 0) zeroY - h else zeroY
            drawRoundRect(c, Offset(left, top), Size(barW, h), CornerRadius(barW / 2))
        }
        for (i in 0 until slots) {
            val cx = slotW * i + slotW / 2
            if (paired) {
                tick(previous.getOrNull(i), cx - barW - 1.dp.toPx(), muted)
                tick(current.getOrNull(i), cx + 1.dp.toPx(), color)
            } else {
                tick(current.getOrNull(i), cx - barW / 2, color)
            }
        }
        drawLine(axis, Offset(0f, zeroY), Offset(size.width, zeroY), 1.dp.toPx())
    }
    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(firstLabel, style = MaterialTheme.typography.labelSmall, color = MiTextSecondary, modifier = Modifier.weight(1f))
        Text(lastLabel, style = MaterialTheme.typography.labelSmall, color = MiTextSecondary)
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
                Text(formatOneDecimal(it), style = MaterialTheme.typography.labelSmall, color = MiTextSecondary)
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
