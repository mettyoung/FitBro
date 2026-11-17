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
import com.mettyoung.fitbro.data.model.Metabolism
import com.mettyoung.fitbro.data.model.Week
import com.mettyoung.fitbro.data.model.WeekStart
import com.mettyoung.fitbro.data.model.comparisonDays
import com.mettyoung.fitbro.data.model.summarizeWeek
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
    /** Receives each refresh's computed balances (wired to cloud sync so expenditure reaches the MCP server). */
    private val onBalancesComputed: (List<DailyBalance>) -> Unit = {}
) {
    private val _state = MutableStateFlow(
        userSettingsDataSource.getWeekStart().let { weekStart ->
            DashboardState(
                uiState = DashboardUiState.Loading,
                lastSyncTime = emptyMap(),
                errorMessage = null,
                weekStart = weekStart,
                selectedWeek = Week.of(todayString(), weekStart),
                showCarbsFat = userSettingsDataSource.getShowCarbsFatComparison()
            )
        }
    )
    val state: StateFlow<DashboardState> = _state.asStateFlow()

    private val _selectedDate = MutableStateFlow(todayString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun refresh() {
        scope.launch(Dispatchers.Default) {
            val today = todayString()
            val week = _state.value.selectedWeek
            // FR-3: one read covering the previous week through the selected week (clamped to today).
            val range = DateRange(week.previous().start, minOf(week.end, today))
            _state.update { it.copy(uiState = DashboardUiState.Loading, errorMessage = null) }

            val metabolismDeferred = async { healthDataSource.readBasalMetabolicRate(range.startDate, range.endDate) }
            val activityDeferred = async { healthDataSource.readActivityData(range.startDate, range.endDate) }
            val foodDiaryTotalsDeferred = async {
                foodDiaryRepository.getDailyTotalsForRange(range.startDate, range.endDate).first()
            }

            val metabolismResult = metabolismDeferred.await()
            val activityResult = activityDeferred.await()
            val foodDiaryTotals = foodDiaryTotalsDeferred.await()

            val healthMetabolismFailed = metabolismResult is HealthResult.Failure
            val healthFailed = activityResult is HealthResult.Failure

            val now = currentEpochMs()
            val errors = mutableListOf<String>()

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

            val allBalances = computeBalances(foodDiaryTotals.map { it.toDailyIntake() }, metabolisms, activities)
            val balances = allBalances.filter { week.contains(it.date) }
            // Averages exclude today (partial day) for the current week.
            val weekSummary = summarizeWeek(balances, week.averagingDays(today))
            val previousWeekSummary = summarizeWeek(allBalances, comparisonDays(week, today))

            val updatedSyncTime = mapOf(
                CacheSource.HEALTH_METABOLISM to cacheDataSource.getSyncTimestamp(CacheSource.HEALTH_METABOLISM),
                CacheSource.HEALTH_ACTIVITY to cacheDataSource.getSyncTimestamp(CacheSource.HEALTH_ACTIVITY),
            )

            val warnings = buildWarnings(healthMetabolismFailed, healthFailed)
            val totalFailure = allBalances.isEmpty() && errors.isNotEmpty() &&
                updatedSyncTime.values.all { it == null }
            val newUiState = if (totalFailure) {
                DashboardUiState.Error(errors.joinToString("\n"))
            } else {
                DashboardUiState.Success(balances, warnings)
            }
            if (!totalFailure) onBalancesComputed(allBalances)

            _state.update {
                it.copy(
                    uiState = newUiState,
                    lastSyncTime = updatedSyncTime,
                    weekSummary = weekSummary,
                    previousWeekSummary = previousWeekSummary,
                    previousWeekBalances = allBalances.filter { week.previous().contains(it.date) },
                    errorMessage = if (errors.isNotEmpty()) errors.joinToString("\n") else null
                )
            }
        }
    }

    fun selectWeek(week: Week) {
        val current = Week.of(todayString(), _state.value.weekStart)
        val target = if (week.start > current.start) current else week
        if (target == _state.value.selectedWeek) return
        _state.update { it.copy(selectedWeek = target) }
        refresh()
    }

    fun previousWeek() = selectWeek(_state.value.selectedWeek.previous())

    fun nextWeek() = selectWeek(_state.value.selectedWeek.next())

    fun currentWeek() = selectWeek(Week.of(todayString(), _state.value.weekStart))

    /** US-003: persist and re-align to the week containing the old selection's first day. */
    fun setWeekStart(weekStart: WeekStart) {
        if (weekStart == _state.value.weekStart) return
        userSettingsDataSource.setWeekStart(weekStart)
        _state.update {
            it.copy(weekStart = weekStart, selectedWeek = Week.of(it.selectedWeek.start, weekStart))
        }
        refresh()
    }

    fun setShowCarbsFat(show: Boolean) {
        userSettingsDataSource.setShowCarbsFatComparison(show)
        _state.update { it.copy(showCarbsFat = show) }
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
