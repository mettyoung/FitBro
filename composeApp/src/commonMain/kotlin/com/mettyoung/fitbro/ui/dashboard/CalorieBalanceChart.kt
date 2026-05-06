package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mettyoung.fitbro.data.model.DailyBalance
import kotlin.math.abs

@Composable
fun CalorieBalanceChart(
    balances: List<DailyBalance>,
    onBarClick: (DailyBalance) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (balances.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No data available", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val positiveColor = Color(0xFF4CAF50)
    val negativeColor = Color(0xFFF44336)
    val maxAbsBalance = balances.maxOf { abs(it.balance) }.coerceAtLeast(1.0)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val gridColor = onSurface.copy(alpha = 0.15f)
    val zeroLineColor = onSurface.copy(alpha = 0.5f)

    Column(modifier = modifier) {
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Y-axis labels
            Box(modifier = Modifier.width(52.dp).fillMaxHeight()) {
                Text(
                    text = formatCalories(maxAbsBalance),
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = onSurface
                )
                Text(
                    text = "0",
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = onSurface
                )
                Text(
                    text = "-${formatCalories(maxAbsBalance)}",
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = onSurface
                )
            }

            Canvas(modifier = Modifier.weight(1f).fillMaxHeight().pointerInput(balances) {
                detectTapGestures { offset ->
                    val slotWidth = size.width / balances.size.toFloat()
                    val index = (offset.x / slotWidth).toInt().coerceIn(0, balances.size - 1)
                    onBarClick(balances[index])
                }
            }) {
                val chartWidth = size.width
                val chartHeight = size.height
                val zeroY = chartHeight / 2f
                val slotWidth = chartWidth / balances.size
                val barWidth = slotWidth * 0.5f

                // Grid lines at ±50% of max (25% and 75% of chart height)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, chartHeight * 0.25f),
                    end = Offset(chartWidth, chartHeight * 0.25f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, chartHeight * 0.75f),
                    end = Offset(chartWidth, chartHeight * 0.75f),
                    strokeWidth = 1.dp.toPx()
                )

                // Zero line
                drawLine(
                    color = zeroLineColor,
                    start = Offset(0f, zeroY),
                    end = Offset(chartWidth, zeroY),
                    strokeWidth = 1.5.dp.toPx()
                )

                balances.forEachIndexed { index, balance ->
                    val centerX = index * slotWidth + slotWidth / 2f
                    val normalizedHeight = ((abs(balance.balance) / maxAbsBalance).toFloat() * (chartHeight / 2f))
                        .coerceAtLeast(1f)
                    val color = if (balance.balance >= 0) positiveColor else negativeColor
                    val top = if (balance.balance >= 0) zeroY - normalizedHeight else zeroY

                    drawRect(
                        color = color,
                        topLeft = Offset(centerX - barWidth / 2f, top),
                        size = Size(barWidth, normalizedHeight)
                    )
                }
            }
        }

        // X-axis day labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 52.dp, top = 4.dp)
        ) {
            balances.forEach { balance ->
                Text(
                    text = balance.date.toDayLabel(),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun formatCalories(value: Double): String {
    val v = value.toInt()
    return if (v >= 1000) "${v / 1000}.${(v % 1000) / 100}k" else v.toString()
}

private fun String.toDayLabel(): String {
    val parts = split("-")
    if (parts.size != 3) return this
    val year = parts[0].toIntOrNull() ?: return this
    val month = parts[1].toIntOrNull() ?: return this
    val day = parts[2].toIntOrNull() ?: return this
    return dayOfWeekAbbr(year, month, day)
}

// Zeller's congruence: h=0→Sat, h=1→Sun, h=2→Mon, h=3→Tue, h=4→Wed, h=5→Thu, h=6→Fri
private fun dayOfWeekAbbr(year: Int, month: Int, day: Int): String {
    val m = if (month < 3) month + 12 else month
    val y = if (month < 3) year - 1 else year
    val k = y % 100
    val j = y / 100
    val h = (day + (13 * (m + 1)) / 5 + k + k / 4 + j / 4 - 2 * j).mod(7)
    return listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")[h]
}
