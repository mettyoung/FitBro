package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.model.CustomFood
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary
import kotlin.math.roundToInt

private sealed class ManagerScreen {
    object List : ManagerScreen()
    object Create : ManagerScreen()
    data class Edit(val food: CustomFood) : ManagerScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomFoodManagerSheet(
    customFoods: List<CustomFood>,
    onCreate: (CustomFood) -> Unit,
    onUpdate: (CustomFood) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var screen by remember { mutableStateOf<ManagerScreen>(ManagerScreen.List) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        tonalElevation = 0.dp
    ) {
        when (val s = screen) {
            is ManagerScreen.List -> CustomFoodList(
                customFoods = customFoods,
                onNew = { screen = ManagerScreen.Create },
                onEdit = { screen = ManagerScreen.Edit(it) },
                onDelete = onDelete
            )
            is ManagerScreen.Create -> CustomFoodFormContent(
                title = "New Custom Food",
                actionLabel = "Save",
                onBack = { screen = ManagerScreen.List },
                onSubmit = {
                    onCreate(it)
                    screen = ManagerScreen.List
                }
            )
            is ManagerScreen.Edit -> CustomFoodFormContent(
                title = "Edit Custom Food",
                actionLabel = "Save changes",
                initial = s.food,
                onBack = { screen = ManagerScreen.List },
                onSubmit = {
                    onUpdate(it)
                    screen = ManagerScreen.List
                }
            )
        }
    }
}

@Composable
private fun CustomFoodList(
    customFoods: List<CustomFood>,
    onNew: () -> Unit,
    onEdit: (CustomFood) -> Unit,
    onDelete: (Long) -> Unit
) {
    var deleteTarget by remember { mutableStateOf<CustomFood?>(null) }

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        Text(
            text = "Custom foods",
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
            Text("New custom food")
        }
        Spacer(Modifier.size(16.dp))

        if (customFoods.isEmpty()) {
            Text(
                text = "No custom foods yet. Create one here, or via \"Create custom food\" in the Log Food search.",
                style = MaterialTheme.typography.bodyMedium,
                color = MiTextSecondary,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(customFoods, key = { it.id }) { food ->
                    CustomFoodRow(
                        food = food,
                        onEdit = { onEdit(food) },
                        onDelete = { deleteTarget = food }
                    )
                }
            }
        }
    }

    deleteTarget?.let { food ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete food") },
            text = { Text("Delete \"${food.name}\"? This cannot be undone. Existing diary entries are unaffected.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(food.id)
                    deleteTarget = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CustomFoodRow(
    food: CustomFood,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                    text = food.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${food.servingSizeG.roundToInt()}g · ${food.calories.roundToInt()} kcal" +
                        (food.brandName?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MiTextSecondary
                )
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MiTextSecondary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MiTextSecondary)
            }
        }
    }
}
