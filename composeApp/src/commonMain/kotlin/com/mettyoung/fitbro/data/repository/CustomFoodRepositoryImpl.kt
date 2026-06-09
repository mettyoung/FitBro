package com.mettyoung.fitbro.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.mettyoung.fitbro.data.db.FitBroDatabase
import com.mettyoung.fitbro.data.model.CustomFood
import com.mettyoung.fitbro.util.todayString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CustomFoodRepositoryImpl(private val database: FitBroDatabase) : CustomFoodRepository {

    override fun getAllCustomFoods(): Flow<List<CustomFood>> =
        database.customFoodQueries.getAllCustomFoods()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun searchCustomFoods(query: String): List<CustomFood> =
        withContext(Dispatchers.Default) {
            database.customFoodQueries.searchCustomFoods(query, query)
                .executeAsList()
                .map { it.toDomain() }
        }

    override suspend fun getCustomFood(id: Long): CustomFood? =
        withContext(Dispatchers.Default) {
            database.customFoodQueries.getCustomFoodById(id)
                .executeAsOneOrNull()
                ?.toDomain()
        }

    override suspend fun createCustomFood(food: CustomFood): Long =
        withContext(Dispatchers.Default) {
            database.customFoodQueries.transactionWithResult {
                database.customFoodQueries.insertCustomFood(
                    name = food.name,
                    brandName = food.brandName,
                    calories = food.calories,
                    proteinG = food.proteinG,
                    carbG = food.carbG,
                    fatG = food.fatG,
                    servingSizeG = food.servingSizeG,
                    createdAt = food.createdAt.ifBlank { todayString() }
                )
                database.customFoodQueries.lastInsertRowId().executeAsOne()
            }
        }

    override suspend fun updateCustomFood(food: CustomFood): Unit = withContext(Dispatchers.Default) {
        database.customFoodQueries.updateCustomFood(
            name = food.name,
            brandName = food.brandName,
            calories = food.calories,
            proteinG = food.proteinG,
            carbG = food.carbG,
            fatG = food.fatG,
            servingSizeG = food.servingSizeG,
            id = food.id
        )
    }

    override suspend fun deleteCustomFood(id: Long): Unit = withContext(Dispatchers.Default) {
        database.customFoodQueries.deleteCustomFood(id)
    }
}

private fun com.mettyoung.fitbro.data.db.CustomFood.toDomain() = CustomFood(
    id = id,
    name = name,
    brandName = brandName,
    calories = calories,
    proteinG = proteinG,
    carbG = carbG,
    fatG = fatG,
    servingSizeG = servingSizeG,
    createdAt = createdAt
)
