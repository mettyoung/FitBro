package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.ui.components.ScreenGutter
import com.mettyoung.fitbro.ui.components.ScreenHeader
import com.mettyoung.fitbro.ui.PositiveColor
import com.mettyoung.fitbro.ui.NegativeColor
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
import com.mettyoung.fitbro.data.model.CardioSession
import com.mettyoung.fitbro.data.model.compareWeeks
import com.mettyoung.fitbro.data.model.comparedDayCount
import com.mettyoung.fitbro.data.model.comparisonDays
import com.mettyoung.fitbro.data.model.minutesOn
import com.mettyoung.fitbro.data.model.partialWeekSpan
import com.mettyoung.fitbro.ui.BackHandlerEffect
import com.mettyoung.fitbro.ui.ColorCarbs
import com.mettyoung.fitbro.ui.ColorFat
import com.mettyoung.fitbro.ui.ColorProtein
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary
import com.mettyoung.fitbro.util.formatTimeAgo
import com.mettyoung.fitbro.util.toShortDate
import com.mettyoung.fitbro.util.todayString
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    stateHolder: DashboardStateHolder,
    cardioSessions: List<CardioSession> = emptyList(),
    previousCardioSessions: List<CardioSession> = emptyList(),
    modifier: Modifier = Modifier
) {
    val state by stateHolder.state.collectAsState()
    DashboardContent(
        state = state,
        onRefresh = { stateHolder.refresh() },
        onPreviousWeek = stateHolder::previousWeek,
        onNextWeek = stateHolder::nextWeek,
        onCurrentWeek = stateHolder::currentWeek,
        onToggleCarbsFat = { stateHolder.setShowCarbsFat(!state.showCarbsFat) },
        cardioSessions = cardioSessions,
        previousCardioSessions = previousCardioSessions,
        modifier = modifier
    )
}

