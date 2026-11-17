package com.mettyoung.fitbro.data.health

import com.mettyoung.fitbro.data.model.ActivityBurn
import com.mettyoung.fitbro.data.model.CardioSession
import com.mettyoung.fitbro.data.model.FoodDiaryEntry
import com.mettyoung.fitbro.data.model.Metabolism

interface HealthDataSource {
    suspend fun readActivityData(startDate: String, endDate: String): HealthResult<List<ActivityBurn>>
    suspend fun readBasalMetabolicRate(startDate: String, endDate: String): HealthResult<List<Metabolism>>
    suspend fun writeNutritionRecord(entry: FoodDiaryEntry)
    /** Exercise sessions from any app (e.g. Mi Fitness), keyed to the local date they started. */
    suspend fun readExerciseSessions(startDate: String, endDate: String): HealthResult<List<CardioSession>>
}

expect fun createHealthDataSource(): HealthDataSource
