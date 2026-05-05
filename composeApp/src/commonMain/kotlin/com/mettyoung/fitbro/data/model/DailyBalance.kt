package com.mettyoung.fitbro.data.model

data class DailyBalance(
    val date: String,
    val intake: Double,
    val burn: Double,
    val balance: Double
)
