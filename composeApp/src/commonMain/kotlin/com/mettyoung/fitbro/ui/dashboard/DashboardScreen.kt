package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.model.DailyBalance
import com.mettyoung.fitbro.util.formatTimeAgo
import com.mettyoung.fitbro.util.minusDays
import com.mettyoung.fitbro.util.plusDays
import com.mettyoung.fitbro.util.toDisplayRange
import com.mettyoung.fitbro.util.todayString
import kotlinx.coroutines.delay

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
    var wasRefreshing by remember { mutableStateOf(false) }
    var showSuccessToast by remember { mutableStateOf(false) }
    var refreshError by remember { mutableStateOf<String?>(null) }

    val today = todayString()
    val startDate = state.selectedDateRange.startDate
    val canGoNext = startDate.plusDays(7) <= today
    val isLoading = state.uiState is DashboardUiState.Loading

    // Detect Loading → Success/Error transition triggered by user refresh
    LaunchedEffect(state.uiState) {
        if (wasRefreshing) {
            when (val uiState = state.uiState) {
                is DashboardUiState.Success -> {
                    wasRefreshing = false
                    showSuccessToast = true
                }
                is DashboardUiState.Error -> {
                    wasRefreshing = false
                    refreshError = uiState.message
                }
                is DashboardUiState.Loading -> { /* still loading */ }
            }
        }
    }

    LaunchedEffect(showSuccessToast) {
        if (showSuccessToast) {
            delay(2000)
            showSuccessToast = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        if (isLoading && wasRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            TextButton(
                                onClick = { wasRefreshing = true; onRefresh() },
                                enabled = !isLoading
                            ) { Text("🔄") }
                        }
                    }
                    TextButton(onClick = { showPicker = true }) {
                        Text("📅")
                    }
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
                    if (uiState.warnings.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        uiState.warnings.forEach { warning ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠ $warning",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
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

        // Success toast overlay
        if (showSuccessToast) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text("Data updated")
            }
        }
    }

    // Error dialog for user-triggered refresh failures
    refreshError?.let { error ->
        AlertDialog(
            onDismissRequest = { refreshError = null },
            title = { Text("Sync Failed") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(error, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        suggestAction(error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { refreshError = null }) { Text("OK") }
            }
        )
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

private fun suggestAction(errorMessage: String): String = when {
    "Auth expired" in errorMessage -> "Suggestion: Re-login to Cronometer"
    "Permission denied" in errorMessage -> "Suggestion: Grant Health Connect permission in Settings"
    "Network" in errorMessage -> "Suggestion: Check your internet connection"
    "Rate limited" in errorMessage -> "Suggestion: Wait a moment and try again"
    else -> "Suggestion: Try again later"
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
