package com.mettyoung.fitbro.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DailyIntake(
    val date: String,
    val totalCalories: Double
)
