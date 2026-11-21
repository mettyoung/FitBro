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
import androidx.compose.foundation.layout.wrapContentHeight
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
import com.mettyoung.fitbro.ui.BrandPrimary
import com.mettyoung.fitbro.ui.TextSecondary
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
            Text("No data available", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        return
    }

    // Diverging (balance) keeps the original 100..3000 clamp; positive-only modes
    // normalize against the actual window max of the plotted value.
    // Diverging: the zero line sits where the data needs it — an all-deficit week uses the full
    // height below it instead of wasting the top half. Both sides share one kcal-per-dp scale.
    val values = balances.map(valueSelector)
    val posMax = (values.maxOrNull() ?: 0.0).coerceAtLeast(0.0)
    val negMax = (-(values.minOrNull() ?: 0.0)).coerceAtLeast(0.0)
    val maxValue = if (diverging) maxOf(posMax, negMax).coerceAtLeast(100.0)
        else (values.maxOfOrNull { abs(it) } ?: 0.0).coerceAtLeast(1.0)
    val topWeight = if (!diverging) 1f else (posMax / maxValue).toFloat()
    val bottomWeight = if (!diverging) 0f else (negMax / maxValue).toFloat()
    val split = if (diverging && posMax + negMax > 0) Split(topWeight, bottomWeight) else Split(1f, if (diverging) 1f else 0f)

    Box(modifier = modifier) {

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
                            split = split,
                            diverging = diverging,
                            showLabel = showDayLabels,
                            showValue = showValues,
                            onClick = { onBarClick(balance) }
                        )
                    } else {
                        EmptySlot(date = date, showLabel = showDayLabels, split = split, diverging = diverging)
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
    split: Split,
    diverging: Boolean,
    showLabel: Boolean,
    showValue: Boolean,
    onClick: () -> Unit
) {
    // Positive-only modes always use the positive accent; diverging keys on sign.
    val color = if (!diverging || value >= 0) BrandPrimary else MaterialTheme.colorScheme.tertiary
    val above = !diverging || value >= 0
    // Relative to its own side's max (each side's height is proportional to that max).
    val sideMax = maxValue * (if (above) split.top else split.bottom)
    val factor = if (sideMax <= 0) 0f else (abs(value) / sideMax).toFloat().coerceIn(0.06f, 1f)
    val label = if (showValue) barValueLabel(value, signed = diverging) else null

    SlotColumn(
        date = balance.date,
        showLabel = showLabel,
        split = split,
        diverging = diverging,
        modifier = Modifier.clickable(onClick = onClick),
        top = {
            val h = maxHeight
            // Bar grows up from zero; its label rides on top (overflowing into the label strip).
            // A deficit's label sits just above the zero line.
            Column(
                Modifier.fillMaxWidth().wrapContentHeight(Alignment.Bottom, unbounded = true),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (label != null) BarLabel(label)
                if (above) Box(Modifier.width(12.dp).height(h * factor).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(color))
            }
        },
        bottom = {
            if (!above) Box(
                Modifier.align(Alignment.TopCenter).width(12.dp).height(maxHeight * factor)
                    .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)).background(color)
            )
        }
    )
}

@Composable
private fun BarLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 13.sp),
        color = BrandPrimary,
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

private val LabelStrip = 16.dp

/** Relative heights of the regions above / below zero. */
private class Split(val top: Float, val bottom: Float)

/**
 * One day's column: label strip, region above zero, zero line, region below zero, day initial.
 * Every slot uses the same structure so zero lines and labels align across the week.
 */
@Composable
private fun SlotColumn(
    date: String,
    showLabel: Boolean,
    split: Split,
    diverging: Boolean,
    modifier: Modifier = Modifier,
    labelAlpha: Float = 1f,
    top: @Composable androidx.compose.foundation.layout.BoxWithConstraintsScope.() -> Unit = {},
    bottom: @Composable androidx.compose.foundation.layout.BoxWithConstraintsScope.() -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxHeight().fillMaxWidth().padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(LabelStrip))
        BoxWithConstraints(
            (if (split.top > 0f) Modifier.weight(split.top) else Modifier.height(0.dp)).fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter,
            content = top
        )
        Box(
            Modifier.fillMaxWidth().padding(vertical = 1.dp).height(1.dp)
                .background(if (diverging) MaterialTheme.colorScheme.outlineVariant else Color.Transparent)
        )
        if (split.bottom > 0f) BoxWithConstraints(Modifier.weight(split.bottom).fillMaxWidth(), content = bottom)
        if (showLabel) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = date.toDayInitial(),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = labelAlpha),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptySlot(date: String, showLabel: Boolean, split: Split, diverging: Boolean) =
    SlotColumn(date, showLabel, split, diverging, labelAlpha = 0.4f)

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
