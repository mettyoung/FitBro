package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.model.DailyMacroTotals
import com.mettyoung.fitbro.ui.ColorCarbs
import com.mettyoung.fitbro.ui.ColorFat
import com.mettyoung.fitbro.ui.ColorProtein
import com.mettyoung.fitbro.ui.NegativeColor
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Today's energy + macro progress: a calorie ring (remaining / over in the centre) beside
 * three macro progress bars.
 */
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
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CalorieRing(consumed = totals.calories, goal = calorieGoal)
            Spacer(Modifier.width(20.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MacroBar("Protein", totals.proteinG, proteinGoal, ColorProtein)
                MacroBar("Carbs", totals.carbG, carbGoal, ColorCarbs)
                MacroBar("Fat", totals.fatG, fatGoal, ColorFat)
            }
        }
    }
}

@Composable
private fun CalorieRing(consumed: Double, goal: Double) {
    val hasGoal = goal > 0
    val remaining = goal - consumed
    val over = hasGoal && remaining < 0
    val target = if (hasGoal) (consumed / goal).toFloat().coerceIn(0f, 1f) else 0f
    val progress by animateFloatAsState(target)

    val track = MaterialTheme.colorScheme.surfaceVariant
    val arc = if (over) NegativeColor else MaterialTheme.colorScheme.primary

    Box(modifier = Modifier.size(128.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(128.dp)) {
            val stroke = 12.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(track, 135f, 270f, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            if (progress > 0f) {
                drawArc(arc, 135f, 270f * progress, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (hasGoal) "${abs(remaining).roundToInt()}" else "${consumed.roundToInt()}",
                style = MaterialTheme.typography.headlineMedium,
                color = if (over) NegativeColor else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = when {
                    !hasGoal -> "kcal eaten"
                    over -> "kcal over"
                    else -> "kcal left"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hasGoal) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${consumed.roundToInt()} / ${goal.roundToInt()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MacroBar(label: String, consumed: Double, goal: Double, color: Color) {
    val target = if (goal > 0) (consumed / goal).toFloat().coerceIn(0f, 1f) else 0f
    val progress by animateFloatAsState(target)
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${consumed.roundToInt()}",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (goal > 0) {
                Text(
                    text = " / ${goal.roundToInt()} g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(" g", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
    }
}
