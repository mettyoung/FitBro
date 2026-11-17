package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.cache.CacheSource
import com.mettyoung.fitbro.data.model.Week
import com.mettyoung.fitbro.data.model.WeekStart
import com.mettyoung.fitbro.data.model.WeekSummary

data class DashboardState(
    val uiState: DashboardUiState,
    val lastSyncTime: Map<CacheSource, Long?>,
    val errorMessage: String?,
    val weekStart: WeekStart,
    val selectedWeek: Week,
    /** Selected week's numbers (cardio minutes filled in by the UI from CardioState). */
    val weekSummary: WeekSummary = WeekSummary.EMPTY,
    /** Previous week, like-for-like with the selected week (FR-7). */
    val previousWeekSummary: WeekSummary = WeekSummary.EMPTY,
    val showCarbsFat: Boolean = false
)
