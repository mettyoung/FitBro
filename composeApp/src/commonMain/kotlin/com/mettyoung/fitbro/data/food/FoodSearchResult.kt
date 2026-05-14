package com.mettyoung.fitbro.data.food

data class FoodSearchResult(
    val name: String,
    val brand: String?,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbPer100g: Double,
    val fatPer100g: Double,
    val servingSizeG: Double?,
    val servingDescription: String?,
    val source: String?
)
