package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import com.mettyoung.fitbro.getPlatform
import com.mettyoung.fitbro.data.cache.UserSettingsDataSource
import com.mettyoung.fitbro.data.food.FoodDataSource
import com.mettyoung.fitbro.data.model.FoodDiaryEntry
import com.mettyoung.fitbro.data.model.MacroDataSource
import com.mettyoung.fitbro.data.model.MealType
import com.mettyoung.fitbro.ui.FitroBroIcon
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
    customMealStateHolder: CustomMealStateHolder,
    foodDataSource: FoodDataSource,
    onDateSelected: (String) -> Unit = {},
    onBalanceRefreshNeeded: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val foodState by foodDiaryStateHolder.state.collectAsState()
    val selectedDate by foodDiaryStateHolder.selectedDate.collectAsState()
    val weeklyTotals by foodDiaryStateHolder.weeklyTotals.collectAsState()
    val customMeals by customMealStateHolder.customMeals.collectAsState()

    val today = todayString()

    var proteinGoal by remember { mutableStateOf(userSettingsDataSource.getProteinGoalG()) }
    var carbsGoal by remember { mutableStateOf(userSettingsDataSource.getCarbsGoalG()) }
    var fatGoal by remember { mutableStateOf(userSettingsDataSource.getFatGoalG()) }
    var calorieGoal by remember { mutableStateOf(userSettingsDataSource.getCalorieGoalKcal()) }

    var showGoalsDialog by remember { mutableStateOf(false) }
    var showCustomMeals by remember { mutableStateOf(false) }
    var addingToMeal by remember { mutableStateOf<String?>(null) }
    var customMealTarget by remember { mutableStateOf<String?>(null) }
    var editingEntry by remember { mutableStateOf<FoodDiaryEntry?>(null) }

    var selectionMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }
    var namingSelection by remember { mutableStateOf(false) }
    val exitSelection = {
        selectionMode = false
        selectedIds.clear()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<FoodDiaryEntry?>(null) }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val healthNutritionSourceName = remember { getPlatform().healthNutritionSourceName }

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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = FitroBroIcon,
                                contentDescription = null,
                                tint = MiOrange,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.width(12.dp))
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
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showCustomMeals = true },
                                modifier = Modifier.background(MaterialTheme.colorScheme.background, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.List,
                                    contentDescription = "Custom meals",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.width(8.dp))
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
                            onClick = { onDateSelected(selectedDate.plusDays(1)) }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next"
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    MacroDataSourceToggle(
                        selectedSource = foodState.macroDataSource,
                        healthSourceLabel = healthNutritionSourceName,
                        onSourceSelected = { source ->
                            foodDiaryStateHolder.setMacroDataSourceForSelectedDate(source)
                            onBalanceRefreshNeeded()
                        }
                    )

                    if (foodState.error != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = foodState.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
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

            if (foodState.macroDataSource == MacroDataSource.FOOD_DIARY) {
                MealType.ordered.forEach { mealType ->
                    item(key = mealType) {
                        val entries = (foodState.entriesByMeal[mealType] ?: emptyList())
                            .filter { it.id != pendingDelete?.id }
                        FoodDiarySection(
                            mealType = mealType,
                            entries = entries,
                            onAddFood = { addingToMeal = mealType },
                            onAddCustomMeal = { customMealTarget = mealType },
                            onEditClick = { editingEntry = it },
                            onDeleteClick = { entry ->
                                pendingDelete?.let { prev ->
                                    scope.launch {
                                        foodDiaryStateHolder.deleteEntry(prev.id).join()
                                        onBalanceRefreshNeeded()
                                    }
                                }
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
                                            foodDiaryStateHolder.deleteEntry(entry.id).join()
                                            onBalanceRefreshNeeded()
                                            pendingDelete = null
                                        }
                                    }
                                }
                            },
                            onReorder = { orderedIds ->
                                foodDiaryStateHolder.reorderMeal(selectedDate, mealType, orderedIds)
                            },
                            selectionMode = selectionMode,
                            selectedIds = selectedIds.toSet(),
                            onToggleSelect = { entry ->
                                if (selectedIds.contains(entry.id)) selectedIds.remove(entry.id)
                                else selectedIds.add(entry.id)
                            },
                            onLongPress = { entry ->
                                selectionMode = true
                                if (!selectedIds.contains(entry.id)) selectedIds.add(entry.id)
                            },
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                    }
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

        if (selectionMode) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = exitSelection) { Text("Cancel") }
                    Text(
                        text = "${selectedIds.size} selected",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Button(
                        onClick = { namingSelection = true },
                        enabled = selectedIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = MiOrange)
                    ) { Text("Save as meal") }
                }
            }
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
            foodDataSource = foodDataSource,
            onDismiss = { addingToMeal = null },
            onAddEntry = { entry ->
                scope.launch {
                    foodDiaryStateHolder.addEntry(entry).join()
                    onBalanceRefreshNeeded()
                }
                addingToMeal = null
            }
        )
    }

    editingEntry?.let { entry ->
        EditEntrySheet(
            entry = entry,
            foodDataSource = foodDataSource,
            onDismiss = { editingEntry = null },
            onSave = { updated ->
                scope.launch {
                    foodDiaryStateHolder.updateEntry(updated).join()
                    onBalanceRefreshNeeded()
                }
                editingEntry = null
            }
        )
    }

    if (namingSelection) {
        var mealName by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { namingSelection = false }) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Save ${selectedIds.size} items as custom meal",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(16.dp))
                    TextField(
                        value = mealName,
                        onValueChange = { mealName = it },
                        singleLine = true,
                        placeholder = { Text("Meal name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TextButton(onClick = { namingSelection = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val allEntries = foodState.entriesByMeal.values.flatten().associateBy { it.id }
                                val items = selectedIds.mapIndexedNotNull { index, id ->
                                    allEntries[id]?.toCustomMealItem(index.toLong())
                                }
                                customMealStateHolder.create(mealName, items)
                                namingSelection = false
                                exitSelection()
                                scope.launch {
                                    snackbarHostState.showSnackbar("Saved \"${mealName.trim()}\"")
                                }
                            },
                            enabled = mealName.isNotBlank() && selectedIds.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MiOrange)
                        ) { Text("Save") }
                    }
                }
            }
        }
    }

    customMealTarget?.let { mealType ->
        Dialog(onDismissRequest = { customMealTarget = null }) {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Add custom meal to ${mealType.lowercase().replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(16.dp))
                    if (customMeals.isEmpty()) {
                        Text(
                            text = "No custom meals yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MiTextSecondary
                        )
                    } else {
                        customMeals.forEach { meal ->
                            val kcal = meal.items.sumOf { it.calories }.roundToInt()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val target = mealType
                                        scope.launch {
                                            meal.items.forEach { item ->
                                                foodDiaryStateHolder.addEntry(
                                                    FoodDiaryEntry(
                                                        date = selectedDate,
                                                        mealType = target,
                                                        foodName = item.foodName,
                                                        brandName = item.brandName,
                                                        calories = item.calories,
                                                        proteinG = item.proteinG,
                                                        carbG = item.carbG,
                                                        fatG = item.fatG,
                                                        servingSizeG = item.servingSizeG,
                                                        servingUnit = item.servingUnit,
                                                        foodId = item.foodId
                                                    )
                                                ).join()
                                            }
                                            onBalanceRefreshNeeded()
                                        }
                                        customMealTarget = null
                                    }
                                    .padding(vertical = 12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = meal.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${meal.items.size} items · $kcal kcal",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MiTextSecondary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { customMealTarget = null }, modifier = Modifier.align(Alignment.End)) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    if (showCustomMeals) {
        CustomMealManagerSheet(
            customMeals = customMeals,
            foodDataSource = foodDataSource,
            onCreate = { name, items -> customMealStateHolder.create(name, items) },
            onRename = { id, name -> customMealStateHolder.rename(id, name) },
            onDelete = { id -> customMealStateHolder.delete(id) },
            onDismiss = { showCustomMeals = false }
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
private fun MacroDataSourceToggle(
    selectedSource: MacroDataSource,
    healthSourceLabel: String,
    onSourceSelected: (MacroDataSource) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MacroDataSourceButton(
            text = healthSourceLabel,
            selected = selectedSource == MacroDataSource.HEALTH_CONNECT,
            onClick = { onSourceSelected(MacroDataSource.HEALTH_CONNECT) },
            modifier = Modifier.weight(1f)
        )
        MacroDataSourceButton(
            text = "Food Diary",
            selected = selectedSource == MacroDataSource.FOOD_DIARY,
            onClick = { onSourceSelected(MacroDataSource.FOOD_DIARY) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MacroDataSourceButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (selected) MiOrange else Color.Transparent,
            contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FoodDiarySection(
    mealType: String,
    entries: List<FoodDiaryEntry>,
    onAddFood: () -> Unit,
    onAddCustomMeal: () -> Unit,
    onEditClick: (FoodDiaryEntry) -> Unit,
    onDeleteClick: (FoodDiaryEntry) -> Unit,
    onReorder: (orderedIds: List<Long>) -> Unit,
    selectionMode: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
    onToggleSelect: (FoodDiaryEntry) -> Unit = {},
    onLongPress: (FoodDiaryEntry) -> Unit = {},
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
                Box {
                    var menuOpen by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier
                            .size(40.dp)
                            .background(MiOrange.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = MiOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Add food") },
                            onClick = { menuOpen = false; onAddFood() }
                        )
                        DropdownMenuItem(
                            text = { Text("Add custom meal") },
                            onClick = { menuOpen = false; onAddCustomMeal() }
                        )
                    }
                }
            }

            if (entries.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                ReorderableEntries(
                    entries = entries,
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick,
                    onReorder = onReorder,
                    selectionMode = selectionMode,
                    selectedIds = selectedIds,
                    onToggleSelect = onToggleSelect,
                    onLongPress = onLongPress
                )
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

/**
 * Manual drag-to-reorder for the entries within a single meal section.
 * Renders a plain Column (already inside the outer LazyColumn), so reordering is
 * confined to this section. Long-press the drag handle to pick up a row; on drop the
 * new id order is persisted via [onReorder]. Local order resets whenever upstream
 * [entries] change, so the StateFlow remains the source of truth after refresh.
 */
@Composable
private fun ReorderableEntries(
    entries: List<FoodDiaryEntry>,
    onEditClick: (FoodDiaryEntry) -> Unit,
    onDeleteClick: (FoodDiaryEntry) -> Unit,
    onReorder: (orderedIds: List<Long>) -> Unit,
    selectionMode: Boolean = false,
    selectedIds: Set<Long> = emptySet(),
    onToggleSelect: (FoodDiaryEntry) -> Unit = {},
    onLongPress: (FoodDiaryEntry) -> Unit = {}
) {
    var items by remember(entries) { mutableStateOf(entries) }
    var draggingId by remember(entries) { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val heights = remember(entries) { mutableStateMapOf<Long, Int>() }

    Column(modifier = Modifier.fillMaxWidth()) {
        items.forEachIndexed { index, entry ->
            key(entry.id) {
            val isDragging = entry.id == draggingId
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { heights[entry.id] = it.size.height }
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffsetY else 0f
                    }
            ) {
                FoodEntryRow(
                    entry = entry,
                    onEdit = { onEditClick(entry) },
                    onDelete = { onDeleteClick(entry) },
                    selectionMode = selectionMode,
                    isSelected = entry.id in selectedIds,
                    onToggleSelect = { onToggleSelect(entry) },
                    onLongPress = { onLongPress(entry) },
                    dragHandle = {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Reorder",
                            tint = MiTextSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .pointerInput(entry.id, items.size) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggingId = entry.id
                                            dragOffsetY = 0f
                                        },
                                        onDragEnd = {
                                            draggingId = null
                                            dragOffsetY = 0f
                                            onReorder(items.map { it.id })
                                        },
                                        onDragCancel = {
                                            // A mid-drag list reorder can reposition the keyed
                                            // node and cancel the pointer, so persist here too —
                                            // otherwise the new order is shown but never saved.
                                            draggingId = null
                                            dragOffsetY = 0f
                                            onReorder(items.map { it.id })
                                        },
                                        onDrag = { change, drag ->
                                            change.consume()
                                            dragOffsetY += drag.y
                                            val cur = items.indexOfFirst { it.id == entry.id }
                                            if (cur >= 0) {
                                                // Move down past the next row's midpoint.
                                                if (cur < items.lastIndex) {
                                                    val nextH = heights[items[cur + 1].id] ?: 0
                                                    if (nextH > 0 && dragOffsetY > nextH / 2f) {
                                                        items = items.toMutableList().apply {
                                                            add(cur + 1, removeAt(cur))
                                                        }
                                                        dragOffsetY -= nextH
                                                    }
                                                }
                                                // Move up past the previous row's midpoint.
                                                if (cur > 0) {
                                                    val prevH = heights[items[cur - 1].id] ?: 0
                                                    if (prevH > 0 && dragOffsetY < -prevH / 2f) {
                                                        items = items.toMutableList().apply {
                                                            add(cur - 1, removeAt(cur))
                                                        }
                                                        dragOffsetY += prevH
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                        )
                    }
                )
            }
            if (index < items.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }
            }
        }
    }
}

@Composable
private fun FoodEntryRow(
    entry: FoodDiaryEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dragHandle: (@Composable () -> Unit)? = null,
    selectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(entry.id, selectionMode) {
                detectTapGestures(
                    onLongPress = { if (!selectionMode) onLongPress() },
                    onTap = { if (selectionMode) onToggleSelect() }
                )
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectionMode) {
            Box(modifier = Modifier.padding(end = 12.dp)) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() })
            }
        } else if (dragHandle != null) {
            Box(modifier = Modifier.padding(end = 12.dp)) { dragHandle() }
        }
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
            if (!selectionMode) {
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
