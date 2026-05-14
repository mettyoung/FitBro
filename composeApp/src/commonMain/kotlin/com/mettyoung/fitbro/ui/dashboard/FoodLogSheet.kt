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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mettyoung.fitbro.data.food.BarcodeScanResult
import com.mettyoung.fitbro.data.food.FoodDataSource
import com.mettyoung.fitbro.data.food.FoodError
import com.mettyoung.fitbro.data.food.FoodResult
import com.mettyoung.fitbro.data.food.FoodSearchResult
import com.mettyoung.fitbro.data.food.rememberBarcodeScanner
import com.mettyoung.fitbro.data.model.FoodDiaryEntry
import com.mettyoung.fitbro.data.model.ServingUnit
import com.mettyoung.fitbro.ui.ColorCarbs
import com.mettyoung.fitbro.ui.ColorFat
import com.mettyoung.fitbro.ui.ColorProtein
import com.mettyoung.fitbro.ui.FitroBroIcon
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
    data class Error(val isNetwork: Boolean) : SearchState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchSheet(
    mealType: String,
    date: String,
    foodDataSource: FoodDataSource,
    onDismiss: () -> Unit,
    onAddEntry: (FoodDiaryEntry) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedFood by remember { mutableStateOf<FoodSearchResult?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        tonalElevation = 0.dp
    ) {
        if (selectedFood == null) {
            FoodSearchContent(
                foodDataSource = foodDataSource,
                onSelectFood = { selectedFood = it }
            )
        } else {
            val food = selectedFood!!
            FoodEntryContent(
                food = food,
                mealType = mealType,
                date = date,
                initialServingAmount = if (food.servingSizeG != null) 1.0 else 100.0,
                initialUnit = if (food.servingSizeG != null) ServingUnit.SERVING else ServingUnit.GRAMS,
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
            servingSizeG = null,
            servingDescription = null,
            source = null
        )
    } else {
        FoodSearchResult(entry.foodName, entry.brandName, 0.0, 0.0, 0.0, 0.0, null, null, null)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        FoodEntryContent(
            food = reconstructed,
            mealType = entry.mealType,
            date = entry.date,
            initialServingAmount = entry.servingSizeG,
            initialUnit = ServingUnit.GRAMS,
            actionLabel = "Update Entry",
            onBack = onDismiss,
            onAdd = { updated -> onSave(updated.copy(id = entry.id)) }
        )
    }
}

@Composable
private fun FoodSearchContent(
    foodDataSource: FoodDataSource,
    onSelectFood: (FoodSearchResult) -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var searchState by remember { mutableStateOf<SearchState>(SearchState.Idle) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val barcodeScanner = rememberBarcodeScanner()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = FitroBroIcon,
                contentDescription = null,
                tint = MiOrange,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "Log Food",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Find nutrition data for millions of items",
                    style = MaterialTheme.typography.labelSmall,
                    color = MiOrange
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextField(
                value = query,
                onValueChange = { q ->
                    query = q
                    searchJob?.cancel()
                    if (q.length >= 2) {
                        searchState = SearchState.Loading
                        searchJob = scope.launch {
                            delay(400)
                            searchState = when (val r = foodDataSource.search(q)) {
                                is FoodResult.Success ->
                                    if (r.value.isEmpty()) SearchState.Empty
                                    else SearchState.Results(r.value)
                                is FoodResult.Failure -> when (r.error) {
                                    is FoodError.EmptyResults -> SearchState.Empty
                                    else -> SearchState.Error(isNetwork = true)
                                }
                            }
                        }
                    } else {
                        searchState = SearchState.Idle
                    }
                },
                placeholder = { Text("Search ingredients, snacks, meals...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MiOrange) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = ""; searchState = SearchState.Idle }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                )
            )

            if (barcodeScanner != null && foodDataSource.supportsBarcode) {
                IconButton(
                    onClick = {
                        scope.launch {
                            searchState = SearchState.Loading
                            when (val scanResult = barcodeScanner()) {
                                is BarcodeScanResult.Success -> {
                                    searchState = when (val r = foodDataSource.searchByBarcode(scanResult.barcode)) {
                                        is FoodResult.Success -> SearchState.Results(listOf(r.value))
                                        is FoodResult.Failure -> SearchState.Empty
                                    }
                                }
                                is BarcodeScanResult.Cancelled -> searchState = SearchState.Idle
                                is BarcodeScanResult.Error -> searchState = SearchState.Error(isNetwork = true)
                                is BarcodeScanResult.NotAvailable -> searchState = SearchState.Idle
                            }
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan barcode",
                        tint = MiOrange
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        when (val state = searchState) {
            is SearchState.Idle -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Search results will appear here", style = MaterialTheme.typography.bodyMedium, color = MiTextSecondary)
                }
            }
            is SearchState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MiOrange, strokeWidth = 3.dp)
                }
            }
            is SearchState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val emptyMsg = if (query.isNotBlank()) "No foods found matching \"$query\"" else "No food found for that barcode"
                    Text(emptyMsg, style = MaterialTheme.typography.bodyMedium, color = MiTextSecondary)
                }
            }
            is SearchState.Error -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Check your connection and try again", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                }
            }
            is SearchState.Results -> {
                LazyColumn(modifier = Modifier.heightIn(max = 500.dp)) {
                    itemsIndexed(state.items, key = { index, food -> "${index}_${food.name}_${food.brand}" }) { index, food ->
                        FoodResultRow(food = food, onClick = { onSelectFood(food) })
                        if (index < state.items.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FoodResultRow(food: FoodSearchResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = food.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (food.brand != null) {
                Text(
                    text = food.brand,
                    style = MaterialTheme.typography.bodySmall,
                    color = MiTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (food.servingDescription != null) {
                Text(
                    text = food.servingDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MiTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        if (food.source != null) {
            Text(
                text = food.source,
                style = MaterialTheme.typography.labelSmall,
                color = MiTextSecondary
            )
        }
        Spacer(Modifier.width(12.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MiTextSecondary.copy(alpha = 0.4f),
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
            .padding(horizontal = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(MaterialTheme.colorScheme.background, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (food.brand != null) {
                    Text(
                        text = food.brand,
                        style = MaterialTheme.typography.bodySmall,
                        color = MiTextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MacroPreviewItem("Calories", "${calories.roundToInt()}", "kcal", MiOrange)
                MacroPreviewItem("Protein", formatMacroG(proteinG), "g", ColorProtein)
                MacroPreviewItem("Carbs", formatMacroG(carbG), "g", ColorCarbs)
                MacroPreviewItem("Fat", formatMacroG(fatG), "g", ColorFat)
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = "SERVING SIZE",
            style = MaterialTheme.typography.labelSmall,
            color = MiTextSecondary
        )
        
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = servingInput,
                onValueChange = { v ->
                    if (v.all { it.isDigit() || it == '.' } && v.count { it == '.' } <= 1) {
                        servingInput = v
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    focusedLabelColor = MiOrange
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ServingUnitChip(ServingUnit.GRAMS, servingUnit) { servingUnit = it }
                ServingUnitChip(ServingUnit.OZ, servingUnit) { servingUnit = it }
                if (food.servingSizeG != null) {
                    ServingUnitChip(ServingUnit.SERVING, servingUnit) { servingUnit = it }
                }
            }
        }

        if (servingUnit != ServingUnit.GRAMS && actualG > 0) {
            Spacer(Modifier.height(6.dp))
            val gramLabel = when (servingUnit) {
                ServingUnit.SERVING -> "= ${actualG.roundToInt()}g  (1 serving = ${(food.servingSizeG ?: 100.0).roundToInt()}g)"
                else -> "= ${actualG.roundToInt()}g"
            }
            Text(
                text = gramLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MiTextSecondary
            )
        }

        Spacer(Modifier.height(40.dp))

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
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MiOrange),
            enabled = actualG > 0
        ) {
            Text(actionLabel, style = MaterialTheme.typography.titleMedium, color = Color.White)
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun MacroPreviewItem(label: String, value: String, unit: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = color
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
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
            selectedLabelColor = Color.White,
            containerColor = MaterialTheme.colorScheme.background,
            labelColor = MiTextSecondary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = unit == selectedUnit,
            borderColor = Color.Transparent,
            selectedBorderColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

private fun formatMacroG(value: Double): String {
    val tenths = (value * 10).roundToInt()
    val whole = tenths / 10
    val frac = tenths % 10
    return if (frac == 0) whole.toString() else "$whole.$frac"
}
