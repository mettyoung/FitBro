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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mettyoung.fitbro.data.cache.UserSettingsDataSource
import com.mettyoung.fitbro.data.model.DailyBalance

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
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF4ECDC4), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "📊",
                        fontSize = 28.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Macros",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Track your nutritional breakdown",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xFFE0F7F6), RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "→",
                        fontSize = 16.sp,
                        color = Color(0xFF4ECDC4),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (balance != null) {
                MacroProgressBar(
                    label = "Protein",
                    intake = balance.proteinG,
                    goal = proteinGoal,
                    color = Color(0xFF1976D2)
                )

                MacroProgressBar(
                    label = "Carbs",
                    intake = balance.carbG,
                    goal = carbsGoal,
                    color = Color(0xFF43A047)
                )

                MacroProgressBar(
                    label = "Fat",
                    intake = balance.fatG,
                    goal = fatGoal,
                    color = Color(0xFFFFA726)
                )
            }
        }
    }
}

@Composable
private fun MacroProgressBar(
    label: String,
    intake: Double,
    goal: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = (intake / goal).toFloat().coerceIn(0f, 1.5f)
    val displayProgress = progress.coerceAtMost(1f)

    val progressColor = when {
        progress <= 1f -> color
        progress <= 1.2f -> Color(0xFFFFA726)
        else -> Color(0xFFEF5350)
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF999999),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${intake.toInt()}/${goal.toInt()}g",
                fontSize = 11.sp,
                color = Color(0xFF999999)
            )
        }

        LinearProgressIndicator(
            progress = { displayProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = progressColor,
            trackColor = Color(0xFFE0E0E0),
            gapSize = 0.dp
        )
    }
}
