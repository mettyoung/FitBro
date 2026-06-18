package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mettyoung.fitbro.data.model.DailyMacroTotals
import com.mettyoung.fitbro.ui.ColorCarbs
import com.mettyoung.fitbro.ui.ColorFat
import com.mettyoung.fitbro.ui.ColorProtein
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary
import com.mettyoung.fitbro.util.dayOfWeekMonBased
import com.mettyoung.fitbro.util.toYMD

private val DAY_ABBR = arrayOf("M", "T", "W", "T", "F", "S", "S")

@Composable
fun WeeklyTrendsCard(
    weeklyTotals: List<DailyMacroTotals>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Weekly Activity",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Trends across last 7 days",
                        style = MaterialTheme.typography.labelSmall,
                        color = MiTextSecondary
                    )
                }
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.background, CircleShape)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (expanded && weeklyTotals.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))

                val maxCal = weeklyTotals.maxOfOrNull { it.calories }?.coerceAtLeast(1.0) ?: 1.0
                val barColors = listOf(MiOrange, ColorProtein, ColorCarbs, ColorFat)

                Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val groupWidth = size.width / 7f
                        val barSpacing = 2.dp.toPx()
                        val barWidth = (groupWidth - 8.dp.toPx()) / 4f

                        weeklyTotals.forEachIndexed { gi, day ->
                            val values = listOf(day.calories, day.proteinG * 4, day.carbG * 4, day.fatG * 9)
                            
                            values.forEachIndexed { bi, value ->
                                val norm = (value / maxCal).toFloat().coerceIn(0.01f, 1f)
                                val left = gi * groupWidth + (groupWidth - (barWidth * 4 + barSpacing * 3)) / 2f + bi * (barWidth + barSpacing)
                                val barH = norm * size.height
                                
                                drawRoundRect(
                                    color = barColors[bi],
                                    topLeft = Offset(left, size.height - barH),
                                    size = Size(barWidth, barH),
                                    cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    weeklyTotals.forEach { day ->
                        val (y, m, d) = day.date.toYMD()
                        val dow = dayOfWeekMonBased(y, m, d)
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                text = DAY_ABBR[dow],
                                style = MaterialTheme.typography.labelSmall,
                                color = MiTextSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    LegendItem(MiOrange, "Kcal")
                    LegendItem(ColorProtein, "Prot")
                    LegendItem(ColorCarbs, "Carb")
                    LegendItem(ColorFat, "Fat")
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MiTextSecondary
        )
    }
}
