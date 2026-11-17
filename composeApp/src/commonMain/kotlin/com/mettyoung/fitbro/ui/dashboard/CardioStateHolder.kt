package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.cache.UserSettingsDataSource
import com.mettyoung.fitbro.data.health.HealthDataError
import com.mettyoung.fitbro.data.health.HealthDataSource
import com.mettyoung.fitbro.data.health.HealthResult
import com.mettyoung.fitbro.data.model.CardioSession
import com.mettyoung.fitbro.data.model.WorkoutType
import com.mettyoung.fitbro.data.model.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Cardio = exercise sessions read from Health Connect / HealthKit (e.g. synced there by Mi Fitness),
 * minus the workout types the user has hidden. Single owner of the hidden-types setting.
 */
class CardioStateHolder(
    private val healthDataSource: HealthDataSource,
    private val userSettingsDataSource: UserSettingsDataSource,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(CardioState(hiddenTypes = userSettingsDataSource.getHiddenWorkoutTypes()))
    val state: StateFlow<CardioState> = _state.asStateFlow()

    /** Last successful read, unfiltered, so hide/show re-filters without re-reading Health Connect. */
    private var allSessions: List<CardioSession> = emptyList()
    private var loadJob: Job? = null

    fun setDateRange(dateRange: DateRange) {
        _state.update { it.copy(dateRange = dateRange) }
        refresh()
    }

    /** Re-reads the current range; call on resume so sessions synced in the background show up. */
    fun refresh() {
        loadJob?.cancel()
        val range = _state.value.dateRange
        loadJob = scope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = healthDataSource.readExerciseSessions(range.startDate, range.endDate)) {
                is HealthResult.Success -> {
                    allSessions = result.value.sortedByDescending { s -> s.startEpochMs }
                    _state.update { it.copy(isLoading = false, error = null).withFilter(allSessions) }
                }
                is HealthResult.Failure -> {
                    allSessions = emptyList()
                    _state.update {
                        it.copy(isLoading = false, error = result.error.message()).withFilter(allSessions)
                    }
                }
            }
        }
    }

    fun hideType(type: WorkoutType) = setHidden(_state.value.hiddenTypes + type)

    fun showType(type: WorkoutType) = setHidden(_state.value.hiddenTypes - type)

    fun resetHiddenTypes() = setHidden(WorkoutType.DEFAULT_HIDDEN)

    private fun setHidden(types: Set<WorkoutType>) {
        userSettingsDataSource.setHiddenWorkoutTypes(types)
        _state.update { it.copy(hiddenTypes = types).withFilter(allSessions) }
    }

    private fun CardioState.withFilter(all: List<CardioSession>): CardioState {
        val visible = all.visible(hiddenTypes)
        return copy(
            sessions = visible,
            totalMinutes = visible.sumOf { it.minutes },
            hiddenCount = all.size - visible.size
        )
    }
}

private fun HealthDataError.message(): String = when (this) {
    HealthDataError.PermissionDenied ->
        "FitBro can't read exercise sessions. Allow Exercise access for FitBro in Health Connect (Apple Health on iPhone)."
    HealthDataError.NotAvailable -> "Health data isn't available on this device."
    is HealthDataError.QueryError -> "Couldn't read exercise sessions."
}
