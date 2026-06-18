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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.food.FoodDataSource
import com.mettyoung.fitbro.data.model.CustomMeal
import com.mettyoung.fitbro.data.model.CustomMealItem
import com.mettyoung.fitbro.data.model.FoodDiaryEntry
import com.mettyoung.fitbro.data.model.MealType
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary
import com.mettyoung.fitbro.util.todayString
import kotlin.math.roundToInt

internal fun FoodDiaryEntry.toCustomMealItem(sortOrder: Long) = CustomMealItem(
    foodName = foodName,
    brandName = brandName,
    calories = calories,
    proteinG = proteinG,
    carbG = carbG,
    fatG = fatG,
    servingSizeG = servingSizeG,
    servingUnit = servingUnit,
    foodId = foodId,
    sortOrder = sortOrder
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomMealManagerSheet(
    customMeals: List<CustomMeal>,
    foodDataSource: FoodDataSource,
    onCreate: (String, List<CustomMealItem>) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var building by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        tonalElevation = 0.dp
    ) {
        if (building) {
            CustomMealBuilder(
                foodDataSource = foodDataSource,
                onSave = { name, items ->
                    onCreate(name, items)
                    building = false
                },
                onCancel = { building = false }
            )
        } else {
            CustomMealList(
                customMeals = customMeals,
                onNew = { building = true },
                onRename = onRename,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun CustomMealList(
    customMeals: List<CustomMeal>,
    onNew: () -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var renameTarget by remember { mutableStateOf<CustomMeal?>(null) }
    var deleteTarget by remember { mutableStateOf<CustomMeal?>(null) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = null,
                tint = MiOrange,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.size(12.dp))
            Column {
                Text(
                    text = "Custom Meals",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Combine foods into single-tap logs",
                    style = MaterialTheme.typography.labelSmall,
                    color = MiOrange
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onNew,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MiOrange)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Create New Meal", style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(Modifier.height(24.dp))

        if (customMeals.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No custom meals yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiTextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(customMeals, key = { it.id }) { meal ->
                    CustomMealRow(
                        meal = meal,
                        onRename = { renameTarget = meal },
                        onDelete = { deleteTarget = meal }
                    )
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }

    renameTarget?.let { meal ->
        var name by remember(meal.id) { mutableStateOf(meal.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename meal") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Meal Name") },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MiOrange,
                        focusedLabelColor = MiOrange
                    )
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        onRename(meal.id, name)
                        renameTarget = null
                    }
                ) { Text("Save", color = MiOrange) }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } },
            shape = RoundedCornerShape(24.dp)
        )
    }

    deleteTarget?.let { meal ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete meal") },
            text = { Text("Delete \"${meal.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(meal.id)
                    deleteTarget = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun CustomMealRow(
    meal: CustomMeal,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val totalCalories = meal.items.sumOf { it.calories }.roundToInt()
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meal.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${meal.items.size} items · $totalCalories kcal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiTextSecondary
                )
            }
            Row {
                IconButton(onClick = onRename, modifier = Modifier.background(MaterialTheme.colorScheme.background, CircleShape)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Rename", tint = MiTextSecondary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.size(8.dp))
                IconButton(onClick = onDelete, modifier = Modifier.background(MaterialTheme.colorScheme.background, CircleShape)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun CustomMealBuilder(
    foodDataSource: FoodDataSource,
    onSave: (String, List<CustomMealItem>) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    val items = remember { mutableListOf<CustomMealItem>().toMutableStateList() }
    var searching by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text(
            text = "Build New Meal",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            label = { Text("Meal Name") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MiOrange,
                focusedLabelColor = MiOrange
            )
        )
        
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Foods (${items.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            TextButton(
                onClick = { searching = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = MiOrange)
                Spacer(Modifier.size(4.dp))
                Text("Add food", color = MiOrange)
            }
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp).background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Add at least one food",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiTextSecondary.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items.size) { index ->
                    val item = items[index]
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.foodName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${item.calories.roundToInt()} kcal · ${item.servingSizeG.roundToInt()}g",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MiTextSecondary
                                )
                            }
                            IconButton(onClick = { items.removeAt(index) }, modifier = Modifier.background(MaterialTheme.colorScheme.background, CircleShape)) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f).height(56.dp), shape = RoundedCornerShape(16.dp)) { Text("Cancel") }
            Button(
                onClick = { onSave(name, items.toList()) },
                enabled = name.isNotBlank() && items.isNotEmpty(),
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MiOrange)
            ) { Text("Save Meal", style = MaterialTheme.typography.titleMedium) }
        }
        
        Spacer(Modifier.height(32.dp))
    }

    if (searching) {
        FoodSearchSheet(
            mealType = MealType.BREAKFAST,
            date = todayString(),
            foodDataSource = foodDataSource,
            onDismiss = { searching = false },
            onAddEntry = { entry ->
                items.add(entry.toCustomMealItem(items.size.toLong()))
                searching = false
            }
        )
    }
}

@Composable
private fun IconButton(onClick: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onClick, modifier = modifier, content = content)
}
