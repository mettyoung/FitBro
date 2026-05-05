package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.cache.CacheSource

data class DashboardState(
    val uiState: DashboardUiState,
    val lastSyncTime: Map<CacheSource, Long?>,
    val errorMessage: String?,
    val selectedDateRange: DateRange
)
