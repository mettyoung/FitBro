package com.mettyoung.fitbro.ui.dashboard

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mettyoung.fitbro.data.cache.UserSettingsDataSource
import com.mettyoung.fitbro.data.food.OpenFoodFactsDataSource
import com.mettyoung.fitbro.data.model.FoodDiaryEntry
import com.mettyoung.fitbro.data.model.MealType
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary
import com.mettyoung.fitbro.util.MONTH_ABBR
import com.mettyoung.fitbro.util.minusDays
import com.mettyoung.fitbro.util.plusDays
import com.mettyoung.fitbro.util.toYMD
import com.mettyoung.fitbro.util.todayString
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MacroDailyCounterDetail(
    userSettingsDataSource: UserSettingsDataSource,
    foodDiaryStateHolder: FoodDiaryStateHolder,
    openFoodFactsDataSource: OpenFoodFactsDataSource,
    onDateSelected: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val foodState by foodDiaryStateHolder.state.collectAsState()
    val selectedDate by foodDiaryStateHolder.selectedDate.collectAsState()
    val weeklyTotals by foodDiaryStateHolder.weeklyTotals.collectAsState()

    val today = todayString()
    val canGoNext = selectedDate.plusDays(1) <= today

    var proteinGoal by remember { mutableStateOf(userSettingsDataSource.getProteinGoalG()) }
    var carbsGoal by remember { mutableStateOf(userSettingsDataSource.getCarbsGoalG()) }
    var fatGoal by remember { mutableStateOf(userSettingsDataSource.getFatGoalG()) }
    var calorieGoal by remember { mutableStateOf(userSettingsDataSource.getCalorieGoalKcal()) }

    var showGoalsDialog by remember { mutableStateOf(false) }
    var addingToMeal by remember { mutableStateOf<String?>(null) }
    var editingEntry by remember { mutableStateOf<FoodDiaryEntry?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<FoodDiaryEntry?>(null) }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(
                            top = statusBarPadding + 16.dp,
                            start = 24.dp,
                            end = 24.dp,
                            bottom = 24.dp
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Nutrition",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Daily Nutrient Breakdown",
                                style = MaterialTheme.typography.labelSmall,
                                color = MiOrange
                            )
                        }
                        IconButton(
                            onClick = { showGoalsDialog = true },
                            modifier = Modifier.background(MaterialTheme.colorScheme.background, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Edit Goals",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            onDateSelected(selectedDate.minusDays(1))
                        }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev")
                        }
                        Text(
                            text = selectedDate.let { d ->
                                val (y, m, day) = d.toYMD()
                                "${MONTH_ABBR[m]} $day, $y"
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        IconButton(
                            onClick = { onDateSelected(selectedDate.plusDays(1)) },
                            enabled = canGoNext
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next",
                                tint = if (canGoNext) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    MacroSummaryHeader(
                        totals = foodState.dailyTotals,
                        proteinGoal = proteinGoal,
                        carbGoal = carbsGoal,
                        fatGoal = fatGoal,
                        calorieGoal = calorieGoal
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }

            MealType.ordered.forEach { mealType ->
                item(key = mealType) {
                    val entries = (foodState.entriesByMeal[mealType] ?: emptyList())
                        .filter { it.id != pendingDelete?.id }
                    FoodDiarySection(
                        mealType = mealType,
                        entries = entries,
                        onAddClick = { addingToMeal = mealType },
                        onEditClick = { editingEntry = it },
                        onDeleteClick = { entry ->
                            pendingDelete?.let { prev -> foodDiaryStateHolder.deleteEntry(prev.id) }
                            pendingDelete = entry
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Removed ${entry.foodName}",
                                    actionLabel = "Undo",
                                    duration = SnackbarDuration.Short
                                )
                                when (result) {
                                    SnackbarResult.ActionPerformed -> pendingDelete = null
                                    SnackbarResult.Dismissed -> {
                                        foodDiaryStateHolder.deleteEntry(entry.id)
                                        pendingDelete = null
                                    }
                                }
                            }
                        },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            if (weeklyTotals.isNotEmpty()) {
                item {
                    WeeklyTrendsCard(
                        weeklyTotals = weeklyTotals,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            item { Spacer(Modifier.height(48.dp)) }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }

    addingToMeal?.let { mealType ->
        FoodSearchSheet(
            mealType = mealType,
            date = selectedDate,
            openFoodFactsDataSource = openFoodFactsDataSource,
            onDismiss = { addingToMeal = null },
            onAddEntry = { entry ->
                foodDiaryStateHolder.addEntry(entry)
                addingToMeal = null
            }
        )
    }

    editingEntry?.let { entry ->
        EditEntrySheet(
            entry = entry,
            onDismiss = { editingEntry = null },
            onSave = { updated ->
                foodDiaryStateHolder.updateEntry(updated)
                editingEntry = null
            }
        )
    }

    if (showGoalsDialog) {
        MacroGoalsDialog(
            proteinGoal = proteinGoal,
            carbsGoal = carbsGoal,
            fatGoal = fatGoal,
            calorieGoal = calorieGoal,
            onDismiss = { showGoalsDialog = false },
            onSave = { p, c, f, cal ->
                userSettingsDataSource.setProteinGoalG(p); proteinGoal = p
                userSettingsDataSource.setCarbsGoalG(c); carbsGoal = c
                userSettingsDataSource.setFatGoalG(f); fatGoal = f
                userSettingsDataSource.setCalorieGoalKcal(cal); calorieGoal = cal
                showGoalsDialog = false
            }
        )
    }
}

@Composable
private fun FoodDiarySection(
    mealType: String,
    entries: List<FoodDiaryEntry>,
    onAddClick: () -> Unit,
    onEditClick: (FoodDiaryEntry) -> Unit,
    onDeleteClick: (FoodDiaryEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val mealCalories = entries.sumOf { it.calories }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val title = mealType.lowercase().replaceFirstChar { it.uppercase() }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (mealCalories > 0) {
                        Text(
                            text = "${mealCalories.roundToInt()} kcal total",
                            style = MaterialTheme.typography.labelSmall,
                            color = MiOrange
                        )
                    }
                }
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(MiOrange.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add food",
                        tint = MiOrange,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                entries.forEachIndexed { index, entry ->
                    FoodEntryRow(
                        entry = entry,
                        onEdit = { onEditClick(entry) },
                        onDelete = { onDeleteClick(entry) }
                    )
                    if (index < entries.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "No items logged for $mealType",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiTextSecondary.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun FoodEntryRow(
    entry: FoodDiaryEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.foodName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    entry.brandName?.let { append(it); append(" · ") }
                    append("${entry.servingSizeG.roundToInt()}g")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MiTextSecondary
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroTag("P ${entry.proteinG.roundToInt()}g")
                MacroTag("C ${entry.carbG.roundToInt()}g")
                MacroTag("F ${entry.fatG.roundToInt()}g")
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${entry.calories.roundToInt()}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = MiOrange
            )
            Text(
                text = "kcal",
                style = MaterialTheme.typography.labelSmall,
                color = MiTextSecondary
            )
            Row(modifier = Modifier.padding(top = 4.dp)) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MiTextSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun MacroTag(text: String) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MacroGoalsDialog(
    proteinGoal: Double,
    carbsGoal: Double,
    fatGoal: Double,
    calorieGoal: Double,
    onDismiss: () -> Unit,
    onSave: (protein: Double, carbs: Double, fat: Double, calorie: Double) -> Unit
) {
    var proteinInput by remember { mutableStateOf(proteinGoal.toInt().toString()) }
    var carbsInput by remember { mutableStateOf(carbsGoal.toInt().toString()) }
    var fatInput by remember { mutableStateOf(fatGoal.toInt().toString()) }
    var calorieInput by remember { mutableStateOf(calorieGoal.toInt().toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(32.dp)) {
                Text(
                    text = "Target Goals",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Adjust your daily targets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MiTextSecondary
                )
                
                Spacer(Modifier.height(24.dp))
                
                GoalField("Calories (kcal)", calorieInput) {
                    if (it.all { c -> c.isDigit() }) calorieInput = it
                }
                Spacer(Modifier.height(16.dp))
                GoalField("Protein (g)", proteinInput) {
                    if (it.all { c -> c.isDigit() }) proteinInput = it
                }
                Spacer(Modifier.height(16.dp))
                GoalField("Carbohydrates (g)", carbsInput) {
                    if (it.all { c -> c.isDigit() }) carbsInput = it
                }
                Spacer(Modifier.height(16.dp))
                GoalField("Total Fats (g)", fatInput) {
                    if (it.all { c -> c.isDigit() }) fatInput = it
                }
                
                Spacer(Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Discard", color = MiTextSecondary)
                    }
                    Button(
                        onClick = {
                            val p = proteinInput.toDoubleOrNull() ?: return@Button
                            val c = carbsInput.toDoubleOrNull() ?: return@Button
                            val f = fatInput.toDoubleOrNull() ?: return@Button
                            val cal = calorieInput.toDoubleOrNull() ?: return@Button
                            onSave(p, c, f, cal)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MiOrange)
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalField(label: String, value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.background,
            unfocusedContainerColor = MaterialTheme.colorScheme.background,
            focusedLabelColor = MiOrange
        )
    )
}
