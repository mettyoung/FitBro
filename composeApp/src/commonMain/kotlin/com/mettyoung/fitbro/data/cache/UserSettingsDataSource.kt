package com.mettyoung.fitbro.data.cache

import com.mettyoung.fitbro.data.food.FoodDatabase
import com.mettyoung.fitbro.data.model.MacroDataSource

interface UserSettingsDataSource {
    fun getProteinGoalG(): Double
    fun setProteinGoalG(grams: Double)

    fun getCarbsGoalG(): Double
    fun setCarbsGoalG(grams: Double)

    fun getFatGoalG(): Double
    fun setFatGoalG(grams: Double)

    fun getCalorieGoalKcal(): Double
    fun setCalorieGoalKcal(kcal: Double)

    fun getFoodDatabase(): FoodDatabase
    fun setFoodDatabase(db: FoodDatabase)

    fun getMacroDataSourceForDate(date: String): MacroDataSource
    fun setMacroDataSourceForDate(date: String, source: MacroDataSource)
}

expect fun createUserSettingsDataSource(): UserSettingsDataSource
