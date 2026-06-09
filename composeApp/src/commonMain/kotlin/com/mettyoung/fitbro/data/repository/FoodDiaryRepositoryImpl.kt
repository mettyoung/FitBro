package com.mettyoung.fitbro.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.mettyoung.fitbro.data.db.FitBroDatabase
import com.mettyoung.fitbro.data.model.DailyMacroTotals
import com.mettyoung.fitbro.data.model.FoodDiaryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class FoodDiaryRepositoryImpl(private val database: FitBroDatabase) : FoodDiaryRepository {

    override fun getEntriesForDate(date: String): Flow<List<FoodDiaryEntry>> =
        database.foodDiaryQueries.getEntriesByDate(date)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun addEntry(entry: FoodDiaryEntry): Long = withContext(Dispatchers.Default) {
        database.foodDiaryQueries.transactionWithResult {
            val nextSortOrder = database.foodDiaryQueries
                .maxSortOrderForDateMeal(entry.date, entry.mealType)
                .executeAsOne() + 1
            database.foodDiaryQueries.insertEntry(
                date = entry.date,
                mealType = entry.mealType,
                foodName = entry.foodName,
                brandName = entry.brandName,
                calories = entry.calories,
                proteinG = entry.proteinG,
                carbG = entry.carbG,
                fatG = entry.fatG,
                servingSizeG = entry.servingSizeG,
                servingUnit = entry.servingUnit,
                food_id = entry.foodId,
                sortOrder = nextSortOrder
            )
            database.foodDiaryQueries.lastInsertRowId().executeAsOne()
        }
    }

    override suspend fun updateEntry(entry: FoodDiaryEntry): Unit = withContext(Dispatchers.Default) {
        database.foodDiaryQueries.updateEntry(
            foodName = entry.foodName,
            brandName = entry.brandName,
            calories = entry.calories,
            proteinG = entry.proteinG,
            carbG = entry.carbG,
            fatG = entry.fatG,
            servingSizeG = entry.servingSizeG,
            servingUnit = entry.servingUnit,
            food_id = entry.foodId,
            id = entry.id
        )
    }

    override suspend fun deleteEntry(id: Long): Unit = withContext(Dispatchers.Default) {
        database.foodDiaryQueries.deleteEntry(id)
    }

    override suspend fun reorderMeal(
        date: String,
        mealType: String,
        orderedIds: List<Long>
    ): Unit = withContext(Dispatchers.Default) {
        database.foodDiaryQueries.transaction {
            orderedIds.forEachIndexed { index, id ->
                database.foodDiaryQueries.updateSortOrder(sortOrder = index.toLong(), id = id)
            }
        }
    }

    override suspend fun moveEntryToPosition(
        date: String,
        movedId: Long,
        targetMeal: String,
        targetOrderedIds: List<Long>,
        sourceMeal: String,
        sourceOrderedIds: List<Long>
    ): Unit = withContext(Dispatchers.Default) {
        database.foodDiaryQueries.transaction {
            targetOrderedIds.forEachIndexed { index, id ->
                if (id == movedId) {
                    database.foodDiaryQueries.moveEntryToMeal(
                        mealType = targetMeal,
                        sortOrder = index.toLong(),
                        id = id
                    )
                } else {
                    database.foodDiaryQueries.updateSortOrder(sortOrder = index.toLong(), id = id)
                }
            }
            sourceOrderedIds.forEachIndexed { index, id ->
                database.foodDiaryQueries.updateSortOrder(sortOrder = index.toLong(), id = id)
            }
        }
    }

    override fun getRecentFoods(limit: Int): Flow<List<FoodDiaryEntry>> =
        database.foodDiaryQueries.recentFoods(limit.toLong())
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain().copy(id = 0, sortOrder = 0) } }

    override fun getDailyTotals(date: String): Flow<DailyMacroTotals> =
        database.foodDiaryQueries.getDailyTotals(date)
            .asFlow()
            .mapToOne(Dispatchers.Default)
            .map { row ->
                DailyMacroTotals(
                    date = date,
                    calories = row.calories ?: 0.0,
                    proteinG = row.proteinG ?: 0.0,
                    carbG = row.carbG ?: 0.0,
                    fatG = row.fatG ?: 0.0
                )
            }

    override fun getDailyTotalsForRange(startDate: String, endDate: String): Flow<List<DailyMacroTotals>> =
        database.foodDiaryQueries.getDailyTotalsForDateRange(startDate, endDate)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows ->
                rows.map { row ->
                    DailyMacroTotals(
                        date = row.date,
                        calories = row.calories ?: 0.0,
                        proteinG = row.proteinG ?: 0.0,
                        carbG = row.carbG ?: 0.0,
                        fatG = row.fatG ?: 0.0
                    )
                }
            }
}

private fun com.mettyoung.fitbro.data.db.FoodDiaryEntry.toDomain() = FoodDiaryEntry(
    id = id,
    date = date,
    mealType = mealType,
    foodName = foodName,
    brandName = brandName,
    calories = calories,
    proteinG = proteinG,
    carbG = carbG,
    fatG = fatG,
    servingSizeG = servingSizeG,
    servingUnit = servingUnit,
    foodId = food_id,
    sortOrder = sortOrder
)
