package com.mettyoung.fitbro.data.model

data class FoodDiaryEntry(
    val id: Long = 0,
    val date: String,
    val mealType: String,
    val foodName: String,
    val brandName: String? = null,
    val calories: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val servingSizeG: Double,
    val servingUnit: String,
    val foodId: String? = null,
    val sortOrder: Long = 0,
    val servingId: String? = null,
    val servingQuantity: Double? = null
)

object MealType {
    const val BREAKFAST = "BREAKFAST"
    const val LUNCH = "LUNCH"
    const val DINNER = "DINNER"
    const val SNACKS = "SNACKS"
    val ordered = listOf(BREAKFAST, LUNCH, DINNER, SNACKS)
}

object ServingUnit {
    const val GRAMS = "g"
    const val OZ = "oz"
    const val SERVING = "serving"
    const val OZ_TO_GRAMS = 28.3495
}
