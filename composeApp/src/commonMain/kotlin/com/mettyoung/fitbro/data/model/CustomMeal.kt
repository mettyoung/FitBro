package com.mettyoung.fitbro.data.model

data class CustomMeal(
    val id: Long = 0,
    val name: String,
    val createdAt: String,
    val items: List<CustomMealItem> = emptyList()
)

data class CustomMealItem(
    val id: Long = 0,
    val customMealId: Long = 0,
    val foodName: String,
    val brandName: String? = null,
    val calories: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val servingSizeG: Double,
    val servingUnit: String,
    val foodId: String? = null,
    val sortOrder: Long = 0
)
