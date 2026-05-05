package com.mettyoung.fitbro.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ActivityBurn(
    val date: String,
    val neat: Double,
    val eat: Double
)
