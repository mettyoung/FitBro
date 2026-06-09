package com.mettyoung.fitbro.data.repository

import com.mettyoung.fitbro.data.model.CustomFood
import kotlinx.coroutines.flow.Flow

interface CustomFoodRepository {
    fun getAllCustomFoods(): Flow<List<CustomFood>>
    suspend fun searchCustomFoods(query: String): List<CustomFood>
    suspend fun getCustomFood(id: Long): CustomFood?
    suspend fun createCustomFood(food: CustomFood): Long
    suspend fun updateCustomFood(food: CustomFood)
    suspend fun deleteCustomFood(id: Long)
}
