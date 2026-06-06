package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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

private fun FoodDiaryEntry.toCustomMealItem(sortOrder: Long) = CustomMealItem(
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

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        Text(
            text = "Custom meals",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.size(16.dp))

        Button(
            onClick = onNew,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MiOrange)
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("New custom meal")
        }
        Spacer(Modifier.size(16.dp))

        if (customMeals.isEmpty()) {
            Text(
                text = "No custom meals yet. Build one from foods, or multi-select diary entries to save them as a meal.",
                style = MaterialTheme.typography.bodyMedium,
                color = MiTextSecondary,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        onRename(meal.id, name)
                        renameTarget = null
                    }
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } }
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
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meal.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${meal.items.size} items · $totalCalories kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MiTextSecondary
                )
            }
            IconButton(onClick = onRename) {
                Icon(Icons.Filled.Edit, contentDescription = "Rename", tint = MiTextSecondary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MiTextSecondary)
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

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        Text(
            text = "New custom meal",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.size(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            label = { Text("Meal name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.size(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Foods (${items.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { searching = true }) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = MiOrange)
                Spacer(Modifier.size(4.dp))
                Text("Add food", color = MiOrange)
            }
        }

        if (items.isEmpty()) {
            Text(
                text = "Add at least one food.",
                style = MaterialTheme.typography.bodySmall,
                color = MiTextSecondary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(items.size) { index ->
                    val item = items[index]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.foodName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${item.calories.roundToInt()} kcal · ${item.servingSizeG.roundToInt()}g",
                                style = MaterialTheme.typography.bodySmall,
                                color = MiTextSecondary
                            )
                        }
                        IconButton(onClick = { items.removeAt(index) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MiTextSecondary)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.size(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = { onSave(name, items.toList()) },
                enabled = name.isNotBlank() && items.isNotEmpty(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MiOrange)
            ) { Text("Save meal") }
        }
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
