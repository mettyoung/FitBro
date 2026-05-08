package com.mettyoung.fitbro.ui.dashboard

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mettyoung.fitbro.data.cache.UserSettingsDataSource
import com.mettyoung.fitbro.data.model.DailyBalance
import com.mettyoung.fitbro.util.MONTH_ABBR
import com.mettyoung.fitbro.util.minusDays
import com.mettyoung.fitbro.util.plusDays
import com.mettyoung.fitbro.util.toYMD
import com.mettyoung.fitbro.util.todayString

@Composable
fun MacroDailyCounterDetail(
    balances: List<DailyBalance>,
    userSettingsDataSource: UserSettingsDataSource,
    modifier: Modifier = Modifier
) {
    var selectedDate by remember { mutableStateOf(todayString()) }
    var showGoalEditDialog by remember { mutableStateOf<String?>(null) }

    val today = todayString()
    val canGoNext = selectedDate.plusDays(1) <= today
    val selectedBalance = balances.lastOrNull()

    val proteinGoal = remember { userSettingsDataSource.getProteinGoalG() }
    val carbsGoal = remember { userSettingsDataSource.getCarbsGoalG() }
    val fatGoal = remember { userSettingsDataSource.getFatGoalG() }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Premium header with elevation
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text("‹", fontSize = 24.sp, fontWeight = FontWeight.Light)
                    }
                    Spacer(Modifier.weight(1f))
                    Column {
                        Text(
                            text = "Macros",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Protein, Carbs & Fat",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text("📅", fontSize = 20.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    selectedDate = selectedDate.minusDays(1)
                }) { Text("‹ Prev") }

                Text(
                    text = selectedDate.let { d ->
                        val (y, m, day) = d.toYMD()
                        "${MONTH_ABBR[m]} $day, $y"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = {
                        selectedDate = selectedDate.plusDays(1)
                    },
                    enabled = canGoNext
                ) { Text("Next ›") }
            }

            Spacer(Modifier.height(24.dp))

            selectedBalance?.let { balance ->
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    MacroCard(
                        label = "Protein",
                        intake = balance.proteinG,
                        goal = proteinGoal,
                        unit = "g",
                        color = Color(0xFF1976D2),
                        onEditGoal = { showGoalEditDialog = "protein" }
                    )

                    MacroCard(
                        label = "Carbs",
                        intake = balance.carbG,
                        goal = carbsGoal,
                        unit = "g",
                        color = Color(0xFF43A047),
                        onEditGoal = { showGoalEditDialog = "carbs" }
                    )

                    MacroCard(
                        label = "Fat",
                        intake = balance.fatG,
                        goal = fatGoal,
                        unit = "g",
                        color = Color(0xFFFFA726),
                        onEditGoal = { showGoalEditDialog = "fat" }
                    )

                    Spacer(Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Total Calories from Macros",
                                fontSize = 12.sp,
                                color = Color(0xFF999999)
                            )
                            val caloriesFromMacros = (balance.proteinG * 4) + (balance.carbG * 4) + (balance.fatG * 9)
                            Text(
                                text = "${caloriesFromMacros.toInt()} kcal",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }

    if (showGoalEditDialog != null) {
        GoalEditDialog(
            macroType = showGoalEditDialog!!,
            currentGoal = when (showGoalEditDialog) {
                "protein" -> proteinGoal
                "carbs" -> carbsGoal
                "fat" -> fatGoal
                else -> 0.0
            },
            onDismiss = { showGoalEditDialog = null },
            onSave = { newGoal ->
                when (showGoalEditDialog) {
                    "protein" -> userSettingsDataSource.setProteinGoalG(newGoal)
                    "carbs" -> userSettingsDataSource.setCarbsGoalG(newGoal)
                    "fat" -> userSettingsDataSource.setFatGoalG(newGoal)
                }
                showGoalEditDialog = null
            }
        )
    }
}

@Composable
private fun MacroCard(
    label: String,
    intake: Double,
    goal: Double,
    unit: String,
    color: Color,
    onEditGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val intakeInt = intake.toInt()
    val goalInt = goal.toInt()
    val remaining = (goal - intake).toInt()
    val progress = (intake / goal).toFloat().coerceIn(0f, 2f)

    val progressColor = when {
        progress <= 1f -> Color(0xFF43A047)
        progress <= 1.2f -> Color(0xFFFFA726)
        else -> Color(0xFFEF5350)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = Color(0xFF999999)
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "$intakeInt",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "/ $goalInt $unit",
                            fontSize = 14.sp,
                            color = Color(0xFF999999)
                        )
                    }
                }
                TextButton(onClick = onEditGoal) {
                    Text("Edit Goal", fontSize = 12.sp)
                }
            }

            LinearProgressIndicator(
                progress = { (progress.coerceAtMost(1f)).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = progressColor,
                trackColor = Color(0xFFE0E0E0),
                gapSize = 0.dp
            )

            Text(
                text = if (remaining > 0) "Remaining: $remaining $unit" else "Over by ${-remaining} $unit",
                fontSize = 11.sp,
                color = if (remaining > 0) Color(0xFF999999) else Color(0xFFEF5350)
            )
        }
    }
}

@Composable
private fun GoalEditDialog(
    macroType: String,
    currentGoal: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf(currentGoal.toInt().toString()) }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .padding(24.dp)
                .width(280.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Edit $macroType Goal",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                TextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("$macroType (g)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            input.toDoubleOrNull()?.let { onSave(it) }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
