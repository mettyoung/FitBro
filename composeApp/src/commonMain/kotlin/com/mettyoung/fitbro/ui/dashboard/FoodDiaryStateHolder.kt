package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.health.HealthDataSource
import com.mettyoung.fitbro.data.model.DailyMacroTotals
import com.mettyoung.fitbro.data.model.FoodDiaryEntry
import com.mettyoung.fitbro.data.model.MealType
import com.mettyoung.fitbro.data.repository.FoodDiaryRepository
import com.mettyoung.fitbro.util.minusDays
import com.mettyoung.fitbro.util.plusDays
import com.mettyoung.fitbro.util.todayString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FoodDiaryStateHolder(
    private val repository: FoodDiaryRepository,
    private val healthDataSource: HealthDataSource,
    private val scope: CoroutineScope
) {
    private val _selectedDate = MutableStateFlow(todayString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val weeklyTotals: StateFlow<List<DailyMacroTotals>> = _selectedDate
        .flatMapLatest { date ->
            val startDate = date.minusDays(6)
            repository.getDailyTotalsForRange(startDate, date)
                .map { rows ->
                    val byDate = rows.associateBy { it.date }
                    (0..6).map { i ->
                        val d = startDate.plusDays(i)
                        byDate[d] ?: DailyMacroTotals.empty(d)
                    }
                }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val state: StateFlow<FoodDiaryState> = _selectedDate
        .flatMapLatest { date ->
            combine(
                repository.getEntriesForDate(date),
                repository.getDailyTotals(date)
            ) { entries, totals ->
                FoodDiaryState(
                    entriesByMeal = MealType.ordered.associateWith { mealType ->
                        entries.filter { it.mealType == mealType }
                    },
                    dailyTotals = totals,
                    isLoading = false,
                    error = null
                )
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FoodDiaryState.initial()
        )

    fun setDate(date: String) {
        _selectedDate.value = date
    }

    fun addEntry(entry: FoodDiaryEntry) {
        scope.launch {
            val id = repository.addEntry(entry)
            syncToHealthConnect(entry.copy(id = id))
        }
    }

    fun updateEntry(entry: FoodDiaryEntry) {
        scope.launch {
            repository.updateEntry(entry)
            syncToHealthConnect(entry)
        }
    }

    fun deleteEntry(id: Long) {
        scope.launch {
            repository.deleteEntry(id)
        }
    }

    private fun syncToHealthConnect(entry: FoodDiaryEntry) {
        scope.launch {
            try {
                healthDataSource.writeNutritionRecord(entry)
            } catch (e: Exception) {
                println("FoodDiary: HealthConnect sync failed for entry ${entry.id}: ${e.message}")
            }
        }
    }
}
