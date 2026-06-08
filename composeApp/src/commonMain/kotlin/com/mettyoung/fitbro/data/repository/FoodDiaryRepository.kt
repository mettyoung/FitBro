package com.mettyoung.fitbro.data.repository

import com.mettyoung.fitbro.data.model.DailyMacroTotals
import com.mettyoung.fitbro.data.model.FoodDiaryEntry
import kotlinx.coroutines.flow.Flow

interface FoodDiaryRepository {
    fun getEntriesForDate(date: String): Flow<List<FoodDiaryEntry>>
    suspend fun addEntry(entry: FoodDiaryEntry): Long
    suspend fun updateEntry(entry: FoodDiaryEntry)
    suspend fun deleteEntry(id: Long)
    suspend fun reorderMeal(date: String, mealType: String, orderedIds: List<Long>)
    fun getDailyTotals(date: String): Flow<DailyMacroTotals>
    fun getDailyTotalsForRange(startDate: String, endDate: String): Flow<List<DailyMacroTotals>>
}
