package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.model.DailyBalance

sealed class DashboardUiState {
    data object Loading : DashboardUiState()
    data class Success(
        val balances: List<DailyBalance>,
        val warnings: List<String> = emptyList()
    ) : DashboardUiState()
    data class Error(val message: String) : DashboardUiState()
}
