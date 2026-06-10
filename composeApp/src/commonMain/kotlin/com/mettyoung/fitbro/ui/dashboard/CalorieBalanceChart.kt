package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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

@Composable
fun CalorieBalanceChart(
    balances: List<DailyBalance>,
    onBarClick: (DailyBalance) -> Unit = {},
    modifier: Modifier = Modifier,
    valueSelector: (DailyBalance) -> Double = { it.balance },
    diverging: Boolean = true
) {
    if (balances.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No data available", style = MaterialTheme.typography.bodyMedium, color = MiTextSecondary)
        }
        return
    }

    // Diverging (balance) keeps the original 100..3000 clamp; positive-only modes
    // normalize against the actual window max of the plotted value.
    val maxValue = if (diverging) {
        balances.maxOf { abs(valueSelector(it)) }.coerceAtLeast(100.0).coerceAtMost(3000.0)
    } else {
        balances.maxOf { abs(valueSelector(it)) }.coerceAtLeast(1.0)
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
            val displayBalances = if (balances.size > 7) balances.takeLast(7) else balances
            
            displayBalances.forEach { balance ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CalorieBarItem(
                        balance = balance,
                        value = valueSelector(balance),
                        maxValue = maxValue,
                        diverging = diverging,
                        onClick = { onBarClick(balance) }
                    )
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

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top half (positive)
        Box(
            modifier = Modifier.weight(1f).width(12.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (drawTop) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(heightFactor)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(color)
                )
            }
        }

        Spacer(Modifier.height(4.dp)) // Space for the zero line area

        // Bottom half (negative)
        Box(
            modifier = Modifier.weight(1f).width(12.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (drawBottom) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(heightFactor)
                        .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
                        .background(color)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = balance.date.toDayInitial(),
            style = MaterialTheme.typography.labelSmall,
            color = MiTextSecondary,
            fontWeight = FontWeight.Bold
        )
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
