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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.sp
import com.mettyoung.fitbro.data.food.BarcodeScanResult
import com.mettyoung.fitbro.data.food.FoodDataSource
import com.mettyoung.fitbro.data.food.FoodDetail
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

// Unified state for FoodSearchSheet — controls which "screen" is shown
private sealed class SheetContent {
    object Search : SheetContent()
    object Loading : SheetContent()
    data class Entry(
        val name: String,
        val brand: String?,
        val foodId: String?,
        val detail: FoodDetail
    ) : SheetContent()
    object Error : SheetContent()
}

// Per-100g data for gram-mode editing of legacy entries (no foodId stored)
internal data class GramModeData(
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbPer100g: Double,
    val fatPer100g: Double,
    val servingSizeG: Double?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchSheet(
    mealType: String,
    date: String,
    foodDataSource: FoodDataSource,
    onDismiss: () -> Unit,
    onAddEntry: (FoodDiaryEntry) -> Unit
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var content by remember { mutableStateOf<SheetContent>(SheetContent.Search) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        tonalElevation = 0.dp
    ) {
        when (val c = content) {
            is SheetContent.Search -> {
                FoodSearchContent(
                    foodDataSource = foodDataSource,
                    onSelectFood = { food ->
                        val foodId = food.foodId
                        if (foodId != null && foodDataSource.supportsFoodDetail) {
                            content = SheetContent.Loading
                            scope.launch {
                                content = when (val r = foodDataSource.getFoodDetail(foodId)) {
                                    is FoodResult.Success -> SheetContent.Entry(
                                        name = food.name,
                                        brand = food.brand,
                                        foodId = foodId,
                                        detail = r.value
                                    )
                                    is FoodResult.Failure -> SheetContent.Error
                                }
                            }
                        } else {
                            content = SheetContent.Error
                        }
                    },
                    onBarcodeLoading = { content = SheetContent.Loading },
                    onBarcodeDetail = { detail ->
                        content = SheetContent.Entry(
                            name = detail.name,
                            brand = detail.brand,
                            foodId = detail.foodId,
                            detail = detail
                        )
                    },
                    onBarcodeEmpty = { content = SheetContent.Search }
                )
            }
            is SheetContent.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MiOrange, strokeWidth = 3.dp)
                }
            }
            is SheetContent.Error -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(Modifier.height(48.dp))
                    Text(
                        text = "Couldn't load serving details",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = { content = SheetContent.Search },
                        colors = ButtonDefaults.buttonColors(containerColor = MiOrange),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Go back", color = Color.White)
                    }
                    Spacer(Modifier.height(48.dp))
                }
            }
            is SheetContent.Entry -> {
                FoodEntryContent(
                    name = c.name,
                    brand = c.brand,
                    foodId = c.foodId,
                    foodDetail = c.detail,
                    gramModeData = null,
                    mealType = mealType,
                    date = date,
                    initialServingAmount = 1.0,
                    initialUnit = ServingUnit.SERVING,
                    actionLabel = "Add to ${mealType.lowercase().replaceFirstChar { it.uppercase() }}",
                    onBack = { content = SheetContent.Search },
                    onAdd = onAddEntry
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntrySheet(
    entry: FoodDiaryEntry,
    foodDataSource: FoodDataSource,
    onDismiss: () -> Unit,
    onSave: (FoodDiaryEntry) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // If entry has a foodId, start in Loading; otherwise go straight to gram mode
    var detailState by remember {
        mutableStateOf<SheetContent>(
            if (entry.foodId != null) SheetContent.Loading else SheetContent.Search
        )
    }

    LaunchedEffect(Unit) {
        val foodId = entry.foodId ?: return@LaunchedEffect
        if (foodDataSource.supportsFoodDetail) {
            detailState = when (val r = foodDataSource.getFoodDetail(foodId)) {
                is FoodResult.Success -> SheetContent.Entry(
                    name = entry.foodName,
                    brand = entry.brandName,
                    foodId = foodId,
                    detail = r.value
                )
                is FoodResult.Failure -> SheetContent.Error
            }
        } else {
            detailState = SheetContent.Search  // fall through to gram mode
        }
    }

    val gramModeData: GramModeData? = if (entry.foodId == null && entry.servingSizeG > 0) {
        val factor = 100.0 / entry.servingSizeG
        GramModeData(
            caloriesPer100g = entry.calories * factor,
            proteinPer100g = entry.proteinG * factor,
            carbPer100g = entry.carbG * factor,
            fatPer100g = entry.fatG * factor,
            servingSizeG = null
        )
    } else null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        when (val s = detailState) {
            is SheetContent.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MiOrange, strokeWidth = 3.dp)
                }
            }
            is SheetContent.Error -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(Modifier.height(48.dp))
                    Text(
                        text = "Couldn't load serving details",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = MiOrange),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Go back", color = Color.White)
                    }
                    Spacer(Modifier.height(48.dp))
                }
            }
            else -> {
                // SheetContent.Search means "gram mode"; SheetContent.Entry means "detail mode"
                val detail = (s as? SheetContent.Entry)?.detail
                FoodEntryContent(
                    name = entry.foodName,
                    brand = entry.brandName,
                    foodId = entry.foodId,
                    foodDetail = detail,
                    gramModeData = gramModeData,
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
    }
}

