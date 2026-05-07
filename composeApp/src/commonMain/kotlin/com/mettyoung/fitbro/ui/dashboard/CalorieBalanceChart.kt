package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.model.DailyBalance
import kotlin.math.abs

@Composable
fun CalorieBalanceChart(
    balances: List<DailyBalance>,
    onBarClick: (DailyBalance) -> Unit = {},
    onListStateCreated: (LazyListState) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (balances.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No data available", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val listState = rememberLazyListState()
    remember { onListStateCreated(listState) }

    val positiveColor = Color(0xFF4CAF50)
    val negativeColor = Color(0xFFF44336)
    val maxAbsBalance = balances.maxOf { abs(it.balance) }.coerceAtLeast(1.0)

    LazyColumn(
        modifier = modifier,
        state = listState
    ) {
        items(balances, key = { it.date }) { balance ->
            CalorieBalanceRow(
                balance = balance,
                maxAbsBalance = maxAbsBalance,
                positiveColor = positiveColor,
                negativeColor = negativeColor,
                onBarClick = { onBarClick(balance) }
            )
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
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

private fun formatCalories(value: Double): String {
    val v = value.toInt()
    return if (v >= 1000) "${v / 1000}.${(v % 1000) / 100}k" else v.toString()
}

@Composable
private fun CalorieBalanceRow(
    balance: DailyBalance,
    maxAbsBalance: Double,
    positiveColor: Color,
    negativeColor: Color,
    onBarClick: () -> Unit
) {
    val color = if (balance.balance >= 0) positiveColor else negativeColor
    val normalizedWidth = (abs(balance.balance) / maxAbsBalance).toFloat() * 0.8f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures { onBarClick() }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = balance.date.toDayLabel(),
                modifier = Modifier.width(50.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(color.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(normalizedWidth)
                        .height(40.dp)
                        .background(color)
                )
            }

            Text(
                text = formatCalories(balance.balance),
                modifier = Modifier
                    .width(60.dp)
                    .padding(start = 8.dp),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.End,
                color = color
            )
        }
    }
}
