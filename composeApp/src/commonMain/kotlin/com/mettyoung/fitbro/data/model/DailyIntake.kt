package com.mettyoung.fitbro.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DailyIntake(
    val date: String,
    val totalCalories: Double,
    val proteinG: Double = 0.0,
    val carbG: Double = 0.0,
    val fatG: Double = 0.0
)
