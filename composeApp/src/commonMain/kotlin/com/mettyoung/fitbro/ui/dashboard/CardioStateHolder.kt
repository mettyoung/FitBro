package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.model.resistanceCount
import com.mettyoung.fitbro.data.model.strengthSessions
import com.mettyoung.fitbro.data.model.strengthSources
import com.mettyoung.fitbro.data.model.overlappingSessions

import com.mettyoung.fitbro.data.cache.UserSettingsDataSource
import com.mettyoung.fitbro.data.health.HealthDataError
import com.mettyoung.fitbro.data.health.HealthDataSource
import com.mettyoung.fitbro.data.health.HealthResult
import com.mettyoung.fitbro.data.model.CardioSession
import com.mettyoung.fitbro.data.model.Week
import com.mettyoung.fitbro.data.model.comparisonDays
import com.mettyoung.fitbro.data.model.minutesOn
import com.mettyoung.fitbro.data.model.WorkoutType
import com.mettyoung.fitbro.data.model.visible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.mettyoung.fitbro.util.todayString
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
    private val _state = MutableStateFlow(
        CardioState(
            hiddenTypes = userSettingsDataSource.getHiddenWorkoutTypes(),
            excludedResistanceSources = userSettingsDataSource.getExcludedResistanceSources()
        )
    )
    val state: StateFlow<CardioState> = _state.asStateFlow()

    /**
     * Last successful read (previous + selected week), unfiltered, so hide/show re-filters
     * without re-reading Health Connect.
     */
    private var allSessions: List<CardioSession> = emptyList()
    private var loadJob: Job? = null

    fun setWeek(week: Week) {
        if (week == _state.value.week && allSessions.isNotEmpty()) return
        _state.update { it.copy(week = week) }
        refresh()
    }

    /** Re-reads the current range; call on resume so sessions synced in the background show up. */
    fun refresh() {
        loadJob?.cancel()
        val week = _state.value.week
        loadJob = scope.launch {
            _state.update { it.copy(isLoading = true) }
            when (val result = healthDataSource.readExerciseSessions(week.previous().start, week.end)) {
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

    /** Opens a workout's detail page and reads its full record (UDF: result lands in [CardioState.workoutDetail]). */
    fun openWorkout(session: CardioSession) {
        _state.update { it.copy(workoutDetail = WorkoutDetailState.Loading(session)) }
        scope.launch {
            val next = when (val result = healthDataSource.readWorkoutDetail(session)) {
                is HealthResult.Success -> WorkoutDetailState.Loaded(result.value, overlappingSessions(session, allSessions))
                is HealthResult.Failure -> WorkoutDetailState.Failed(session, result.error.message())
            }
            // Ignore a late result if the user already left or opened another workout.
            _state.update { if (it.workoutDetail?.session?.id == session.id) it.copy(workoutDetail = next) else it }
        }
    }

    fun closeWorkout() = _state.update { it.copy(workoutDetail = null) }

    fun setResistanceSourceIncluded(source: String, included: Boolean) {
        val excluded = _state.value.excludedResistanceSources.let { if (included) it - source else it + source }
        userSettingsDataSource.setExcludedResistanceSources(excluded)
        _state.update { it.copy(excludedResistanceSources = excluded).withFilter(allSessions) }
    }

    fun hideType(type: WorkoutType) = setHidden(_state.value.hiddenTypes + type)

    fun showType(type: WorkoutType) = setHidden(_state.value.hiddenTypes - type)

    fun resetHiddenTypes() = setHidden(WorkoutType.DEFAULT_HIDDEN)

    private fun setHidden(types: Set<WorkoutType>) {
        userSettingsDataSource.setHiddenWorkoutTypes(types)
        _state.update { it.copy(hiddenTypes = types).withFilter(allSessions) }
    }

    private fun CardioState.withFilter(all: List<CardioSession>): CardioState {
        val thisWeek = all.filter { week.contains(it.date) }
        val visible = thisWeek.visible(hiddenTypes)
        val previousWeek = week.previous()
        val previousVisible = all.filter { previousWeek.contains(it.date) }.visible(hiddenTypes)
        return copy(
            sessions = visible,
            totalMinutes = visible.sumOf { it.minutes },
            previousWeekMinutes = previousVisible.minutesOn(comparisonDays(week, todayString(), includeToday = true)),
            hiddenCount = thisWeek.size - visible.size,
            resistanceCount = resistanceCount(all, week, excludedResistanceSources),
            strengthSessions = strengthSessions(all, week, excludedResistanceSources),
            resistanceSources = strengthSources(all, excludedResistanceSources)
        )
    }
}

private fun HealthDataError.message(): String = when (this) {
    HealthDataError.PermissionDenied ->
        "FitBro can't read exercise sessions. Allow Exercise access for FitBro in Health Connect (Apple Health on iPhone)."
    HealthDataError.NotAvailable -> "Health data isn't available on this device."
    is HealthDataError.QueryError -> "Couldn't read exercise sessions."
}
