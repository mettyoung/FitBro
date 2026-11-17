package com.mettyoung.fitbro.data.cache

import com.mettyoung.fitbro.data.food.FoodDatabase
import com.mettyoung.fitbro.data.model.WeekStart
import com.mettyoung.fitbro.data.model.WorkoutType

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

    /** First day of the week for Trends / Cardio; defaults to Monday. */
    fun getWeekStart(): WeekStart
    fun setWeekStart(weekStart: WeekStart)

    /** Show avg carbs / fat rows in the week-over-week card (hidden by default). */
    fun getShowCarbsFatComparison(): Boolean
    fun setShowCarbsFatComparison(show: Boolean)

    /** Workout types hidden from the Cardio tab; defaults to [WorkoutType.DEFAULT_HIDDEN] until set. */
    fun getHiddenWorkoutTypes(): Set<WorkoutType>
    fun setHiddenWorkoutTypes(types: Set<WorkoutType>)
}

expect fun createUserSettingsDataSource(): UserSettingsDataSource
