package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.mettyoung.fitbro.data.cache.UserSettingsDataSource
import com.mettyoung.fitbro.data.model.DailyBalance
import com.mettyoung.fitbro.ui.ColorCarbs
import com.mettyoung.fitbro.ui.ColorFat
import com.mettyoung.fitbro.ui.ColorProtein
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary
import kotlin.math.roundToInt

@Composable
fun MacroSummaryCard(
    balance: DailyBalance?,
    userSettingsDataSource: UserSettingsDataSource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val proteinGoal = userSettingsDataSource.getProteinGoalG()
    val carbsGoal = userSettingsDataSource.getCarbsGoalG()
    val fatGoal = userSettingsDataSource.getFatGoalG()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MiOrange.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🍏", fontSize = 20.sp)
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Nutrition",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Daily Macro Breakdown",
                        style = MaterialTheme.typography.labelSmall,
                        color = MiTextSecondary
                    )
                }
                
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MiTextSecondary.copy(alpha = 0.4f)
                )
            }

            if (balance != null) {
                Spacer(Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MiniMacroStatus(
                        label = "Protein",
                        value = balance.proteinG,
                        goal = proteinGoal,
                        color = ColorProtein,
                        modifier = Modifier.weight(1f)
                    )
                    MiniMacroStatus(
                        label = "Carbs",
                        value = balance.carbG,
                        goal = carbsGoal,
                        color = ColorCarbs,
                        modifier = Modifier.weight(1f)
                    )
                    MiniMacroStatus(
                        label = "Fat",
                        value = balance.fatG,
                        goal = fatGoal,
                        color = ColorFat,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "No nutrition data logged for today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MiTextSecondary
                )
            }
        }
    }
}

@Composable
private fun MiniMacroStatus(
    label: String,
    value: Double,
    goal: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = (value / goal.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MiTextSecondary
        )
        Text(
            text = "${value.roundToInt()}g",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(color.copy(alpha = 0.1f), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .background(color, CircleShape)
            )
        }
    }
}
