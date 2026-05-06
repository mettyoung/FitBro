package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import com.mettyoung.fitbro.data.model.DailyBalance
import com.mettyoung.fitbro.util.formatTimeAgo
import com.mettyoung.fitbro.util.minusDays
import com.mettyoung.fitbro.util.plusDays
import com.mettyoung.fitbro.util.toDisplayRange
import com.mettyoung.fitbro.util.todayString

@Composable
fun DashboardScreen(
    stateHolder: DashboardStateHolder,
    modifier: Modifier = Modifier
) {
    val state by stateHolder.state.collectAsState()
    DashboardContent(
        state = state,
        onRefresh = { stateHolder.refresh() },
        onDateRangeChanged = { stateHolder.selectDateRange(it) },
        modifier = modifier
    )
}

@Composable
fun DashboardContent(
    state: DashboardState,
    onRefresh: () -> Unit,
    onDateRangeChanged: (DateRange) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    var selectedBreakdown by remember { mutableStateOf<DailyBalance?>(null) }
    val today = todayString()
    val startDate = state.selectedDateRange.startDate
    val canGoNext = startDate.plusDays(7) <= today

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Title row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Calorie Balance",
                style = MaterialTheme.typography.headlineMedium
            )
            TextButton(onClick = { showPicker = true }) {
                Text("📅")
            }
        }

        // Week navigation row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                val newStart = startDate.minusDays(7)
                onDateRangeChanged(DateRange(newStart, newStart.plusDays(6)))
            }) { Text("‹ Prev") }

            Text(
                text = startDate.toDisplayRange(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            TextButton(
                onClick = {
                    val newStart = startDate.plusDays(7)
                    onDateRangeChanged(DateRange(newStart, newStart.plusDays(6)))
                },
                enabled = canGoNext
            ) { Text("Next ›") }
        }

        Spacer(Modifier.height(8.dp))

        when (val uiState = state.uiState) {
            is DashboardUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is DashboardUiState.Success -> {
                CalorieBalanceChart(
                    balances = uiState.balances,
                    onBarClick = { selectedBreakdown = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
            is DashboardUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onRefresh) {
                            Text("Retry")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        SyncStatusBar(state = state)
    }

    selectedBreakdown?.let { breakdown ->

        BreakdownDialog(
            balance = breakdown,
            onDismiss = { selectedBreakdown = null }
        )
    }

    if (showPicker) {
        DatePickerDialog(
            initialStartDate = startDate,
            onDismiss = { showPicker = false },
            onDateRangeSelected = { range ->
                onDateRangeChanged(range)
                showPicker = false
            }
        )
    }
}

@Composable
private fun SyncStatusBar(state: DashboardState) {
    val isLoading = state.uiState is DashboardUiState.Loading
    val isOffline = state.errorMessage != null
    val latestSyncMs = state.lastSyncTime.values.filterNotNull().maxOrNull()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val statusText = when {
            isLoading -> "Syncing..."
            latestSyncMs != null -> "Last synced: ${formatTimeAgo(latestSyncMs)}"
            else -> "Never synced"
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isOffline && !isLoading) {
            Text(
                text = "OFFLINE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onError,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}
