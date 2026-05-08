package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mettyoung.fitbro.data.cache.UserSettingsDataSource
import com.mettyoung.fitbro.data.model.DailyBalance
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary
import com.mettyoung.fitbro.util.MONTH_ABBR
import com.mettyoung.fitbro.util.minusDays
import com.mettyoung.fitbro.util.plusDays
import com.mettyoung.fitbro.util.toYMD
import com.mettyoung.fitbro.util.todayString
import kotlin.math.roundToInt

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
    val selectedBalance = balances.find { it.date == selectedDate } ?: balances.lastOrNull()

    var proteinGoal by remember { mutableStateOf(userSettingsDataSource.getProteinGoalG()) }
    var carbsGoal by remember { mutableStateOf(userSettingsDataSource.getCarbsGoalG()) }
    var fatGoal by remember { mutableStateOf(userSettingsDataSource.getFatGoalG()) }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Immersive Header Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(top = statusBarPadding, bottom = 24.dp, start = 20.dp, end = 20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Nutrition",
                                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                            )
                            Text(
                                text = "Daily Macro Intake",
                                style = MaterialTheme.typography.labelSmall,
                                color = MiOrange,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Date Navigator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(28.dp))
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            selectedDate = selectedDate.minusDays(1)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev", tint = MiOrange)
                        }

                        Text(
                            text = selectedDate.let { d ->
                                val (y, m, day) = d.toYMD()
                                "${MONTH_ABBR[m]} $day, $y"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = {
                                selectedDate = selectedDate.plusDays(1)
                            },
                            enabled = canGoNext
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next",
                                tint = if (canGoNext) MiOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(Modifier.height(20.dp))

                selectedBalance?.let { balance ->
                    // Summary Insight Card
                    MacroSummaryCard(balance = balance)

                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = "Detailed Breakdown",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        MiMacroCard(
                            label = "Protein",
                            intake = balance.proteinG,
                            goal = proteinGoal,
                            unit = "g",
                            color = Color(0xFF5C6BC0),
                            onEditGoal = { showGoalEditDialog = "protein" }
                        )

                        MiMacroCard(
                            label = "Carbohydrates",
                            intake = balance.carbG,
                            goal = carbsGoal,
                            unit = "g",
                            color = Color(0xFFFFA726),
                            onEditGoal = { showGoalEditDialog = "carbs" }
                        )

                        MiMacroCard(
                            label = "Fats",
                            intake = balance.fatG,
                            goal = fatGoal,
                            unit = "g",
                            color = Color(0xFF66BB6A),
                            onEditGoal = { showGoalEditDialog = "fat" }
                        )
                    }
                } ?: run {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No data for this date", color = MiTextSecondary)
                    }
                }

                Spacer(Modifier.height(32.dp))
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
                    "protein" -> { userSettingsDataSource.setProteinGoalG(newGoal); proteinGoal = newGoal }
                    "carbs" -> { userSettingsDataSource.setCarbsGoalG(newGoal); carbsGoal = newGoal }
                    "fat" -> { userSettingsDataSource.setFatGoalG(newGoal); fatGoal = newGoal }
                }
                showGoalEditDialog = null
            }
        )
    }
}

@Composable
private fun MacroSummaryCard(balance: DailyBalance) {
    val caloriesFromMacros = (balance.proteinG * 4) + (balance.carbG * 4) + (balance.fatG * 9)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Calorie Contribution",
                style = MaterialTheme.typography.labelSmall,
                color = MiTextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${caloriesFromMacros.roundToInt()} kcal",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Total: ${balance.intake.roundToInt()} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MiTextSecondary
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            // Simple stacked bar representing macro ratios
            val totalG = (balance.proteinG + balance.carbG + balance.fatG).coerceAtLeast(1.0)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Box(modifier = Modifier.fillMaxHeight().weight((balance.proteinG / totalG).toFloat()).background(Color(0xFF5C6BC0)))
                Box(modifier = Modifier.fillMaxHeight().weight((balance.carbG / totalG).toFloat()).background(Color(0xFFFFA726)))
                Box(modifier = Modifier.fillMaxHeight().weight((balance.fatG / totalG).toFloat()).background(Color(0xFF66BB6A)))
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MacroLegend(label = "Protein", color = Color(0xFF5C6BC0))
                MacroLegend(label = "Carbs", color = Color(0xFFFFA726))
                MacroLegend(label = "Fat", color = Color(0xFF66BB6A))
            }
        }
    }
}

@Composable
private fun MacroLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MiTextSecondary)
    }
}

@Composable
private fun MiMacroCard(
    label: String,
    intake: Double,
    goal: Double,
    unit: String,
    color: Color,
    onEditGoal: () -> Unit
) {
    val progress = (intake / goal).toFloat().coerceIn(0f, 2f)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = intake.roundToInt().toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = color
                        )
                        Text(
                            text = " / ${goal.roundToInt()} $unit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MiTextSecondary,
                            modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                        )
                    }
                }
                
                IconButton(
                    onClick = onEditGoal,
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.background, CircleShape)
                ) {
                    Text("✎", fontSize = 14.sp, color = MiOrange)
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = { (progress.coerceAtMost(1f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.1f),
                gapSize = 0.dp
            )
            
            Spacer(Modifier.height(8.dp))
            
            val remaining = (goal - intake).roundToInt()
            Text(
                text = if (remaining >= 0) "$remaining $unit remaining" else "${-remaining} $unit over limit",
                style = MaterialTheme.typography.labelSmall,
                color = if (remaining >= 0) MiTextSecondary else Color(0xFFEF5350)
            )
        }
    }
}

@Composable
private fun GoalEditDialog(
    macroType: String,
    currentGoal: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var input by remember { mutableStateOf(currentGoal.toInt().toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Edit ${macroType.replaceFirstChar { it.uppercase() }} Goal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(20.dp))

                TextField(
                    value = input,
                    onValueChange = { if (it.all { char -> char.isDigit() }) input = it },
                    label = { Text("Goal (grams)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background
                    )
                )

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Cancel", color = MiTextSecondary)
                    }
                    Button(
                        onClick = { input.toDoubleOrNull()?.let { onSave(it) } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MiOrange)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
