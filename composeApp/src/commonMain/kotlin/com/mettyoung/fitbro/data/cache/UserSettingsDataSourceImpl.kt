package com.mettyoung.fitbro.data.cache

import com.russhwolf.settings.Settings

class UserSettingsDataSourceImpl(private val settings: Settings) : UserSettingsDataSource {
    companion object {
        private const val PROTEIN_GOAL_KEY = "macro_goal_protein_g"
        private const val CARBS_GOAL_KEY = "macro_goal_carbs_g"
        private const val FAT_GOAL_KEY = "macro_goal_fat_g"
        private const val DEFAULT_PROTEIN_GOAL = 150.0
        private const val DEFAULT_CARBS_GOAL = 200.0
        private const val DEFAULT_FAT_GOAL = 65.0
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
}
