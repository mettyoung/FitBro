package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.health.HealthDataError
import com.mettyoung.fitbro.data.health.HealthDataSource
import com.mettyoung.fitbro.data.health.HealthResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Cardio = exercise sessions read from Health Connect / HealthKit (e.g. synced there by Mi Fitness). */
class CardioStateHolder(
    private val healthDataSource: HealthDataSource,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(CardioState())
    val state: StateFlow<CardioState> = _state.asStateFlow()

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
            val result = healthDataSource.readExerciseSessions(range.startDate, range.endDate)
            _state.update {
                when (result) {
                    is HealthResult.Success -> {
                        val sessions = result.value.sortedByDescending { s -> s.startEpochMs }
                        it.copy(
                            sessions = sessions,
                            totalMinutes = sessions.sumOf { s -> s.minutes },
                            isLoading = false,
                            error = null
                        )
                    }
                    is HealthResult.Failure -> it.copy(
                        sessions = emptyList(),
                        totalMinutes = 0,
                        isLoading = false,
                        error = result.error.message()
                    )
                }
            }
        }
    }
}

private fun HealthDataError.message(): String = when (this) {
    HealthDataError.PermissionDenied ->
        "FitBro can't read exercise sessions. Allow Exercise access for FitBro in Health Connect (Apple Health on iPhone)."
    HealthDataError.NotAvailable -> "Health data isn't available on this device."
    is HealthDataError.QueryError -> "Couldn't read exercise sessions."
}
