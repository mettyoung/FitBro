package com.mettyoung.fitbro.ui.dashboard

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.food.FoodResult
import com.mettyoung.fitbro.data.food.FoodSearchResult
import com.mettyoung.fitbro.data.food.OpenFoodFactsDataSource
import com.mettyoung.fitbro.data.model.FoodDiaryEntry
import com.mettyoung.fitbro.data.model.ServingUnit
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private sealed class SearchState {
    object Idle : SearchState()
    object Loading : SearchState()
    data class Results(val items: List<FoodSearchResult>) : SearchState()
    object Empty : SearchState()
    object Error : SearchState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchSheet(
    mealType: String,
    date: String,
    openFoodFactsDataSource: OpenFoodFactsDataSource,
    onDismiss: () -> Unit,
    onAddEntry: (FoodDiaryEntry) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedFood by remember { mutableStateOf<FoodSearchResult?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        if (selectedFood == null) {
            FoodSearchContent(
                openFoodFactsDataSource = openFoodFactsDataSource,
                onSelectFood = { selectedFood = it }
            )
        } else {
            FoodEntryContent(
                food = selectedFood!!,
                mealType = mealType,
                date = date,
                actionLabel = "Add to ${mealType.lowercase().replaceFirstChar { it.uppercase() }}",
                onBack = { selectedFood = null },
                onAdd = onAddEntry
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntrySheet(
    entry: FoodDiaryEntry,
    onDismiss: () -> Unit,
    onSave: (FoodDiaryEntry) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reconstructed = if (entry.servingSizeG > 0) {
        val factor = 100.0 / entry.servingSizeG
        FoodSearchResult(
            name = entry.foodName,
            brand = entry.brandName,
            caloriesPer100g = entry.calories * factor,
            proteinPer100g = entry.proteinG * factor,
            carbPer100g = entry.carbG * factor,
            fatPer100g = entry.fatG * factor,
            servingSizeG = null
        )
    } else {
        FoodSearchResult(entry.foodName, entry.brandName, 0.0, 0.0, 0.0, 0.0, null)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        FoodEntryContent(
            food = reconstructed,
            mealType = entry.mealType,
            date = entry.date,
            initialServingAmount = entry.servingSizeG,
            initialUnit = ServingUnit.GRAMS,
            actionLabel = "Save Changes",
            onBack = onDismiss,
            onAdd = { updated -> onSave(updated.copy(id = entry.id)) }
        )
    }
}

@Composable
private fun FoodSearchContent(
    openFoodFactsDataSource: OpenFoodFactsDataSource,
    onSelectFood: (FoodSearchResult) -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var searchState by remember { mutableStateOf<SearchState>(SearchState.Idle) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Search Food",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { q ->
                query = q
                searchJob?.cancel()
                if (q.length >= 2) {
                    searchState = SearchState.Loading
                    searchJob = scope.launch {
                        delay(400)
                        searchState = when (val r = openFoodFactsDataSource.search(q)) {
                            is FoodResult.Success ->
                                if (r.value.isEmpty()) SearchState.Empty
                                else SearchState.Results(r.value)
                            is FoodResult.Failure -> SearchState.Error
                        }
                    }
                } else {
                    searchState = SearchState.Idle
                }
            },
            placeholder = { Text("Type food name...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = ""; searchState = SearchState.Idle }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )
        Spacer(Modifier.height(12.dp))
        when (val state = searchState) {
            is SearchState.Idle -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Type to search foods", color = MiTextSecondary)
                }
            }
            is SearchState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MiOrange)
                }
            }
            is SearchState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No results found", color = MiTextSecondary)
                }
            }
            is SearchState.Error -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Search failed. Try again.", color = MaterialTheme.colorScheme.error)
                }
            }
            is SearchState.Results -> {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(state.items, key = { "${it.name}_${it.brand}" }) { food ->
                        FoodResultRow(food = food, onClick = { onSelectFood(food) })
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun FoodResultRow(food: FoodSearchResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (food.brand != null) {
                Text(
                    text = food.brand,
                    style = MaterialTheme.typography.labelSmall,
                    color = MiTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${food.caloriesPer100g.roundToInt()} kcal/100g",
            style = MaterialTheme.typography.labelSmall,
            color = MiTextSecondary
        )
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MiTextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
internal fun FoodEntryContent(
    food: FoodSearchResult,
    mealType: String,
    date: String,
    initialServingAmount: Double = 100.0,
    initialUnit: String = ServingUnit.GRAMS,
    actionLabel: String,
    onBack: () -> Unit,
    onAdd: (FoodDiaryEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var servingInput by remember { mutableStateOf(initialServingAmount.roundToInt().toString()) }
    var servingUnit by remember { mutableStateOf(initialUnit) }

    val servingAmount = servingInput.toDoubleOrNull() ?: 0.0
    val actualG = when (servingUnit) {
        ServingUnit.OZ -> servingAmount * ServingUnit.OZ_TO_GRAMS
        ServingUnit.SERVING -> servingAmount * (food.servingSizeG ?: 100.0)
        else -> servingAmount
    }
    val calories = food.caloriesPer100g * actualG / 100.0
    val proteinG = food.proteinPer100g * actualG / 100.0
    val carbG = food.carbPer100g * actualG / 100.0
    val fatG = food.fatPer100g * actualG / 100.0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = MiOrange
                )
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (food.brand != null) {
                    Text(
                        text = food.brand,
                        style = MaterialTheme.typography.labelSmall,
                        color = MiTextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MiOrange.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroPreviewItem("Calories", "${calories.roundToInt()}", "kcal", MiOrange)
                MacroPreviewItem("Protein", formatMacroG(proteinG), "g", MaterialTheme.colorScheme.error)
                MacroPreviewItem("Carbs", formatMacroG(carbG), "g", MaterialTheme.colorScheme.primary)
                MacroPreviewItem("Fat", formatMacroG(fatG), "g", MaterialTheme.colorScheme.tertiary)
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Serving Size",
            style = MaterialTheme.typography.labelMedium,
            color = MiTextSecondary
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = servingInput,
                onValueChange = { v ->
                    if (v.all { it.isDigit() || it == '.' } && v.count { it == '.' } <= 1) {
                        servingInput = v
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ServingUnitChip(ServingUnit.GRAMS, servingUnit) { servingUnit = it }
                ServingUnitChip(ServingUnit.OZ, servingUnit) { servingUnit = it }
                if (food.servingSizeG != null) {
                    ServingUnitChip(ServingUnit.SERVING, servingUnit) { servingUnit = it }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (actualG > 0) {
                    onAdd(
                        FoodDiaryEntry(
                            date = date,
                            mealType = mealType,
                            foodName = food.name,
                            brandName = food.brand,
                            calories = calories,
                            proteinG = proteinG,
                            carbG = carbG,
                            fatG = fatG,
                            servingSizeG = actualG,
                            servingUnit = servingUnit
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MiOrange),
            enabled = actualG > 0
        ) {
            Text(actionLabel)
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun MacroPreviewItem(label: String, value: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$value$unit",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MiTextSecondary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServingUnitChip(unit: String, selectedUnit: String, onSelect: (String) -> Unit) {
    FilterChip(
        selected = unit == selectedUnit,
        onClick = { onSelect(unit) },
        label = { Text(unit, style = MaterialTheme.typography.labelSmall) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MiOrange,
            selectedLabelColor = Color.White
        )
    )
}

private fun formatMacroG(value: Double): String {
    val tenths = (value * 10).roundToInt()
    val whole = tenths / 10
    val frac = tenths % 10
    return if (frac == 0) whole.toString() else "$whole.$frac"
}
