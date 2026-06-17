package com.mettyoung.fitbro.data.model

data class MacroGoalProfile(
    val id: Long = 0,
    val name: String,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val caloriesKcal: Double
)
