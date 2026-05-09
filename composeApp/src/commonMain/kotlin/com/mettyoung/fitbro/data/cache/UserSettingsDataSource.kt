package com.mettyoung.fitbro.data.cache

interface UserSettingsDataSource {
    fun getProteinGoalG(): Double
    fun setProteinGoalG(grams: Double)

    fun getCarbsGoalG(): Double
    fun setCarbsGoalG(grams: Double)

    fun getFatGoalG(): Double
    fun setFatGoalG(grams: Double)

    fun getCalorieGoalKcal(): Double
    fun setCalorieGoalKcal(kcal: Double)
}

expect fun createUserSettingsDataSource(): UserSettingsDataSource
