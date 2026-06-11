package com.mettyoung.fitbro.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.cache.UserSettingsDataSource
import com.mettyoung.fitbro.ui.ColorCarbs
import com.mettyoung.fitbro.ui.ColorFat
import com.mettyoung.fitbro.ui.ColorProtein
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary
import com.mettyoung.fitbro.util.MacroMath
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun MacroGoalsSettings(
    userSettingsDataSource: UserSettingsDataSource,
    modifier: Modifier = Modifier
) {
    var proteinGoal by remember { mutableStateOf("") }
    var carbsGoal by remember { mutableStateOf("") }
    var fatGoal by remember { mutableStateOf("") }
    var calorieGoal by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        proteinGoal = userSettingsDataSource.getProteinGoalG().toInt().toString()
        carbsGoal = userSettingsDataSource.getCarbsGoalG().toInt().toString()
        fatGoal = userSettingsDataSource.getFatGoalG().toInt().toString()
        calorieGoal = userSettingsDataSource.getCalorieGoalKcal().toInt().toString()
    }

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(2000)
            showSuccess = false
        }
    }

    // Atwater: 4 kcal/g carb, 4 kcal/g protein, 9 kcal/g fat. Recompute on any macro change,
    // overwriting the calorie goal. A manual calorie edit persists until the next macro change.
    fun recalcCalories(p: String, c: String, f: String) {
        val sum = MacroMath.caloriesFromMacros(
            carbG = (c.toIntOrNull() ?: 0).toDouble(),
            proteinG = (p.toIntOrNull() ?: 0).toDouble(),
            fatG = (f.toIntOrNull() ?: 0).toDouble()
        )
        calorieGoal = if (sum > 0) sum.roundToInt().toString() else ""
    }

    val percents = MacroMath.macroPercents(
        carbG = (carbsGoal.toIntOrNull() ?: 0).toDouble(),
        proteinG = (proteinGoal.toIntOrNull() ?: 0).toDouble(),
        fatG = (fatGoal.toIntOrNull() ?: 0).toDouble()
    )

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Premium Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(top = statusBarPadding + 16.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
            ) {
                Column {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Customize your metabolic targets",
                        style = MaterialTheme.typography.labelSmall,
                        color = MiOrange
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Daily Goals",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        SettingsInputField(
                            label = "Daily Calorie Target",
                            value = calorieGoal,
                            onValueChange = { calorieGoal = it },
                            unit = "kcal",
                            color = MiOrange
                        )

                        SettingsInputField(
                            label = "Protein Goal",
                            value = proteinGoal,
                            onValueChange = { proteinGoal = it; recalcCalories(it, carbsGoal, fatGoal) },
                            unit = "g",
                            color = ColorProtein,
                            percent = percents?.let { "${it.proteinPct}%" }
                        )

                        SettingsInputField(
                            label = "Carbohydrates Goal",
                            value = carbsGoal,
                            onValueChange = { carbsGoal = it; recalcCalories(proteinGoal, it, fatGoal) },
                            unit = "g",
                            color = ColorCarbs,
                            percent = percents?.let { "${it.carbPct}%" }
                        )

                        SettingsInputField(
                            label = "Total Fats Goal",
                            value = fatGoal,
                            onValueChange = { fatGoal = it; recalcCalories(proteinGoal, carbsGoal, it) },
                            unit = "g",
                            color = ColorFat,
                            percent = percents?.let { "${it.fatPct}%" }
                        )
                    }
                }

                Spacer(Modifier.height(40.dp))

                Button(
                    onClick = {
                        isSaving = true
                        val p = proteinGoal.toDoubleOrNull() ?: 0.0
                        val c = carbsGoal.toDoubleOrNull() ?: 0.0
                        val f = fatGoal.toDoubleOrNull() ?: 0.0
                        val cal = calorieGoal.toDoubleOrNull() ?: 0.0

                        if (p > 0) userSettingsDataSource.setProteinGoalG(p)
                        if (c > 0) userSettingsDataSource.setCarbsGoalG(c)
                        if (f > 0) userSettingsDataSource.setFatGoalG(f)
                        if (cal > 0) userSettingsDataSource.setCalorieGoalKcal(cal)
                        
                        isSaving = false
                        showSuccess = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MiOrange),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else if (showSuccess) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Updated Successfully", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        }
                    } else {
                        Text("Apply New Goals", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                }

                Spacer(Modifier.height(48.dp))
            }
        }
    }
}

@Composable
private fun SettingsInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    unit: String,
    color: Color,
    percent: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MiTextSecondary
        )
        TextField(
            value = value,
            onValueChange = { if (it.all { char -> char.isDigit() }) onValueChange(it) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    if (percent != null) {
                        Text(
                            text = percent,
                            style = MaterialTheme.typography.labelSmall,
                            color = MiTextSecondary
                        )
                    }
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = color
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = MaterialTheme.colorScheme.background,
                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                cursorColor = color,
                focusedLabelColor = color
            )
        )
    }
}