@Composable
fun DashboardContent(
    state: DashboardState,
    onRefresh: () -> Unit,
    onPreviousWeek: () -> Unit = {},
    onNextWeek: () -> Unit = {},
    onCurrentWeek: () -> Unit = {},
    onToggleCarbsFat: () -> Unit = {},
    cardioSessions: List<CardioSession> = emptyList(),
    previousCardioSessions: List<CardioSession> = emptyList(),
    modifier: Modifier = Modifier
) {
    var selectedBreakdown by remember { mutableStateOf<DailyBalance?>(null) }
    var selectedLens: DashboardViewMode? by remember { mutableStateOf(null) }
    var showComparison by remember { mutableStateOf(false) }
    var wasRefreshing by remember { mutableStateOf(false) }
    var showSuccessToast by remember { mutableStateOf(false) }
    var refreshError by remember { mutableStateOf<String?>(null) }

    val inDetail = selectedLens != null || showComparison
    BackHandlerEffect(enabled = inDetail) { selectedLens = null; showComparison = false }

    val today = todayString()
    val week = state.selectedWeek
    val canGoNext = !week.contains(today)
    val weekDays = remember(week) { week.days() }
    // Current week: averages and the week-over-week window cover completed days only (today excluded).
    val averagingDays = remember(week, today) { week.averagingDays(today).toSet() }
    val excludesToday = week.contains(today)
    val comparison = remember(state.weekSummary, state.previousWeekSummary, cardioSessions, previousCardioSessions, today) {
        compareWeeks(
            state.weekSummary.copy(cardioMinutes = cardioSessions.minutesOn(averagingDays)),
            state.previousWeekSummary.copy(cardioMinutes = previousCardioSessions.minutesOn(comparisonDays(week, today)))
        )
    }
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
            ScreenHeader(
                title = when {
                    showComparison -> "Vs last week"
                    else -> selectedLens?.label ?: "Trends"
                },
                overline = if (inDetail) "Trends" else "Energy balance",
                navigation = if (inDetail) {
                    {
                        IconButton(onClick = { selectedLens = null; showComparison = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                } else null,
                actions = {
                    if (isLoading && wasRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { wasRefreshing = true; onRefresh() }, enabled = !isLoading) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = MiTextSecondary)
                        }
                    }
                },
                bottom = {
                // Compact date navigator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPreviousWeek,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week", modifier = Modifier.size(20.dp), tint = MiTextSecondary)
                    }
                    // US-001: tap the label to jump back to the current week.
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClick = onCurrentWeek).padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = week.label(today, state.weekStart),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${week.start.toShortDate()} – ${week.end.toShortDate()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MiTextSecondary
                        )
                    }
                    IconButton(
                        onClick = onNextWeek,
                        enabled = canGoNext,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next week",
                            modifier = Modifier.size(20.dp),
                            tint = if (canGoNext) MiTextSecondary else MiTextSecondary.copy(alpha = 0.3f)
                        )
                    }
                }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = ScreenGutter)
            ) {
                Spacer(Modifier.height(8.dp))

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
                        if (showComparison) {
                            WeekComparisonPage(
                                rows = comparison,
                                loggedDays = state.weekSummary.loggedDays,
                                comparedDays = comparedDayCount(week, today),
                                partialSpan = partialWeekSpan(week, today, state.weekStart),
                                showCarbsFat = state.showCarbsFat,
                                onToggleCarbsFat = onToggleCarbsFat,
                                days = weekDays,
                                balances = uiState.balances,
                                averagingDays = averagingDays,
                                previousBalances = state.previousWeekBalances,
                                previousDays = remember(week) { week.previous().days() }
                            )
                        } else if (selectedLens == null) {
                            DashboardHome(
                                balances = uiState.balances,
                                days = weekDays,
                                averagingDays = averagingDays,
                                excludesToday = excludesToday,
                                onBarClick = { selectedBreakdown = it },
                                onIntakeClick = { selectedLens = DashboardViewMode.INTAKE },
                                onExpenditureClick = { selectedLens = DashboardViewMode.EXPENDITURE },
                                comparisonCard = {
                                    WeekComparisonTeaser(
                                        rows = comparison,
                                        loggedDays = state.weekSummary.loggedDays,
                                        comparedDays = comparedDayCount(week, today),
                                        partialSpan = partialWeekSpan(week, today, state.weekStart),
                                        onClick = { showComparison = true }
                                    )
                                }
                            )
                        } else {
                            DashboardDetail(
                                balances = uiState.balances,
                                days = weekDays,
                                averagingDays = averagingDays,
                                excludesToday = excludesToday,
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

}

@Composable
private fun DashboardHome(
    balances: List<DailyBalance>,
    days: List<String>,
    averagingDays: Set<String>,
    excludesToday: Boolean,
    onBarClick: (DailyBalance) -> Unit,
    onIntakeClick: () -> Unit,
    onExpenditureClick: () -> Unit,
    comparisonCard: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SlidingWindowInsightCard(
            balances = balances,
            days = days,
            averagingDays = averagingDays,
            excludesToday = excludesToday,
            viewMode = DashboardViewMode.BALANCE,
            onBarClick = onBarClick,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryMetricCard(
                viewMode = DashboardViewMode.INTAKE,
                balances = balances.filter { it.date in averagingDays },
                onClick = onIntakeClick,
                modifier = Modifier.weight(1f)
            )
            SummaryMetricCard(
                viewMode = DashboardViewMode.EXPENDITURE,
                balances = balances.filter { it.date in averagingDays },
                onClick = onExpenditureClick,
                modifier = Modifier.weight(1f)
            )
        }

        comparisonCard()
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
    val logged = balances.filter { it.intake > 0 }
    val avgValue = if (logged.isEmpty()) 0.0 else logged.sumOf { viewMode.valueOf(it) } / logged.size
    val netBalance = logged.sumOf { it.balance }.roundToInt()

    val valueColor = when (viewMode) {
        DashboardViewMode.BALANCE     -> if (netBalance >= 0) PositiveColor else NegativeColor
        DashboardViewMode.INTAKE      -> PositiveColor
        DashboardViewMode.EXPENDITURE -> NegativeColor
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(if (isBig) 20.dp else 16.dp)) {
            Text(
                text = viewMode.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MiTextSecondary
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = if (logged.isEmpty()) "—" else "${formatCalorieValue(avgValue)} kcal",
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
    days: List<String>,
    averagingDays: Set<String>,
    excludesToday: Boolean,
    viewMode: DashboardViewMode,
    onBarClick: (DailyBalance) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        SlidingWindowInsightCard(
            balances = balances,
            days = days,
            averagingDays = averagingDays,
            excludesToday = excludesToday,
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
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
    days: List<String>,
    averagingDays: Set<String>,
    excludesToday: Boolean,
    viewMode: DashboardViewMode,
    onBarClick: (DailyBalance) -> Unit,
    modifier: Modifier = Modifier
) {
    // Averages/trend over logged days only, matching the week-over-week card (FR-5).
    val logged = remember(balances, averagingDays) { balances.filter { it.intake > 0 && it.date in averagingDays } }
    val metrics = remember(logged) { com.mettyoung.fitbro.data.model.calculateWindowMetrics(logged) }
    val trendColor = when (metrics.trend) {
        com.mettyoung.fitbro.data.model.TrendDirection.IMPROVING -> PositiveColor
        com.mettyoung.fitbro.data.model.TrendDirection.DECLINING -> NegativeColor
        com.mettyoung.fitbro.data.model.TrendDirection.STABLE -> MiOrange
    }

    val isBalance = viewMode == DashboardViewMode.BALANCE
    val headlineLabel = when (viewMode) {
        DashboardViewMode.BALANCE -> "AVERAGE BALANCE"
        DashboardViewMode.INTAKE -> "AVERAGE INTAKE"
        DashboardViewMode.EXPENDITURE -> "AVERAGE EXPENDITURE"
    }
    val avgValue = if (isBalance) metrics.avgDailyBalance
        else if (logged.isEmpty()) 0.0 else logged.sumOf { viewMode.valueOf(it) } / logged.size
    val totalValue = if (isBalance) metrics.totalBalance else logged.sumOf { viewMode.valueOf(it) }

    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
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
                        text = headlineLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MiTextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (logged.isEmpty()) "—" else "${formatCalorieValue(avgValue)} kcal",
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (excludesToday) {
                        Text(
                            text = "Completed days only · today excluded",
                            style = MaterialTheme.typography.labelSmall,
                            color = MiTextSecondary
                        )
                    }
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
                diverging = viewMode.isDiverging,
                days = days
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
                val balanceColor = if (balance.balance >= 0) PositiveColor else NegativeColor
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

internal fun String.toDayAbbr(): String {
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
