package com.mettyoung.fitbro.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.mettyoung.fitbro.data.db.FitBroDatabase
import com.mettyoung.fitbro.data.model.CustomMeal
import com.mettyoung.fitbro.data.model.CustomMealItem
import com.mettyoung.fitbro.util.todayString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class CustomMealRepositoryImpl(private val database: FitBroDatabase) : CustomMealRepository {

    override fun getAllCustomMeals(): Flow<List<CustomMeal>> =
        database.customMealQueries.getAllCustomMeals()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { meals ->
                meals.map { meal ->
                    val items = database.customMealQueries
                        .getItemsForMeal(meal.id)
                        .executeAsList()
                        .map { it.toDomain() }
                    meal.toDomain(items)
                }
            }

    override suspend fun createCustomMeal(
        name: String,
        items: List<CustomMealItem>
    ): Long = withContext(Dispatchers.Default) {
        database.customMealQueries.transactionWithResult {
            database.customMealQueries.insertCustomMeal(name = name, createdAt = todayString())
            val mealId = database.customMealQueries.lastInsertRowId().executeAsOne()
            items.forEachIndexed { index, item ->
                database.customMealQueries.insertCustomMealItem(
                    customMealId = mealId,
                    foodName = item.foodName,
                    brandName = item.brandName,
                    calories = item.calories,
                    proteinG = item.proteinG,
                    carbG = item.carbG,
                    fatG = item.fatG,
                    servingSizeG = item.servingSizeG,
                    servingUnit = item.servingUnit,
                    food_id = item.foodId,
                    sortOrder = index.toLong()
                )
            }
            mealId
        }
    }

    override suspend fun renameCustomMeal(id: Long, name: String): Unit =
        withContext(Dispatchers.Default) {
            database.customMealQueries.updateCustomMealName(name = name, id = id)
        }

    override suspend fun deleteCustomMeal(id: Long): Unit = withContext(Dispatchers.Default) {
        database.customMealQueries.transaction {
            database.customMealQueries.deleteItemsForMeal(id)
            database.customMealQueries.deleteCustomMeal(id)
        }
    }
}

private fun com.mettyoung.fitbro.data.db.CustomMeal.toDomain(items: List<CustomMealItem>) = CustomMeal(
    id = id,
    name = name,
    createdAt = createdAt,
    items = items
)

private fun com.mettyoung.fitbro.data.db.CustomMealItem.toDomain() = CustomMealItem(
    id = id,
    customMealId = customMealId,
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
