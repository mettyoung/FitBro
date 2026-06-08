package com.mettyoung.fitbro.data.repository

import com.mettyoung.fitbro.data.model.CustomMeal
import com.mettyoung.fitbro.data.model.CustomMealItem
import kotlinx.coroutines.flow.Flow

interface CustomMealRepository {
    fun getAllCustomMeals(): Flow<List<CustomMeal>>
    suspend fun createCustomMeal(name: String, items: List<CustomMealItem>): Long
    suspend fun renameCustomMeal(id: Long, name: String)
    suspend fun deleteCustomMeal(id: Long)
}
