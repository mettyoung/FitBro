package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.health.HealthDataSource
import com.mettyoung.fitbro.data.model.DailyMacroTotals
import com.mettyoung.fitbro.data.model.FoodDiaryEntry
import com.mettyoung.fitbro.data.model.MealType
import com.mettyoung.fitbro.data.repository.FoodDiaryRepository
import com.mettyoung.fitbro.util.minusDays
import com.mettyoung.fitbro.util.newUuid
import com.mettyoung.fitbro.util.plusDays
import com.mettyoung.fitbro.util.todayString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FoodDiaryStateHolder(
    private val repository: FoodDiaryRepository,
    private val healthDataSource: HealthDataSource,
    private val scope: CoroutineScope,
    /** Called after every local diary mutation so it can be pushed to the cloud. */
    private val onDiaryChanged: () -> Unit = {}
) {
    private val _selectedDate = MutableStateFlow(todayString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

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
                    isLoading = false
                )
            }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FoodDiaryState.initial()
        )

    val recentFoods: StateFlow<List<FoodDiaryEntry>> = repository.getRecentFoods(20)
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun setDate(date: String) {
        _selectedDate.value = date
    }

    fun copyMeal(sourceDate: String, mealType: String, targetDate: String): Job {
        return scope.launch {
            repository.copyMealToDate(sourceDate, mealType, targetDate)
            onDiaryChanged()
        }
    }

    fun addEntry(entry: FoodDiaryEntry): Job {
        return scope.launch {
            val withUuid = if (entry.uuid.isBlank()) entry.copy(uuid = newUuid()) else entry
            val id = repository.addEntry(withUuid)
            syncToHealthConnect(withUuid.copy(id = id))
            onDiaryChanged()
        }
    }

    fun updateEntry(entry: FoodDiaryEntry): Job {
        return scope.launch {
            repository.updateEntry(entry)
            syncToHealthConnect(entry)
            onDiaryChanged()
        }
    }

    fun deleteEntry(id: Long): Job {
        return scope.launch {
            repository.deleteEntry(id)
            onDiaryChanged()
        }
    }

    fun reorderMeal(date: String, mealType: String, orderedIds: List<Long>): Job {
        return scope.launch {
            repository.reorderMeal(date, mealType, orderedIds)
            onDiaryChanged()
        }
    }

    fun moveEntryToPosition(
        date: String,
        movedId: Long,
        targetMeal: String,
        targetOrderedIds: List<Long>,
        sourceMeal: String,
        sourceOrderedIds: List<Long>
    ): Job {
        return scope.launch {
            repository.moveEntryToPosition(
                date, movedId, targetMeal, targetOrderedIds, sourceMeal, sourceOrderedIds
            )
            onDiaryChanged()
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
