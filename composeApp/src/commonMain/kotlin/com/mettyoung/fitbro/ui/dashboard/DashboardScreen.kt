package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mettyoung.fitbro.data.model.DailyBalance
import com.mettyoung.fitbro.ui.ColorCarbs
import com.mettyoung.fitbro.ui.ColorFat
import com.mettyoung.fitbro.ui.ColorProtein
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary
import com.mettyoung.fitbro.util.formatTimeAgo
import com.mettyoung.fitbro.util.minusDays
import com.mettyoung.fitbro.util.plusDays
import com.mettyoung.fitbro.util.toShortDate
import com.mettyoung.fitbro.util.todayString
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    stateHolder: DashboardStateHolder,
    weeklyCardioMinutes: Int = 0,
    modifier: Modifier = Modifier
) {
    val state by stateHolder.state.collectAsState()
    DashboardContent(
        state = state,
        onRefresh = { stateHolder.refresh() },
        onDateRangeChanged = { stateHolder.selectDateRange(it) },
        weeklyCardioMinutes = weeklyCardioMinutes,
        modifier = modifier
    )
}

@Composable
fun DashboardContent(
    state: DashboardState,
    onRefresh: () -> Unit,
    onDateRangeChanged: (DateRange) -> Unit = {},
    weeklyCardioMinutes: Int = 0,
    modifier: Modifier = Modifier
) {
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var selectedBreakdown by remember { mutableStateOf<DailyBalance?>(null) }
    var selectedLens: DashboardViewMode? by remember { mutableStateOf(null) }
    var wasRefreshing by remember { mutableStateOf(false) }
    var showSuccessToast by remember { mutableStateOf(false) }
    var refreshError by remember { mutableStateOf<String?>(null) }

    val today = todayString()
    val startDate = state.selectedDateRange.startDate
    val endDate = state.selectedDateRange.endDate
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

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header — Samsung Health style: flat, large title, compact date nav
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarPadding + 20.dp, bottom = 16.dp, start = 24.dp, end = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (selectedLens != null) {
                            IconButton(
                                onClick = { selectedLens = null },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            text = selectedLens?.label ?: "FitBro",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (isLoading && wasRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MiOrange)
                    } else {
                        IconButton(onClick = { wasRefreshing = true; onRefresh() }, enabled = !isLoading) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MiTextSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Compact date navigator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onDateRangeChanged(DateRange(startDate.minusDays(7), endDate)) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev", modifier = Modifier.size(20.dp), tint = MiTextSecondary)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = startDate.toShortDate(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { showStartPicker = true }.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Text("–", style = MaterialTheme.typography.bodyMedium, color = MiTextSecondary)
                        Text(
                            text = endDate.toShortDate(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable { showEndPicker = true }.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    IconButton(
                        onClick = { onDateRangeChanged(DateRange(startDate.plusDays(7), endDate)) },
                        enabled = canGoNext,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next",
                            modifier = Modifier.size(20.dp),
                            tint = if (canGoNext) MiTextSecondary else MiTextSecondary.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(24.dp))

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
                        if (selectedLens == null) {
                            DashboardHome(
                                balances = uiState.balances,
                                weeklyCardioMinutes = weeklyCardioMinutes,
                                onBalanceClick = { selectedLens = DashboardViewMode.BALANCE },
                                onIntakeClick = { selectedLens = DashboardViewMode.INTAKE },
                                onExpenditureClick = { selectedLens = DashboardViewMode.EXPENDITURE }
                            )
                        } else {
                            DashboardDetail(
                                balances = uiState.balances,
                                viewMode = selectedLens!!,
                                onBarClick = { selectedBreakdown = it }
                            )
                        }
                        if (uiState.warnings.isNotEmpty()) {
                            Spacer(Modifier.height(24.dp))
                            uiState.warnings.forEach { warning ->
                                WarningCard(message = warning)
                            }
                        }
                    }
                    is DashboardUiState.Error -> {
                        ErrorView(message = uiState.message, onRetry = onRefresh)
                    }
                }

                Spacer(Modifier.height(32.dp))
                SyncStatusBar(state = state)
                Spacer(Modifier.height(48.dp))
            }
        }

        if (showSuccessToast) {
            SuccessToast(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    refreshError?.let { error ->
        ErrorDialog(error = error, onDismiss = { refreshError = null })
    }

    selectedBreakdown?.let { breakdown ->
        BreakdownDialog(balance = breakdown, onDismiss = { selectedBreakdown = null })
    }

    if (showStartPicker) {
        DatePickerDialog(
            initialStartDate = startDate,
            onDismiss = { showStartPicker = false },
            onDateRangeSelected = { range ->
                onDateRangeChanged(DateRange(range.startDate, endDate))
                showStartPicker = false
            },
            singleDay = true
        )
    }

    if (showEndPicker) {
        DatePickerDialog(
            initialStartDate = endDate,
            onDismiss = { showEndPicker = false },
            onDateRangeSelected = { range ->
                val newEnd = if (range.startDate >= startDate) range.startDate else endDate
                onDateRangeChanged(DateRange(startDate, newEnd))
                showEndPicker = false
            },
            singleDay = true
        )
    }
}

@Composable
private fun DashboardHome(
    balances: List<DailyBalance>,
    weeklyCardioMinutes: Int,
    onBalanceClick: () -> Unit,
    onIntakeClick: () -> Unit,
    onExpenditureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SummaryMetricCard(
            viewMode = DashboardViewMode.BALANCE,
            balances = balances,
            onClick = onBalanceClick,
            isBig = true,
            extraContent = {
                if (weeklyCardioMinutes > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "🏃 $weeklyCardioMinutes min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MiTextSecondary
                    )
                }
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryMetricCard(
                viewMode = DashboardViewMode.INTAKE,
                balances = balances,
                onClick = onIntakeClick,
                modifier = Modifier.weight(1f)
            )
            SummaryMetricCard(
                viewMode = DashboardViewMode.EXPENDITURE,
                balances = balances,
                onClick = onExpenditureClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryMetricCard(
    viewMode: DashboardViewMode,
    balances: List<DailyBalance>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBig: Boolean = false,
    extraContent: @Composable () -> Unit = {}
) {
    val avgValue = if (balances.isEmpty()) 0.0 else balances.sumOf { viewMode.valueOf(it) } / balances.size
    val netBalance = balances.sumOf { it.balance }.roundToInt()

    val valueColor = when (viewMode) {
        DashboardViewMode.BALANCE     -> if (netBalance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
        DashboardViewMode.INTAKE      -> Color(0xFF4CAF50)
        DashboardViewMode.EXPENDITURE -> Color(0xFFF44336)
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(if (isBig) 20.dp else 16.dp)) {
            Text(
                text = viewMode.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MiTextSecondary
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "${formatCalorieValue(avgValue)} kcal",
                style = if (isBig) MaterialTheme.typography.displayMedium else MaterialTheme.typography.headlineMedium,
                color = valueColor,
                fontWeight = FontWeight.Black
            )

            if (!isBig) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Average daily",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiTextSecondary
                )
            }

            extraContent()

            if (isBig) {
                Spacer(Modifier.height(20.dp))
                CalorieBalanceChart(
                    balances = balances,
                    onBarClick = { _ -> onClick() },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    valueSelector = { viewMode.valueOf(it) },
                    diverging = viewMode.isDiverging,
                    showDayLabels = true
                )
            }
        }
    }
}

@Composable
private fun DashboardDetail(
    balances: List<DailyBalance>,
    viewMode: DashboardViewMode,
    onBarClick: (DailyBalance) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SlidingWindowInsightCard(
            balances = balances,
            viewMode = viewMode,
            onBarClick = onBarClick,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "History",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "${balances.size} days",
                style = MaterialTheme.typography.labelSmall,
                color = MiTextSecondary
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                balances.reversed().forEachIndexed { index, balance ->
                    CondensedLogItem(
                        balance = balance,
                        viewMode = viewMode,
                        onClick = { onBarClick(balance) }
                    )
                    if (index < balances.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WarningCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
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
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.height(300.dp).fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MiOrange)
            ) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun SuccessToast(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(bottom = 32.dp)
            .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(16.dp))
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text("Sync successful", color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ErrorDialog(error: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sync Failed", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(error, style = MaterialTheme.typography.bodyMedium)
                Text(
                    suggestAction(error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MiTextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK", color = MiOrange, fontWeight = FontWeight.Bold) }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun SlidingWindowInsightCard(
    balances: List<DailyBalance>,
    viewMode: DashboardViewMode,
    onBarClick: (DailyBalance) -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = remember(balances) { com.mettyoung.fitbro.data.model.calculateWindowMetrics(balances) }
    val trendColor = when (metrics.trend) {
        com.mettyoung.fitbro.data.model.TrendDirection.IMPROVING -> Color(0xFF4CAF50)
        com.mettyoung.fitbro.data.model.TrendDirection.DECLINING -> Color(0xFFF44336)
        com.mettyoung.fitbro.data.model.TrendDirection.STABLE -> MiOrange
    }

    val isBalance = viewMode == DashboardViewMode.BALANCE
    val headlineLabel = when (viewMode) {
        DashboardViewMode.BALANCE -> "AVERAGE BALANCE"
        DashboardViewMode.INTAKE -> "AVERAGE INTAKE"
        DashboardViewMode.EXPENDITURE -> "AVERAGE EXPENDITURE"
    }
    val avgValue = if (isBalance) metrics.avgDailyBalance
        else if (balances.isEmpty()) 0.0 else balances.sumOf { viewMode.valueOf(it) } / balances.size
    val totalValue = if (isBalance) metrics.totalBalance else balances.sumOf { viewMode.valueOf(it) }

    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = headlineLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MiTextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${formatCalorieValue(avgValue)} kcal",
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isBalance) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(trendColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (metrics.trend == com.mettyoung.fitbro.data.model.TrendDirection.IMPROVING)
                                Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = trendColor,
                            modifier = if (metrics.trend != com.mettyoung.fitbro.data.model.TrendDirection.STABLE)
                                Modifier.size(24.dp) else Modifier.size(0.dp)
                        )
                        if (metrics.trend == com.mettyoung.fitbro.data.model.TrendDirection.STABLE) {
                            Box(Modifier.size(12.dp, 2.dp).background(trendColor))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            CalorieBalanceChart(
                balances = balances,
                onBarClick = onBarClick,
                modifier = Modifier.fillMaxWidth().height(160.dp),
                valueSelector = { viewMode.valueOf(it) },
                diverging = viewMode.isDiverging
            )

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Total", style = MaterialTheme.typography.labelSmall, color = MiTextSecondary)
                    Text("${formatCalorieValue(totalValue)} kcal", style = MaterialTheme.typography.titleMedium)
                }
                if (isBalance) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Trend", style = MaterialTheme.typography.labelSmall, color = MiTextSecondary)
                        Text(
                            metrics.trend.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            color = trendColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CondensedLogItem(
    balance: DailyBalance,
    viewMode: DashboardViewMode,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(48.dp)) {
            Text(
                text = balance.date.toDayAbbr(),
                style = MaterialTheme.typography.labelSmall,
                color = MiTextSecondary
            )
            Text(
                text = balance.date.split("-").last(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            when (viewMode) {
                DashboardViewMode.INTAKE -> {
                    LogDotRow(ColorCarbs, "${balance.carbG.roundToInt()}g carbs")
                    Spacer(Modifier.height(4.dp))
                    LogDotRow(ColorProtein, "${balance.proteinG.roundToInt()}g protein")
                    Spacer(Modifier.height(4.dp))
                    LogDotRow(ColorFat, "${balance.fatG.roundToInt()}g fat")
                }
                DashboardViewMode.EXPENDITURE -> {
                    LogDotRow(ColorProtein, "${balance.bmr.roundToInt()} BMR")
                    Spacer(Modifier.height(4.dp))
                    LogDotRow(ColorCarbs, "${balance.tef.roundToInt()} TEF")
                    Spacer(Modifier.height(4.dp))
                    LogDotRow(ColorFat, "${(balance.neat + balance.eat).roundToInt()} Active")
                }
                else -> {
                    LogDotRow(MiOrange, "${balance.intake.roundToInt()} in")
                    Spacer(Modifier.height(4.dp))
                    LogDotRow(
                        MaterialTheme.colorScheme.tertiary,
                        "${balance.burn.roundToInt()} out",
                        textColor = MiTextSecondary
                    )
                }
            }
        }

        when (viewMode) {
            DashboardViewMode.INTAKE -> {
                Text(
                    text = "${balance.intake.roundToInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            DashboardViewMode.EXPENDITURE -> {
                Text(
                    text = "${balance.burn.roundToInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            else -> {
                val balanceColor = if (balance.balance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                Text(
                    text = "${if (balance.balance >= 0) "+" else ""}${balance.balance.roundToInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    color = balanceColor
                )
            }
        }
    }
}

@Composable
private fun LogDotRow(
    dotColor: Color,
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = textColor)
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
    return listOf("SAT", "SUN", "MON", "TUE", "WED", "THU", "FRI")[h]
}

private fun formatCalorieValue(value: Double): String {
    val v = value.toInt()
    return if (abs(v) >= 1000) "${v / 1000}.${(abs(v) % 1000) / 100}k" else v.toString()
}

private fun suggestAction(errorMessage: String): String = when {
    "Permission denied" in errorMessage -> "Suggestion: Grant Health Connect permission in Settings"
    "Network" in errorMessage -> "Suggestion: Check your internet connection"
    "Rate limited" in errorMessage -> "Suggestion: Wait a moment and try again"
    else -> "Suggestion: Try again later"
}

@Composable
private fun SyncStatusBar(state: DashboardState) {
    val isLoading = state.uiState is DashboardUiState.Loading
    val latestSyncMs = state.lastSyncTime.values.filterNotNull().maxOrNull()

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val statusText = when {
            isLoading -> "Syncing your data..."
            latestSyncMs != null -> "Updated ${formatTimeAgo(latestSyncMs)}"
            else -> "Ready to sync"
        }
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelSmall,
            color = MiTextSecondary.copy(alpha = 0.5f)
        )
    }
}
