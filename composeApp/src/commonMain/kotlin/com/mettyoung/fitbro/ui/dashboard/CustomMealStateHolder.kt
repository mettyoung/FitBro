package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.model.CustomMeal
import com.mettyoung.fitbro.data.model.CustomMealItem
import com.mettyoung.fitbro.data.repository.CustomMealRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CustomMealStateHolder(
    private val repository: CustomMealRepository,
    private val scope: CoroutineScope
) {
    val customMeals: StateFlow<List<CustomMeal>> = repository.getAllCustomMeals()
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun create(name: String, items: List<CustomMealItem>): Job = scope.launch {
        repository.createCustomMeal(name.trim(), items)
    }

    fun rename(id: Long, name: String): Job = scope.launch {
        repository.renameCustomMeal(id, name.trim())
    }

    fun delete(id: Long): Job = scope.launch {
        repository.deleteCustomMeal(id)
    }
}
