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

    /**
     * Cross-meal drag commit. Moves [movedId] into [targetMeal] and rewrites sortOrder for
     * both meals to match the supplied id orders. [targetOrderedIds] is the target meal's full
     * order including [movedId]; [sourceOrderedIds] is the source meal's order excluding it.
     * For an intra-meal move pass the same value as source/target meal with [sourceOrderedIds] empty.
     */
    suspend fun moveEntryToPosition(
        date: String,
        movedId: Long,
        targetMeal: String,
        targetOrderedIds: List<Long>,
        sourceMeal: String,
        sourceOrderedIds: List<Long>
    )
    fun getDailyTotals(date: String): Flow<DailyMacroTotals>
    fun getDailyTotalsForRange(startDate: String, endDate: String): Flow<List<DailyMacroTotals>>
}
