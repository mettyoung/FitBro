package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mettyoung.fitbro.data.model.DailyBalance
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary
import com.mettyoung.fitbro.util.formatTimeAgo
import com.mettyoung.fitbro.util.minusDays
import com.mettyoung.fitbro.util.plusDays
import com.mettyoung.fitbro.util.toDisplayRange
import com.mettyoung.fitbro.util.todayString
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

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

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(24.dp))
            
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        text = "Mi Fitness Style",
                        style = MaterialTheme.typography.labelSmall,
                        color = MiOrange,
                        letterSpacing = 0.5.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { wasRefreshing = true; onRefresh() },
                        enabled = !isLoading
                    ) {
                        if (isLoading && wasRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MiOrange)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MiOrange)
                        }
                    }
                    IconButton(onClick = { showPicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = MiOrange)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Date Navigator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(28.dp))
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val newStart = startDate.minusDays(7)
                    onDateRangeChanged(DateRange(newStart, newStart.plusDays(6)))
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev", tint = MiOrange)
                }

                Text(
                    text = startDate.toDisplayRange(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = {
                        val newStart = startDate.plusDays(7)
                        onDateRangeChanged(DateRange(newStart, newStart.plusDays(6)))
                    },
                    enabled = canGoNext
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next",
                        tint = if (canGoNext) MiOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            when (val uiState = state.uiState) {
                is DashboardUiState.Loading -> {
                    Box(
                        modifier = Modifier.height(300.dp).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MiOrange)
                    }
                }
                is DashboardUiState.Success -> {
                    // Summary Card with Chart - Representing the 7-day sliding window
                    SlidingWindowInsightCard(
                        balances = uiState.balances,
                        onBarClick = { selectedBreakdown = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    // Condensed Daily History Section
                    Text(
                        text = "Daily History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            uiState.balances.forEachIndexed { index, balance ->
                                CondensedLogItem(
                                    balance = balance,
                                    onClick = { selectedBreakdown = balance }
                                )
                                if (index < uiState.balances.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 24.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                    )
                                }
                            }
                        }
                    }
                    
                    if (uiState.warnings.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        uiState.warnings.forEach { warning ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⚠️", fontSize = 16.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = warning,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFC62828),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
                is DashboardUiState.Error -> {
                    Box(
                        modifier = Modifier.height(300.dp).fillMaxWidth(),
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
                            Button(
                                onClick = onRefresh,
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MiOrange)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            SyncStatusBar(state = state)
            Spacer(Modifier.height(32.dp))
        }

        if (showSuccessToast) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text("Sync successful", color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

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
                TextButton(onClick = { refreshError = null }) { Text("OK", color = MiOrange) }
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

@Composable
private fun SlidingWindowInsightCard(
    balances: List<DailyBalance>,
    onBarClick: (DailyBalance) -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = remember(balances) { com.mettyoung.fitbro.data.model.calculateWindowMetrics(balances) }
    val trendColor = when (metrics.trend) {
        com.mettyoung.fitbro.data.model.TrendDirection.IMPROVING -> Color(0xFF4CAF50)
        com.mettyoung.fitbro.data.model.TrendDirection.DECLINING -> Color(0xFFF44336)
        com.mettyoung.fitbro.data.model.TrendDirection.STABLE -> MiOrange
    }
    val trendIcon = when (metrics.trend) {
        com.mettyoung.fitbro.data.model.TrendDirection.IMPROVING -> "↑"
        com.mettyoung.fitbro.data.model.TrendDirection.DECLINING -> "↓"
        com.mettyoung.fitbro.data.model.TrendDirection.STABLE -> "→"
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Average Daily Balance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MiTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${formatCalorieValue(metrics.avgDailyBalance)} kcal",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(trendColor))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Weekly Total: ${formatCalorieValue(metrics.totalBalance)} kcal",
                            style = MaterialTheme.typography.bodySmall,
                            color = MiTextSecondary
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(trendColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = trendIcon,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = trendColor
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            CalorieBalanceChart(
                balances = balances,
                onBarClick = onBarClick,
                modifier = Modifier.fillMaxWidth().height(180.dp)
            )
        }
    }
}

@Composable
private fun CondensedLogItem(
    balance: DailyBalance,
    onClick: () -> Unit
) {
    val balanceColor = if (balance.balance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            // Very compact date/day
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
                Text(
                    text = balance.date.toDayAbbr(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MiTextSecondary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = balance.date.split("-").last(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            
            Spacer(Modifier.width(20.dp))
            
            // Intake/Burn summary
            Column {
                Text(
                    text = "${balance.intake.roundToInt()} kcal in",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${balance.burn.roundToInt()} kcal out",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MiTextSecondary
                )
            }
        }
        
        // Net balance
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${if (balance.balance >= 0) "+" else ""}${balance.balance.roundToInt()}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.ExtraBold,
                color = balanceColor
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MiTextSecondary.copy(alpha = 0.5f)
            )
        }
    }
}

private fun String.toDayAbbr(): String {
    val parts = split("-")
    if (parts.size != 3) return ""
    val year = parts[0].toIntOrNull() ?: return ""
    val month = parts[1].toIntOrNull() ?: return ""
    val day = parts[2].toIntOrNull() ?: return ""
    return dayOfWeekAbbr(year, month, day)
}

private fun dayOfWeekAbbr(year: Int, month: Int, day: Int): String {
    val m = if (month < 3) month + 12 else month
    val y = if (month < 3) year - 1 else year
    val k = y % 100
    val j = y / 100
    val h = (day + (13 * (m + 1)) / 5 + k + k / 4 + j / 4 - 2 * j).mod(7)
    // 0: Saturday, 1: Sunday, 2: Monday, 3: Tuesday, 4: Wednesday, 5: Thursday, 6: Friday
    return listOf("SAT", "SUN", "MON", "TUE", "WED", "THU", "FRI")[h]
}

private fun formatCalorieValue(value: Double): String {
    val v = value.toInt()
    return if (v >= 1000) "${v / 1000}.${(v % 1000) / 100}k" else v.toString()
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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val statusText = when {
            isLoading -> "Syncing data..."
            latestSyncMs != null -> "Last updated: ${formatTimeAgo(latestSyncMs)}"
            else -> "Never synced"
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        if (isOffline && !isLoading) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(Color(0xFFFFEBEE), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Offline",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFC62828)
                )
            }
        }
    }
}
