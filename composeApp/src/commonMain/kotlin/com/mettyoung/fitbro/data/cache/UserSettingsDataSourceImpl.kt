package com.mettyoung.fitbro.data.cache

import com.mettyoung.fitbro.data.food.FoodDatabase
import com.russhwolf.settings.Settings

class UserSettingsDataSourceImpl(private val settings: Settings) : UserSettingsDataSource {
    companion object {
        private const val PROTEIN_GOAL_KEY = "macro_goal_protein_g"
        private const val CARBS_GOAL_KEY = "macro_goal_carbs_g"
        private const val FAT_GOAL_KEY = "macro_goal_fat_g"
        private const val CALORIE_GOAL_KEY = "macro_goal_calorie_kcal"
        private const val FOOD_DATABASE_KEY = "food_database"
        private const val DEFAULT_PROTEIN_GOAL = 150.0
        private const val DEFAULT_CARBS_GOAL = 200.0
        private const val DEFAULT_FAT_GOAL = 65.0
        private const val DEFAULT_CALORIE_GOAL = 2000.0
    }

    override fun getProteinGoalG(): Double =
        settings.getDouble(PROTEIN_GOAL_KEY, DEFAULT_PROTEIN_GOAL)

    override fun setProteinGoalG(grams: Double) {
        settings.putDouble(PROTEIN_GOAL_KEY, grams)
    }

    override fun getCarbsGoalG(): Double =
        settings.getDouble(CARBS_GOAL_KEY, DEFAULT_CARBS_GOAL)

    override fun setCarbsGoalG(grams: Double) {
        settings.putDouble(CARBS_GOAL_KEY, grams)
    }

    override fun getFatGoalG(): Double =
        settings.getDouble(FAT_GOAL_KEY, DEFAULT_FAT_GOAL)

    override fun setFatGoalG(grams: Double) {
        settings.putDouble(FAT_GOAL_KEY, grams)
    }

    override fun getCalorieGoalKcal(): Double =
        settings.getDouble(CALORIE_GOAL_KEY, DEFAULT_CALORIE_GOAL)

    override fun setCalorieGoalKcal(kcal: Double) {
        settings.putDouble(CALORIE_GOAL_KEY, kcal)
    }

    override fun getFoodDatabase(): FoodDatabase {
        val stored = settings.getStringOrNull(FOOD_DATABASE_KEY)
        return FoodDatabase.entries.firstOrNull { it.name == stored } ?: FoodDatabase.OPEN_FOOD_FACTS
    }

    override fun setFoodDatabase(db: FoodDatabase) {
        settings.putString(FOOD_DATABASE_KEY, db.name)
    }
}
