package com.mettyoung.fitbro.data.model

/**
 * A user-defined food: calories + macros for a fixed serving size in grams.
 * Stored locally and exposed as a [com.mettyoung.fitbro.data.food.FoodDataSource]
 * so it can be searched and logged like any FatSecret food.
 */
data class CustomFood(
    val id: Long = 0,
    val name: String,
    val brandName: String? = null,
    val calories: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val servingSizeG: Double,
    val createdAt: String = ""
)
