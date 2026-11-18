package com.mettyoung.fitbro.data.cache

import com.mettyoung.fitbro.data.food.FoodDatabase
import com.mettyoung.fitbro.data.model.WeekStart
import com.mettyoung.fitbro.data.model.WorkoutType
import com.mettyoung.fitbro.data.sync.SyncSettings
import com.russhwolf.settings.Settings

class UserSettingsDataSourceImpl(private val settings: Settings) : UserSettingsDataSource, SyncSettings {
    companion object {
        private const val PROTEIN_GOAL_KEY = "macro_goal_protein_g"
        private const val CARBS_GOAL_KEY = "macro_goal_carbs_g"
        private const val FAT_GOAL_KEY = "macro_goal_fat_g"
        private const val CALORIE_GOAL_KEY = "macro_goal_calorie_kcal"
        private const val FOOD_DATABASE_KEY = "food_database"
        private const val WEEK_START_KEY = "week_start"
        private const val RESISTANCE_TARGET_KEY = "weekly_resistance_target"
        private const val CARDIO_TARGET_KEY = "weekly_cardio_minutes_target"
        private const val HIDDEN_WORKOUT_TYPES_KEY = "hidden_workout_types"
        private const val EXCLUDED_RESISTANCE_SOURCES_KEY = "resistance_excluded_sources"
        private const val THEME_MODE_KEY = "theme_mode"
        private const val SYNC_URL_KEY = "sync_url"
        private const val SYNC_TOKEN_KEY = "sync_token"
        private const val SYNC_CURSOR_KEY = "sync_cursor"
        private const val LAST_SYNCED_AT_KEY = "sync_last_synced_at"
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
        return FoodDatabase.entries.firstOrNull { it.name == stored } ?: FoodDatabase.FATSECRET
    }

    override fun setFoodDatabase(db: FoodDatabase) {
        settings.putString(FOOD_DATABASE_KEY, db.name)
    }

    override fun getWeekStart(): WeekStart = WeekStart.parse(settings.getStringOrNull(WEEK_START_KEY))

    override fun setWeekStart(weekStart: WeekStart) {
        settings.putString(WEEK_START_KEY, weekStart.name)
    }


    override fun getWeeklyResistanceTarget(): Int = settings.getInt(RESISTANCE_TARGET_KEY, 3)

    override fun setWeeklyResistanceTarget(sessions: Int) {
        settings.putInt(RESISTANCE_TARGET_KEY, sessions)
    }

    override fun getWeeklyCardioMinutesTarget(): Int = settings.getInt(CARDIO_TARGET_KEY, 150)

    override fun setWeeklyCardioMinutesTarget(minutes: Int) {
        settings.putInt(CARDIO_TARGET_KEY, minutes)
    }

    override fun getHiddenWorkoutTypes(): Set<WorkoutType> =
        WorkoutType.parseHidden(settings.getStringOrNull(HIDDEN_WORKOUT_TYPES_KEY))

    override fun setHiddenWorkoutTypes(types: Set<WorkoutType>) {
        settings.putString(HIDDEN_WORKOUT_TYPES_KEY, WorkoutType.serializeHidden(types))
    }

    override fun getThemeMode(): com.mettyoung.fitbro.ui.ThemeMode =
        settings.getStringOrNull(THEME_MODE_KEY)
            ?.let { name -> com.mettyoung.fitbro.ui.ThemeMode.entries.firstOrNull { it.name == name } }
            ?: com.mettyoung.fitbro.ui.ThemeMode.SYSTEM

    override fun setThemeMode(mode: com.mettyoung.fitbro.ui.ThemeMode) {
        settings.putString(THEME_MODE_KEY, mode.name)
    }

    override fun getExcludedResistanceSources(): Set<String> =
        settings.getStringOrNull(EXCLUDED_RESISTANCE_SOURCES_KEY)
            ?.split('\n')?.filter { it.isNotBlank() }?.toSet() ?: emptySet()

    override fun setExcludedResistanceSources(sources: Set<String>) {
        settings.putString(EXCLUDED_RESISTANCE_SOURCES_KEY, sources.sorted().joinToString("\n"))
    }

    override fun getSyncUrl(): String = settings.getString(SYNC_URL_KEY, "")

    override fun setSyncUrl(url: String) {
        settings.putString(SYNC_URL_KEY, url.trim().trimEnd('/'))
    }

    override fun getSyncToken(): String = settings.getString(SYNC_TOKEN_KEY, "")

    override fun setSyncToken(token: String) {
        settings.putString(SYNC_TOKEN_KEY, token.trim())
    }

    override fun getSyncCursor(): Long? = settings.getLongOrNull(SYNC_CURSOR_KEY)

    override fun setSyncCursor(cursor: Long?) {
        if (cursor == null) settings.remove(SYNC_CURSOR_KEY) else settings.putLong(SYNC_CURSOR_KEY, cursor)
    }

    override fun getLastSyncedAt(): Long? = settings.getLongOrNull(LAST_SYNCED_AT_KEY)

    override fun setLastSyncedAt(epochMs: Long) {
        settings.putLong(LAST_SYNCED_AT_KEY, epochMs)
    }
}
