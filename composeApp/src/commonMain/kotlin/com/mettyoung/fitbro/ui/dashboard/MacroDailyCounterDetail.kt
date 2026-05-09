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
import androidx.compose.foundation.lazy.items
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
                            top = statusBarPadding + 8.dp,
                            start = 20.dp,
                            end = 20.dp,
                            bottom = 20.dp
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
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            Text(
                                text = "Daily Macro Intake",
                                style = MaterialTheme.typography.labelSmall,
                                color = MiOrange,
                                letterSpacing = 0.5.sp
                            )
                        }
                        IconButton(onClick = { showGoalsDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Edit Goals",
                                tint = MiTextSecondary
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.background,
                                RoundedCornerShape(28.dp)
                            )
                            .padding(vertical = 4.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            onDateSelected(selectedDate.minusDays(1))
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Prev",
                                tint = MiOrange
                            )
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
                            onClick = { onDateSelected(selectedDate.plusDays(1)) },
                            enabled = canGoNext
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next",
                                tint = if (canGoNext) MiOrange
                                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    MacroSummaryHeader(
                        totals = foodState.dailyTotals,
                        proteinGoal = proteinGoal,
                        carbGoal = carbsGoal,
                        fatGoal = fatGoal,
                        calorieGoal = calorieGoal
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }

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
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            if (weeklyTotals.isNotEmpty()) {
                item {
                    WeeklyTrendsCard(
                        weeklyTotals = weeklyTotals,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = mealType.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (mealCalories > 0) {
                        Text(
                            text = "${mealCalories.roundToInt()} kcal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MiTextSecondary
                        )
                    }
                }
                IconButton(
                    onClick = onAddClick,
                    modifier = Modifier
                        .size(36.dp)
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
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
                )
                entries.forEach { entry ->
                    FoodEntryRow(
                        entry = entry,
                        onEdit = { onEditClick(entry) },
                        onDelete = { onDeleteClick(entry) }
                    )
                }
            } else {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "No foods logged",
                    style = MaterialTheme.typography.bodySmall,
                    color = MiTextSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.foodName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val secondary = buildString {
                entry.brandName?.let { append(it); append(" · ") }
                append("${entry.servingSizeG.roundToInt()}g")
            }
            Text(
                text = secondary,
                style = MaterialTheme.typography.labelSmall,
                color = MiTextSecondary
            )
            Text(
                text = "P ${entry.proteinG.roundToInt()}g  C ${entry.carbG.roundToInt()}g  F ${entry.fatG.roundToInt()}g",
                style = MaterialTheme.typography.labelSmall,
                color = MiTextSecondary.copy(alpha = 0.7f)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${entry.calories.roundToInt()} kcal",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MiTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
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
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Edit Macro Goals",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(20.dp))
                GoalField("Calories (kcal)", calorieInput) {
                    if (it.all { c -> c.isDigit() }) calorieInput = it
                }
                Spacer(Modifier.height(12.dp))
                GoalField("Protein (g)", proteinInput) {
                    if (it.all { c -> c.isDigit() }) proteinInput = it
                }
                Spacer(Modifier.height(12.dp))
                GoalField("Carbs (g)", carbsInput) {
                    if (it.all { c -> c.isDigit() }) carbsInput = it
                }
                Spacer(Modifier.height(12.dp))
                GoalField("Fat (g)", fatInput) {
                    if (it.all { c -> c.isDigit() }) fatInput = it
                }
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
                        onClick = {
                            val p = proteinInput.toDoubleOrNull() ?: return@Button
                            val c = carbsInput.toDoubleOrNull() ?: return@Button
                            val f = fatInput.toDoubleOrNull() ?: return@Button
                            val cal = calorieInput.toDoubleOrNull() ?: return@Button
                            onSave(p, c, f, cal)
                        },
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

@Composable
private fun GoalField(label: String, value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
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
}
