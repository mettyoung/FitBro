package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mettyoung.fitbro.data.model.DailyBalance
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CalorieBalanceChart(
    balances: List<DailyBalance>,
    onBarClick: (DailyBalance) -> Unit = {},
    modifier: Modifier = Modifier,
    valueSelector: (DailyBalance) -> Double = { it.balance },
    diverging: Boolean = true,
    showDayLabels: Boolean = true,
    /** Small value label at the end of each bar. */
    showValues: Boolean = false,
    /** Fixed slots (e.g. the 7 days of a week); days without a balance render as empty slots. */
    days: List<String>? = null
) {
    val slots: List<Pair<String, DailyBalance?>> = days?.map { d -> d to balances.firstOrNull { it.date == d } }
        ?: balances.map { it.date to it }
    if (balances.isEmpty() && days == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No data available", style = MaterialTheme.typography.bodyMedium, color = MiTextSecondary)
        }
        return
    }

    // Diverging (balance) keeps the original 100..3000 clamp; positive-only modes
    // normalize against the actual window max of the plotted value.
    val maxValue = if (diverging) {
        (balances.maxOfOrNull { abs(valueSelector(it)) } ?: 0.0).coerceAtLeast(100.0).coerceAtMost(3000.0)
    } else {
        (balances.maxOfOrNull { abs(valueSelector(it)) } ?: 0.0).coerceAtLeast(1.0)
    }

    Box(modifier = modifier) {
        // Zero Line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.Center)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            slots.forEach { (date, balance) ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (balance != null) {
                        CalorieBarItem(
                            balance = balance,
                            value = valueSelector(balance),
                            maxValue = maxValue,
                            diverging = diverging,
                            showLabel = showDayLabels,
                            showValue = showValues,
                            onClick = { onBarClick(balance) }
                        )
                    } else {
                        EmptySlot(date = date, showLabel = showDayLabels)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalorieBarItem(
    balance: DailyBalance,
    value: Double,
    maxValue: Double,
    diverging: Boolean,
    showLabel: Boolean,
    showValue: Boolean,
    onClick: () -> Unit
) {
    val positiveColor = MiOrange
    val negativeColor = MaterialTheme.colorScheme.tertiary
    // Positive-only modes always use the positive accent; diverging keys on sign.
    val color = if (!diverging || value >= 0) positiveColor else negativeColor
    val heightFactor = (abs(value) / maxValue).toFloat().coerceIn(0.1f, 1f)
    // Positive-only modes draw every bar in the top half (upward-only).
    val drawTop = !diverging || value > 0
    val drawBottom = diverging && value < 0

    val label = if (showValue) barValueLabel(value, signed = diverging) else null
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top half (positive)
        // Labels always sit on top of the bar; a deficit bar's top is the zero line.
        BarHalf(Modifier.weight(1f), drawTop, heightFactor, color, label.takeIf { drawTop }, top = true, labelOnly = drawBottom)
        Spacer(Modifier.height(4.dp)) // Space for the zero line area
        // Bottom half (negative)
        BarHalf(Modifier.weight(1f), drawBottom, heightFactor, color, null, top = false, reserveLabel = label != null)

        if (showLabel) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = balance.date.toDayInitial(),
                style = MaterialTheme.typography.labelSmall,
                color = MiTextSecondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** One half of a diverging bar; the optional [label] sits at the bar's outer end. */
@Composable
private fun BarHalf(
    modifier: Modifier,
    draw: Boolean,
    heightFactor: Float,
    color: Color,
    label: String?,
    top: Boolean,
    /** Deficit bar: draw only [label] (passed via labelOnly path) just above the zero line. */
    labelOnly: Boolean = false,
    /** Keep negative bars on the same scale as labelled positive ones. */
    reserveLabel: Boolean = false
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (labelOnly && label != null) {
            BarLabel(label, Modifier.align(Alignment.BottomCenter))
            return@BoxWithConstraints
        }
        if (!draw) return@BoxWithConstraints
        val labelSpace = if (label != null || reserveLabel) 14.dp else 0.dp
        val barHeight = (maxHeight - labelSpace).coerceAtLeast(0.dp) * heightFactor
        val shape = if (top) RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
            else RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (top) Arrangement.Bottom else Arrangement.Top
        ) {
            if (label != null) BarLabel(label)
            Box(Modifier.width(12.dp).height(barHeight).clip(shape).background(color))
        }
    }
}

@Composable
private fun BarLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 13.sp),
        color = MiOrange,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false
    )
}

/** Compact bar label: "-523", "+1.2k", "2.4k". */
internal fun barValueLabel(value: Double, signed: Boolean): String {
    val v = value.roundToInt()
    val a = abs(v)
    val body = if (a >= 1000) "${a / 1000}.${(a % 1000) / 100}k" else a.toString()
    val sign = when {
        v < 0 -> "−"
        signed && v > 0 -> "+"
        else -> ""
    }
    return sign + body
}

@Composable
private fun EmptySlot(date: String, showLabel: Boolean) {
    Column(
        modifier = Modifier.fillMaxHeight().padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        if (showLabel) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = date.toDayInitial(),
                style = MaterialTheme.typography.labelSmall,
                color = MiTextSecondary.copy(alpha = 0.4f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun String.toDayInitial(): String {
    val parts = split("-")
    if (parts.size != 3) return ""
    val year = parts[0].toIntOrNull() ?: return ""
    val month = parts[1].toIntOrNull() ?: return ""
    val day = parts[2].toIntOrNull() ?: return ""
    return dayOfWeekInitial(year, month, day)
}

private fun dayOfWeekInitial(year: Int, month: Int, day: Int): String {
    val m = if (month < 3) month + 12 else month
    val y = if (month < 3) year - 1 else year
    val k = y % 100
    val j = y / 100
    val h = (day + (13 * (m + 1)) / 5 + k + k / 4 + j / 4 - 2 * j).mod(7)
    return listOf("S", "S", "M", "T", "W", "T", "F")[h]
}
