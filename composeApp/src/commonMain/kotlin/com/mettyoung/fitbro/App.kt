package com.mettyoung.fitbro

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mettyoung.fitbro.data.model.DailyBalance
import com.mettyoung.fitbro.ui.dashboard.DashboardContent
import com.mettyoung.fitbro.ui.dashboard.DashboardState
import com.mettyoung.fitbro.ui.dashboard.DashboardUiState
import com.mettyoung.fitbro.ui.dashboard.DateRange

@Composable
@Preview
fun App() {
    MaterialTheme {
        val sampleBalances = remember {
            listOf(
                DailyBalance("2026-04-30", 2200.0, 2500.0, -300.0, bmr=1800.0, tef=180.0, neat=400.0, eat=120.0),
                DailyBalance("2026-05-01", 1800.0, 2100.0, -300.0, bmr=1800.0, tef=180.0, neat=300.0, eat=0.0),
                DailyBalance("2026-05-02", 2400.0, 2000.0,  400.0, bmr=1800.0, tef=180.0, neat=200.0, eat=0.0),
                DailyBalance("2026-05-03", 2600.0, 2200.0,  400.0, bmr=1800.0, tef=180.0, neat=350.0, eat=120.0),
                DailyBalance("2026-05-04", 1900.0, 2300.0, -400.0, bmr=1800.0, tef=180.0, neat=500.0, eat=0.0),
                DailyBalance("2026-05-05", 2100.0, 2000.0,  100.0, bmr=1800.0, tef=180.0, neat=150.0, eat=0.0),
                DailyBalance("2026-05-06", 2300.0, 2150.0,  150.0, bmr=1800.0, tef=180.0, neat=280.0, eat=0.0),
            )
        }
        val demoState = remember {
            DashboardState(
                uiState = DashboardUiState.Success(sampleBalances),
                lastSyncTime = emptyMap(),
                errorMessage = null,
                selectedDateRange = DateRange("2026-04-30", "2026-05-06")
            )
        }
        DashboardContent(
            state = demoState,
            onRefresh = {},
            onDateRangeChanged = {},
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize()
        )
    }
}
