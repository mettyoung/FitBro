package com.mettyoung.fitbro.data.repository

import com.mettyoung.fitbro.data.model.MacroGoalProfile
import kotlinx.coroutines.flow.Flow

interface MacroGoalRepository {
    fun getAllProfiles(): Flow<List<MacroGoalProfile>>
    suspend fun addProfile(name: String, protein: Double, carbs: Double, fat: Double, calories: Double): Long
    suspend fun updateProfile(id: Long, name: String, protein: Double, carbs: Double, fat: Double, calories: Double)
    suspend fun deleteProfile(id: Long)
    suspend fun getMappingForWeekday(weekday: Int): Long?
    suspend fun setMappingForWeekday(weekday: Int, profileId: Long)
    suspend fun getActiveProfileForDate(date: String): MacroGoalProfile
}
