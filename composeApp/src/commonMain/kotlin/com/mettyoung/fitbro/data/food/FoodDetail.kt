package com.mettyoung.fitbro.data.food

data class ServingOption(
    val servingId: String?,
    val description: String,
    val metricAmount: Double?,
    val metricUnit: String?,
    val calories: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double
)

data class FoodDetail(
    val foodId: String,
    val name: String,
    val brand: String?,
    val servings: List<ServingOption>,
    val source: String?
)
