package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mettyoung.fitbro.data.model.DailyMacroTotals
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.util.dayOfWeekMonBased
import com.mettyoung.fitbro.util.toYMD

private val DAY_ABBR = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun WeeklyTrendsCard(
    weeklyTotals: List<DailyMacroTotals>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }
    val proteinColor = MaterialTheme.colorScheme.error
    val carbColor = MaterialTheme.colorScheme.primary
    val fatColor = MaterialTheme.colorScheme.tertiary

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Trends",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            if (expanded && weeklyTotals.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendDot(MiOrange, "Cal")
                    LegendDot(proteinColor, "Protein")
                    LegendDot(carbColor, "Carbs")
                    LegendDot(fatColor, "Fat")
                }

                Spacer(Modifier.height(12.dp))

                val maxCal = weeklyTotals.maxOfOrNull { it.calories }?.coerceAtLeast(1.0) ?: 1.0
                val maxProt = weeklyTotals.maxOfOrNull { it.proteinG }?.coerceAtLeast(1.0) ?: 1.0
                val maxCarb = weeklyTotals.maxOfOrNull { it.carbG }?.coerceAtLeast(1.0) ?: 1.0
                val maxFat = weeklyTotals.maxOfOrNull { it.fatG }?.coerceAtLeast(1.0) ?: 1.0
                val barColors = listOf(MiOrange, proteinColor, carbColor, fatColor)

                Canvas(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                    val groupWidth = size.width / 7f
                    val barPad = 1.5.dp.toPx()
                    val barWidth = (groupWidth - 5f * barPad) / 4f

                    weeklyTotals.forEachIndexed { gi, day ->
                        val norms = listOf(
                            (day.calories / maxCal).toFloat(),
                            (day.proteinG / maxProt).toFloat(),
                            (day.carbG / maxCarb).toFloat(),
                            (day.fatG / maxFat).toFloat()
                        )
                        norms.forEachIndexed { bi, norm ->
                            val left = gi * groupWidth + barPad + bi * (barWidth + barPad)
                            val barH = (norm * size.height).coerceAtLeast(if (norm > 0f) 2f else 0f)
                            if (barH > 0f) {
                                drawRect(
                                    color = barColors[bi],
                                    topLeft = Offset(left, size.height - barH),
                                    size = Size(barWidth, barH)
                                )
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    weeklyTotals.forEach { day ->
                        val (y, m, d) = day.date.toYMD()
                        val dow = dayOfWeekMonBased(y, m, d)
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                text = DAY_ABBR[dow],
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = color, radius = size.minDimension / 2f)
        }
        Spacer(Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}
