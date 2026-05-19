package com.mettyoung.fitbro.ui.dashboard

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mettyoung.fitbro.data.model.DailyMacroTotals
import com.mettyoung.fitbro.ui.ColorCarbs
import com.mettyoung.fitbro.ui.ColorFat
import com.mettyoung.fitbro.ui.ColorProtein
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary
import kotlin.math.roundToInt

@Composable
fun MacroSummaryHeader(
    totals: DailyMacroTotals,
    proteinGoal: Double,
    carbGoal: Double,
    fatGoal: Double,
    calorieGoal: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Energy Intake",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${totals.calories.roundToInt()}",
                            style = MaterialTheme.typography.displayMedium.copy(fontSize = 36.sp),
                            color = MiOrange
                        )
                        Text(
                            text = " / ${calorieGoal.roundToInt()} kcal",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MiTextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val calorieProgress = (totals.calories / calorieGoal.coerceAtLeast(1.0))
                .coerceIn(0.0, 2.0).toFloat()
            
            LinearProgressIndicator(
                progress = { calorieProgress.coerceAtMost(1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape),
                color = MiOrange,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                gapSize = 0.dp
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroMiniCard(
                    label = "Protein",
                    consumed = totals.proteinG,
                    goal = proteinGoal,
                    color = ColorProtein,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                MacroMiniCard(
                    label = "Carbs",
                    consumed = totals.carbG,
                    goal = carbGoal,
                    color = ColorCarbs,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                MacroMiniCard(
                    label = "Fat",
                    consumed = totals.fatG,
                    goal = fatGoal,
                    color = ColorFat,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MacroMiniCard(
    label: String,
    consumed: Double,
    goal: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = consumed / goal.coerceAtLeast(1.0)
    val arcProgress = progress.coerceIn(0.0, 1.0).toFloat()
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(modifier = Modifier.size(48.dp)) {
                val strokeWidth = 5.dp.toPx()
                drawCircle(
                    color = color.copy(alpha = 0.1f),
                    style = Stroke(width = strokeWidth)
                )
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * arcProgress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            Text(
                text = "${(progress * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        
        Spacer(Modifier.height(8.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MiTextSecondary
        )
        Text(
            text = "${consumed.roundToInt()}g",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
