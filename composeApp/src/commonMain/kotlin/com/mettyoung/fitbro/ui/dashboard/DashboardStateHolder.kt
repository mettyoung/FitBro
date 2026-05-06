package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.cache.CacheDataSource
import com.mettyoung.fitbro.data.cache.CacheSource
import com.mettyoung.fitbro.data.health.HealthDataSource
import com.mettyoung.fitbro.data.health.HealthResult
import com.mettyoung.fitbro.data.model.ActivityBurn
import com.mettyoung.fitbro.data.model.DailyBalance
import com.mettyoung.fitbro.data.model.DailyIntake
import com.mettyoung.fitbro.data.model.Metabolism
import com.mettyoung.fitbro.data.repository.CalorieMathRepository
import com.mettyoung.fitbro.data.repository.CalorieResult
import com.mettyoung.fitbro.util.currentEpochMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardStateHolder(
    private val healthDataSource: HealthDataSource,
    private val cacheDataSource: CacheDataSource,
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

    fun refresh() {
        scope.launch(Dispatchers.Default) {
            val range = _state.value.selectedDateRange
            _state.update { it.copy(uiState = DashboardUiState.Loading, errorMessage = null) }

            val intakeDeferred = async { healthDataSource.readDailyIntake(range.startDate, range.endDate) }
            val metabolismDeferred = async { healthDataSource.readBasalMetabolicRate(range.startDate, range.endDate) }
            val activityDeferred = async { healthDataSource.readActivityData(range.startDate, range.endDate) }

            val intakeResult = intakeDeferred.await()
            val metabolismResult = metabolismDeferred.await()
            val activityResult = activityDeferred.await()

            val healthIntakeFailed = intakeResult is HealthResult.Failure
            val healthMetabolismFailed = metabolismResult is HealthResult.Failure
            val healthFailed = activityResult is HealthResult.Failure

            val now = currentEpochMs()
            val errors = mutableListOf<String>()

            val intakes: List<DailyIntake> = when (intakeResult) {
                is HealthResult.Success -> {
                    cacheDataSource.saveDailyIntake(range.startDate, range.endDate, intakeResult.value)
                    cacheDataSource.saveSyncTimestamp(CacheSource.CRONOMETER_INTAKE, now)
                    intakeResult.value
                }
                is HealthResult.Failure -> {
                    errors.add("Health intake: ${intakeResult.error.describe()}")
                    cacheDataSource.getDailyIntake(range.startDate, range.endDate) ?: emptyList()
                }
            }

            val metabolisms: List<Metabolism> = when (metabolismResult) {
                is HealthResult.Success -> {
                    cacheDataSource.saveMetabolism(range.startDate, range.endDate, metabolismResult.value)
                    cacheDataSource.saveSyncTimestamp(CacheSource.CRONOMETER_METABOLISM, now)
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

            val balances = computeBalances(intakes, metabolisms, activities)

            val updatedSyncTime = mapOf(
                CacheSource.CRONOMETER_INTAKE to cacheDataSource.getSyncTimestamp(CacheSource.CRONOMETER_INTAKE),
                CacheSource.CRONOMETER_METABOLISM to cacheDataSource.getSyncTimestamp(CacheSource.CRONOMETER_METABOLISM),
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
        _state.update { it.copy(selectedDateRange = dateRange) }
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

        val allDates = (intakes.map { it.date } + (activityByDate?.keys ?: emptySet())).distinct().sorted()

        return allDates.mapNotNull { date ->
            val metabolism = metabolismByDate[date] ?: return@mapNotNull null
            val intake = intakesByDate[date] ?: DailyIntake(date, 0.0)
            val metabolismWithTef = metabolism.copy(tef = intake.totalCalories * 0.1)
            val activity = activityByDate?.get(date)
            when (val result = calorieMathRepository.computeDailyBalance(intake, metabolismWithTef, activity)) {
                is CalorieResult.Success -> result.value
                is CalorieResult.Failure -> null
            }
        }
    }
}

private fun com.mettyoung.fitbro.data.health.HealthDataError.describe(): String = when (this) {
    com.mettyoung.fitbro.data.health.HealthDataError.PermissionDenied -> "Permission denied"
    com.mettyoung.fitbro.data.health.HealthDataError.NotAvailable -> "Not available"
    is com.mettyoung.fitbro.data.health.HealthDataError.QueryError -> "Query error"
}
