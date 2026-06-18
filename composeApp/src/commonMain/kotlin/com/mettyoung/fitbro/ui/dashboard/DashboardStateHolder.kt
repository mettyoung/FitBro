package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.cache.CacheDataSource
import com.mettyoung.fitbro.data.cache.CacheSource
import com.mettyoung.fitbro.data.cache.UserSettingsDataSource
import com.mettyoung.fitbro.data.health.HealthDataSource
import com.mettyoung.fitbro.data.health.HealthResult
import com.mettyoung.fitbro.data.model.ActivityBurn
import com.mettyoung.fitbro.data.model.DailyBalance
import com.mettyoung.fitbro.data.model.DailyIntake
import com.mettyoung.fitbro.data.model.DailyMacroTotals
import com.mettyoung.fitbro.data.model.MacroDataSource
import com.mettyoung.fitbro.data.model.Metabolism
import com.mettyoung.fitbro.data.repository.CalorieMathRepository
import com.mettyoung.fitbro.data.repository.CalorieResult
import com.mettyoung.fitbro.data.repository.FoodDiaryRepository
import com.mettyoung.fitbro.util.currentEpochMs
import com.mettyoung.fitbro.util.todayString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardStateHolder(
    private val healthDataSource: HealthDataSource,
    private val cacheDataSource: CacheDataSource,
    private val foodDiaryRepository: FoodDiaryRepository,
    private val userSettingsDataSource: UserSettingsDataSource,
    private val calorieMathRepository: CalorieMathRepository,
    private val scope: CoroutineScope,
    initialDateRange: DateRange
) {
    private val _state = MutableStateFlow(
        DashboardState(
            uiState = DashboardUiState.Loading,
            lastSyncTime = emptyMap(),
            errorMessage = null,
            selectedDateRange = initialDateRange
        )
    )
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _selectedDate = MutableStateFlow(todayString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun refresh() {
        scope.launch(Dispatchers.Default) {
            val range = _state.value.selectedDateRange
            _state.update { it.copy(uiState = DashboardUiState.Loading, errorMessage = null) }

            val intakeDeferred = async { healthDataSource.readDailyIntake(range.startDate, range.endDate) }
            val metabolismDeferred = async { healthDataSource.readBasalMetabolicRate(range.startDate, range.endDate) }
            val activityDeferred = async { healthDataSource.readActivityData(range.startDate, range.endDate) }
            val foodDiaryTotalsDeferred = async {
                foodDiaryRepository.getDailyTotalsForRange(range.startDate, range.endDate).first()
            }

            val intakeResult = intakeDeferred.await()
            val metabolismResult = metabolismDeferred.await()
            val activityResult = activityDeferred.await()
            val foodDiaryTotals = foodDiaryTotalsDeferred.await()

            val healthIntakeFailed = intakeResult is HealthResult.Failure
            val healthMetabolismFailed = metabolismResult is HealthResult.Failure
            val healthFailed = activityResult is HealthResult.Failure

            val now = currentEpochMs()
            val errors = mutableListOf<String>()

            val intakes: List<DailyIntake> = when (intakeResult) {
                is HealthResult.Success -> {
                    cacheDataSource.saveDailyIntake(range.startDate, range.endDate, intakeResult.value)
                    cacheDataSource.saveSyncTimestamp(CacheSource.HEALTH_INTAKE, now)
                    intakeResult.value
                }
                is HealthResult.Failure -> {
                    errors.add("Health intake: ${intakeResult.error.describe()}")
                    cacheDataSource.getDailyIntake(range.startDate, range.endDate) ?: emptyList()
                }
            }

            val metabolisms: List<Metabolism> = when (metabolismResult) {
                is HealthResult.Success -> {
                    val maxBmr = metabolismResult.value.maxOfOrNull { it.bmr }
                    if (maxBmr != null && maxBmr > 0) cacheDataSource.saveLatestBmr(maxBmr)
                    cacheDataSource.saveMetabolism(range.startDate, range.endDate, metabolismResult.value)
                    cacheDataSource.saveSyncTimestamp(CacheSource.HEALTH_METABOLISM, now)
                    metabolismResult.value
                }
                is HealthResult.Failure -> {
                    errors.add("BMR: ${metabolismResult.error.describe()}")
                    cacheDataSource.getMetabolism(range.startDate, range.endDate) ?: emptyList()
                }
            }

            val activities: List<ActivityBurn>? = when (activityResult) {
                is HealthResult.Success -> {
                    cacheDataSource.saveActivityBurn(range.startDate, range.endDate, activityResult.value)
                    cacheDataSource.saveSyncTimestamp(CacheSource.HEALTH_ACTIVITY, now)
                    activityResult.value
                }
                is HealthResult.Failure -> {
                    errors.add("Health Connect: ${activityResult.error.describe()}")
                    cacheDataSource.getActivityBurn(range.startDate, range.endDate)
                }
            }

            val effectiveIntakes = selectIntakesForPersistedSources(intakes, foodDiaryTotals)
            val balances = computeBalances(effectiveIntakes, metabolisms, activities)

            val updatedSyncTime = mapOf(
                CacheSource.HEALTH_INTAKE to cacheDataSource.getSyncTimestamp(CacheSource.HEALTH_INTAKE),
                CacheSource.HEALTH_METABOLISM to cacheDataSource.getSyncTimestamp(CacheSource.HEALTH_METABOLISM),
                CacheSource.HEALTH_ACTIVITY to cacheDataSource.getSyncTimestamp(CacheSource.HEALTH_ACTIVITY),
            )

            val warnings = buildWarnings(healthIntakeFailed, healthMetabolismFailed, healthFailed)
            val totalFailure = balances.isEmpty() && errors.isNotEmpty() &&
                updatedSyncTime.values.all { it == null }
            val newUiState = if (totalFailure) {
                DashboardUiState.Error(errors.joinToString("\n"))
            } else {
                DashboardUiState.Success(balances, warnings)
            }

            _state.update {
                it.copy(
                    uiState = newUiState,
                    lastSyncTime = updatedSyncTime,
                    errorMessage = if (errors.isNotEmpty()) errors.joinToString("\n") else null
                )
            }
        }
    }

    fun selectDateRange(dateRange: DateRange) {
        val rollingRange = dateRange.copy(endDate = todayString())
        userSettingsDataSource.setDashboardStartDate(rollingRange.startDate)
        _state.update { it.copy(selectedDateRange = rollingRange) }
        if (rollingRange.startDate == rollingRange.endDate) {
            _selectedDate.value = rollingRange.startDate
        }
        refresh()
    }

    private fun computeBalances(
        intakes: List<DailyIntake>,
        metabolisms: List<Metabolism>,
        activities: List<ActivityBurn>?
    ): List<DailyBalance> {
        val metabolismByDate = metabolisms.associateBy { it.date }
        val activityByDate = activities?.associateBy { it.date }
        val intakesByDate = intakes.associateBy { it.date }
        val fallbackBmr = metabolisms.maxByOrNull { it.date }?.bmr
            ?: cacheDataSource.getLatestBmr()
            ?: 0.0

        val today = todayString()
        val allDates = (intakes.map { it.date } + (activityByDate?.keys ?: emptySet()))
            .distinct()
            .filter { it <= today }
            .sorted()

        return allDates.mapNotNull { date ->
            val metabolism = metabolismByDate[date] ?: Metabolism(date = date, bmr = fallbackBmr, tef = 0.0)
            val intake = intakesByDate[date] ?: DailyIntake(date, 0.0)
            val metabolismWithTef = metabolism.copy(tef = intake.totalCalories * 0.1)
            val activity = activityByDate?.get(date)
            when (val result = calorieMathRepository.computeDailyBalance(intake, metabolismWithTef, activity)) {
                is CalorieResult.Success -> result.value
                is CalorieResult.Failure -> null
            }
        }
    }

    private fun selectIntakesForPersistedSources(
        healthIntakes: List<DailyIntake>,
        foodDiaryTotals: List<DailyMacroTotals>
    ): List<DailyIntake> {
        val healthByDate = healthIntakes.associateBy { it.date }
        val diaryByDate = foodDiaryTotals.associateBy { it.date }
        val allDates = (healthByDate.keys + diaryByDate.keys).distinct().sorted()

        return allDates.map { date ->
            when (userSettingsDataSource.getMacroDataSourceForDate(date)) {
                MacroDataSource.FOOD_DIARY -> diaryByDate[date]?.toDailyIntake() ?: DailyIntake(date, 0.0)
                MacroDataSource.HEALTH_CONNECT -> healthByDate[date] ?: DailyIntake(date, 0.0)
            }
        }
    }
}

private fun DailyMacroTotals.toDailyIntake() = DailyIntake(
    date = date,
    totalCalories = calories,
    proteinG = proteinG,
    carbG = carbG,
    fatG = fatG
)

private fun com.mettyoung.fitbro.data.health.HealthDataError.describe(): String = when (this) {
    com.mettyoung.fitbro.data.health.HealthDataError.PermissionDenied -> "Permission denied"
    com.mettyoung.fitbro.data.health.HealthDataError.NotAvailable -> "Not available"
    is com.mettyoung.fitbro.data.health.HealthDataError.QueryError -> "Query error"
}