@Composable
private fun FoodSearchContent(
    foodDataSource: FoodDataSource,
    onSelectFood: (FoodSearchResult) -> Unit,
    onBarcodeLoading: () -> Unit,
    onBarcodeDetail: (FoodDetail) -> Unit,
    onBarcodeEmpty: () -> Unit
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
                            when (val scanResult = barcodeScanner()) {
                                is BarcodeScanResult.Success -> {
                                    onBarcodeLoading()
                                    when (val r = foodDataSource.searchByBarcode(scanResult.barcode)) {
                                        is FoodResult.Success -> onBarcodeDetail(r.value)
                                        is FoodResult.Failure -> onBarcodeEmpty()
                                    }
                                }
                                is BarcodeScanResult.Cancelled -> Unit
                                is BarcodeScanResult.Error -> searchState = SearchState.Error(isNetwork = true)
                                is BarcodeScanResult.NotAvailable -> Unit
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
            Text(
                text = food.displayText,
                style = MaterialTheme.typography.bodySmall,
                color = MiTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(16.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MiTextSecondary.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FoodEntryContent(
    name: String,
    brand: String?,
    foodId: String?,
    foodDetail: FoodDetail?,
    gramModeData: GramModeData?,
    mealType: String,
    date: String,
    initialServingAmount: Double = 100.0,
    initialUnit: String = ServingUnit.GRAMS,
    actionLabel: String,
    onBack: () -> Unit,
    onAdd: (FoodDiaryEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val useServingDropdown = foodDetail != null && foodDetail.servings.isNotEmpty()

    // Reference serving for custom proration — first serving with a valid metric amount
    val referenceServing = remember(foodDetail) {
        foodDetail?.servings?.firstOrNull { (it.metricAmount ?: 0.0) > 0 }
    }
    val customUnit = referenceServing?.metricUnit ?: "g"

    // Serving dropdown mode state
    var selectedServing by remember(foodDetail) {
        mutableStateOf(foodDetail?.servings?.firstOrNull())
    }
    var customMode by remember { mutableStateOf(false) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var quantityInput by remember { mutableStateOf("1") }

    // Gram input mode state
    var servingInput by remember { mutableStateOf(initialServingAmount.roundToInt().toString()) }
    var servingUnit by remember { mutableStateOf(initialUnit) }

    val calories: Double
    val proteinG: Double
    val carbG: Double
    val fatG: Double
    val actualG: Double

    if (useServingDropdown) {
        val inputAmount = quantityInput.toDoubleOrNull() ?: 0.0
        if (customMode && referenceServing != null && (referenceServing.metricAmount ?: 0.0) > 0) {
            val factor = inputAmount / referenceServing.metricAmount!!
            calories = referenceServing.calories * factor
            proteinG = referenceServing.proteinG * factor
            carbG = referenceServing.carbG * factor
            fatG = referenceServing.fatG * factor
            actualG = if (customUnit == "g") inputAmount else 0.0
        } else if (!customMode && selectedServing != null) {
            calories = selectedServing!!.calories * inputAmount
            proteinG = selectedServing!!.proteinG * inputAmount
            carbG = selectedServing!!.carbG * inputAmount
            fatG = selectedServing!!.fatG * inputAmount
            actualG = if (selectedServing!!.metricUnit == "g") (selectedServing!!.metricAmount ?: 0.0) * inputAmount else 0.0
        } else {
            calories = 0.0; proteinG = 0.0; carbG = 0.0; fatG = 0.0; actualG = 0.0
        }
    } else {
        val grams = gramModeData
        val servingAmount = servingInput.toDoubleOrNull() ?: 0.0
        actualG = when (servingUnit) {
            ServingUnit.OZ -> servingAmount * ServingUnit.OZ_TO_GRAMS
            ServingUnit.SERVING -> servingAmount * (grams?.servingSizeG ?: 100.0)
            else -> servingAmount
        }
        calories = (grams?.caloriesPer100g ?: 0.0) * actualG / 100.0
        proteinG = (grams?.proteinPer100g ?: 0.0) * actualG / 100.0
        carbG = (grams?.carbPer100g ?: 0.0) * actualG / 100.0
        fatG = (grams?.fatPer100g ?: 0.0) * actualG / 100.0
    }

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
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (brand != null) {
                    Text(
                        text = brand,
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

        if (useServingDropdown) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    value = quantityInput,
                    onValueChange = { v ->
                        if (v.all { it.isDigit() || it == '.' } && v.count { it == '.' } <= 1) {
                            quantityInput = v
                        }
                    },
                    label = if (customMode) {{ Text(customUnit, style = MaterialTheme.typography.labelSmall) }} else null,
                    modifier = Modifier.width(80.dp),
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

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    TextField(
                        value = if (customMode) "Custom amount ($customUnit)" else selectedServing?.description ?: "",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        foodDetail.servings.forEach { serving ->
                            DropdownMenuItem(
                                text = { Text(serving.description, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    selectedServing = serving
                                    customMode = false
                                    quantityInput = "1"
                                    dropdownExpanded = false
                                }
                            )
                        }
                        if (referenceServing != null) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Custom amount ($customUnit)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MiOrange
                                    )
                                },
                                onClick = {
                                    customMode = true
                                    selectedServing = null
                                    quantityInput = ""
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        } else {
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
                    if (gramModeData?.servingSizeG != null) {
                        ServingUnitChip(ServingUnit.SERVING, servingUnit) { servingUnit = it }
                    }
                }
            }

            if (servingUnit != ServingUnit.GRAMS && actualG > 0) {
                Spacer(Modifier.height(6.dp))
                val gramLabel = when (servingUnit) {
                    ServingUnit.SERVING -> "= ${actualG.roundToInt()}g  (1 serving = ${(gramModeData?.servingSizeG ?: 100.0).roundToInt()}g)"
                    else -> "= ${actualG.roundToInt()}g"
                }
                Text(
                    text = gramLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MiTextSecondary
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        val addEnabled = if (useServingDropdown) {
            val amount = quantityInput.toDoubleOrNull() ?: 0.0
            if (customMode) amount > 0 && referenceServing != null
            else amount > 0 && selectedServing != null
        } else {
            actualG > 0
        }

        Button(
            onClick = {
                if (addEnabled) {
                    onAdd(
                        FoodDiaryEntry(
                            date = date,
                            mealType = mealType,
                            foodName = name,
                            brandName = brand,
                            calories = calories,
                            proteinG = proteinG,
                            carbG = carbG,
                            fatG = fatG,
                            servingSizeG = actualG,
                            servingUnit = if (useServingDropdown && actualG > 0) ServingUnit.GRAMS
                                         else if (useServingDropdown) ServingUnit.SERVING
                                         else servingUnit,
                            foodId = foodId
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MiOrange),
            enabled = addEnabled
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
