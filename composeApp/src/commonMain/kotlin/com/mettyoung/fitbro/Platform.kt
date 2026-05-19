package com.mettyoung.fitbro

interface Platform {
    val name: String
    val healthNutritionSourceName: String
}

expect fun getPlatform(): Platform
