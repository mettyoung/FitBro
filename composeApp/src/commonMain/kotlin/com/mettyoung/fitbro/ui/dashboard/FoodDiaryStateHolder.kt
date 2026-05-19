package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.getPlatform
import com.mettyoung.fitbro.data.cache.UserSettingsDataSource
import com.mettyoung.fitbro.data.health.HealthResult
import com.mettyoung.fitbro.data.health.HealthDataSource
import com.mettyoung.fitbro.data.model.DailyMacroTotals
import com.mettyoung.fitbro.data.model.DailyIntake
import com.mettyoung.fitbro.data.model.FoodDiaryEntry
import com.mettyoung.fitbro.data.model.MacroDataSource
import com.mettyoung.fitbro.data.model.MealType
import com.mettyoung.fitbro.data.repository.FoodDiaryRepository
import com.mettyoung.fitbro.util.minusDays
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FoodDiaryStateHolder(
    private val repository: FoodDiaryRepository,
    private val healthDataSource: HealthDataSource,
    private val userSettingsDataSource: UserSettingsDataSource,
    private val scope: CoroutineScope
) {
    private val _selectedDate = MutableStateFlow(todayString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()
    private val macroSourceRevision = MutableStateFlow(0)

    val weeklyTotals: StateFlow<List<DailyMacroTotals>> = _selectedDate
        .flatMapLatest { date ->
            val startDate = date.minusDays(6)
            combine(
                repository.getDailyTotalsForRange(startDate, date),
                flow { emit(readHealthMacroTotals(startDate, date).totals) },
                macroSourceRevision
            ) { localRows, healthRows, _ ->
                val localByDate = localRows.associateBy { it.date }
                val healthByDate = healthRows.associateBy { it.date }
                (0..6).map { i ->
                    val d = startDate.plusDays(i)
                    when (userSettingsDataSource.getMacroDataSourceForDate(d)) {
                        MacroDataSource.HEALTH_CONNECT -> healthByDate[d] ?: DailyMacroTotals.empty(d)
                        MacroDataSource.FOOD_DIARY -> localByDate[d] ?: DailyMacroTotals.empty(d)
                    }
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
                repository.getDailyTotals(date),
                flow { emit(readHealthMacroTotals(date, date)) },
                macroSourceRevision
            ) { entries, localTotals, healthTotalsResult, _ ->
                val macroDataSource = userSettingsDataSource.getMacroDataSourceForDate(date)
                val healthTotals = healthTotalsResult.totals.firstOrNull { it.date == date }
                    ?: DailyMacroTotals.empty(date)
                FoodDiaryState(
                    entriesByMeal = MealType.ordered.associateWith { mealType ->
                        entries.filter { it.mealType == mealType }
                    },
                    dailyTotals = when (macroDataSource) {
                        MacroDataSource.HEALTH_CONNECT -> healthTotals
                        MacroDataSource.FOOD_DIARY -> localTotals
                    },
                    macroDataSource = macroDataSource,
                    isLoading = false,
                    error = if (macroDataSource == MacroDataSource.HEALTH_CONNECT) {
                        healthTotalsResult.error
                    } else {
                        null
                    }
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

    fun setMacroDataSourceForSelectedDate(source: MacroDataSource) {
        val date = _selectedDate.value
        userSettingsDataSource.setMacroDataSourceForDate(date, source)
        macroSourceRevision.value += 1
    }

    fun addEntry(entry: FoodDiaryEntry): Job {
        return scope.launch {
            val id = repository.addEntry(entry)
            syncToHealthConnect(entry.copy(id = id))
        }
    }

    fun updateEntry(entry: FoodDiaryEntry): Job {
        return scope.launch {
            repository.updateEntry(entry)
            syncToHealthConnect(entry)
        }
    }

    fun deleteEntry(id: Long): Job {
        return scope.launch {
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

    private suspend fun readHealthMacroTotals(startDate: String, endDate: String): HealthMacroTotalsResult {
        return when (val result = healthDataSource.readDailyIntake(startDate, endDate)) {
            is HealthResult.Success -> HealthMacroTotalsResult(
                totals = result.value.map { it.toMacroTotals() },
                error = null
            )
            is HealthResult.Failure -> HealthMacroTotalsResult(
                totals = emptyList(),
                error = "${getPlatform().healthNutritionSourceName} nutrition is unavailable for this day."
            )
        }
    }
}

private data class HealthMacroTotalsResult(
    val totals: List<DailyMacroTotals>,
    val error: String?
)

private fun DailyIntake.toMacroTotals() = DailyMacroTotals(
    date = date,
    calories = totalCalories,
    proteinG = proteinG,
    carbG = carbG,
    fatG = fatG
)
