package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.model.CustomFood
import com.mettyoung.fitbro.data.repository.CustomFoodRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomFoodStateHolder(
    private val repository: CustomFoodRepository,
    private val scope: CoroutineScope
) {
    val customFoods: StateFlow<List<CustomFood>> = repository.getAllCustomFoods()
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun create(food: CustomFood): Job = scope.launch {
        repository.createCustomFood(food)
    }

    fun update(food: CustomFood): Job = scope.launch {
        repository.updateCustomFood(food)
    }

    fun delete(id: Long): Job = scope.launch {
        repository.deleteCustomFood(id)
    }
}
