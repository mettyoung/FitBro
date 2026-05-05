package com.mettyoung.fitbro.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Metabolism(
    val date: String,
    val bmr: Double,
    val tef: Double
)
