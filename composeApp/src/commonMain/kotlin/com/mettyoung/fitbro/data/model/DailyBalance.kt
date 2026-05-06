package com.mettyoung.fitbro.data.model

data class DailyBalance(
    val date: String,
    val intake: Double,
    val burn: Double,
    val balance: Double,
    val bmr: Double = 0.0,
    val tef: Double = 0.0,
    val neat: Double = 0.0,
    val eat: Double = 0.0
)
