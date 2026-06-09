package com.mettyoung.fitbro.data.food

import com.mettyoung.fitbro.data.model.CustomFood
import com.mettyoung.fitbro.data.repository.CustomFoodRepository
import kotlin.math.roundToInt

/**
 * Exposes user-defined [CustomFood]s through the [FoodDataSource] contract so they
 * appear in the same search/log flow as FatSecret. Food ids are namespaced with
 * [ID_PREFIX] so [CompositeFoodDataSource] can route detail lookups back here.
 *
 * Each custom food has exactly one serving: the grams the user entered, with macros
 * as given. The metric amount lets the existing gram-proration UI scale it.
 */
class CustomFoodDataSource(
    private val repository: CustomFoodRepository
) : FoodDataSource {

    override val supportsBarcode = false
    override val supportsFoodDetail = true

    override suspend fun search(query: String): FoodResult<List<FoodSearchResult>> {
        val matches = repository.searchCustomFoods(query.trim())
        if (matches.isEmpty()) return FoodResult.Failure(FoodError.EmptyResults)
        return FoodResult.Success(matches.map { it.toSearchResult() })
    }

    override suspend fun getFoodDetail(foodId: String): FoodResult<FoodDetail> {
        val localId = foodId.removePrefix(ID_PREFIX).toLongOrNull()
            ?: return FoodResult.Failure(FoodError.EmptyResults)
        val food = repository.getCustomFood(localId)
            ?: return FoodResult.Failure(FoodError.EmptyResults)
        return FoodResult.Success(food.toFoodDetail())
    }

    companion object {
        const val ID_PREFIX = "custom:"
        fun owns(foodId: String): Boolean = foodId.startsWith(ID_PREFIX)
    }
}

internal fun CustomFood.toSearchResult(): FoodSearchResult {
    val cals = calories.roundToInt()
    val grams = servingSizeG.roundToInt()
    return FoodSearchResult(
        name = name,
        brand = brandName,
        foodId = "${CustomFoodDataSource.ID_PREFIX}$id",
        displayText = "${grams}g - ${cals}kcal (Custom)"
    )
}

internal fun CustomFood.toFoodDetail(): FoodDetail = FoodDetail(
    foodId = "${CustomFoodDataSource.ID_PREFIX}$id",
    name = name,
    brand = brandName,
    servings = listOf(
        ServingOption(
            servingId = id.toString(),
            description = "${servingSizeG.roundToInt()} g",
            metricAmount = servingSizeG,
            metricUnit = "g",
            calories = calories,
            proteinG = proteinG,
            carbG = carbG,
            fatG = fatG
        )
    ),
    source = "Custom"
)
