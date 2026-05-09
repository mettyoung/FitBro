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
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroCircleChart(
                    label = "Carbs",
                    consumed = totals.carbG,
                    goal = carbGoal.coerceAtLeast(1.0),
                    color = MaterialTheme.colorScheme.primary
                )
                MacroCircleChart(
                    label = "Protein",
                    consumed = totals.proteinG,
                    goal = proteinGoal.coerceAtLeast(1.0),
                    color = MaterialTheme.colorScheme.error
                )
                MacroCircleChart(
                    label = "Fat",
                    consumed = totals.fatG,
                    goal = fatGoal.coerceAtLeast(1.0),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(Modifier.height(20.dp))

            val calorieProgress = (totals.calories / calorieGoal.coerceAtLeast(1.0))
                .coerceIn(0.0, 2.0).toFloat()
            val progressColor = when {
                calorieProgress < 0.85f -> Color(0xFF4CAF50)
                calorieProgress <= 1.0f -> Color(0xFFFFC107)
                else -> MaterialTheme.colorScheme.error
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${totals.calories.roundToInt()} kcal",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "/ ${calorieGoal.roundToInt()} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { calorieProgress.coerceAtMost(1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.15f),
                gapSize = 0.dp
            )
        }
    }
}

@Composable
private fun MacroCircleChart(
    label: String,
    consumed: Double,
    goal: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(96.dp)) {
            val strokeWidth = 9.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            val sweep = (consumed / goal).coerceIn(0.0, 1.0).toFloat() * 270f
            if (sweep > 0f) {
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = consumed.roundToInt().toString(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = color
            )
            Text(
                text = "/ ${goal.roundToInt()}g",
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariant
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceVariant
            )
        }
    }
}
