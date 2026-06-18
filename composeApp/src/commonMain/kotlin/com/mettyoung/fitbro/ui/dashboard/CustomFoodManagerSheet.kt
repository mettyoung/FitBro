package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
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
    data object List : ManagerScreen()
    data object Create : ManagerScreen()
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

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                tint = MiOrange,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.size(12.dp))
            Column {
                Text(
                    text = "Custom Foods",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Manage your private library",
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
            Text("Create New Food", style = MaterialTheme.typography.titleMedium)
        }
        
        Spacer(Modifier.height(24.dp))

        if (customFoods.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No custom foods yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiTextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
        Spacer(Modifier.height(32.dp))
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
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun CustomFoodRow(
    food: CustomFood,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
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
                    text = food.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${food.servingSizeG.roundToInt()}g · ${food.calories.roundToInt()} kcal" +
                        (food.brandName?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiTextSecondary
                )
            }
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.background(MaterialTheme.colorScheme.background, CircleShape)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = MiTextSecondary, modifier = Modifier.size(20.dp))
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
private fun IconButton(onClick: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onClick, modifier = modifier, content = content)
}
