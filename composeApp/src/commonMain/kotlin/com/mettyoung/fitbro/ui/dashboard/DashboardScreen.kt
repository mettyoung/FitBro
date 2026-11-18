package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.ui.components.ScreenGutter
import com.mettyoung.fitbro.ui.components.ScreenHeader
import com.mettyoung.fitbro.ui.components.DetailHeader
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
import com.mettyoung.fitbro.data.model.partialWeekSpan
import com.mettyoung.fitbro.data.model.BodyMetric
import com.mettyoung.fitbro.util.toShortDate
import com.mettyoung.fitbro.data.model.CardioSession
import com.mettyoung.fitbro.data.model.WeekGoals
import com.mettyoung.fitbro.data.model.WeekMetric
import com.mettyoung.fitbro.data.model.WeekSummary
import com.mettyoung.fitbro.data.model.minutesOn
import androidx.compose.material.icons.filled.CheckCircle
import com.mettyoung.fitbro.ui.BackHandlerEffect
import com.mettyoung.fitbro.ui.ColorCarbs
import com.mettyoung.fitbro.ui.ColorFat
import com.mettyoung.fitbro.ui.ColorProtein
import com.mettyoung.fitbro.ui.BrandPrimary
import com.mettyoung.fitbro.ui.TextSecondary
import com.mettyoung.fitbro.util.formatTimeAgo
import com.mettyoung.fitbro.util.todayString
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    stateHolder: DashboardStateHolder,
    cardioSessions: List<CardioSession> = emptyList(),
    resistanceCount: Int = 0,
    strengthSessions: List<CardioSession> = emptyList(),
    workoutDetail: WorkoutDetailState? = null,
    onOpenWorkout: (CardioSession) -> Unit = {},
    onCloseWorkout: () -> Unit = {},
    resistanceSources: List<String> = emptyList(),
    excludedResistanceSources: Set<String> = emptySet(),
    onToggleResistanceSource: (String, Boolean) -> Unit = { _, _ -> },
    onOpenCardio: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by stateHolder.state.collectAsState()
    DashboardContent(
        state = state,
        onRefresh = { stateHolder.refresh() },
        onPreviousWeek = stateHolder::previousWeek,
        onNextWeek = stateHolder::nextWeek,
        onCurrentWeek = stateHolder::currentWeek,
        cardioSessions = cardioSessions,
        resistanceCount = resistanceCount,
        strengthSessions = strengthSessions,
        workoutDetail = workoutDetail,
        onOpenWorkout = onOpenWorkout,
        onCloseWorkout = onCloseWorkout,
        resistanceSources = resistanceSources,
        excludedResistanceSources = excludedResistanceSources,
        onToggleResistanceSource = onToggleResistanceSource,
        onOpenCardio = onOpenCardio,
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
    cardioSessions: List<CardioSession> = emptyList(),
    resistanceCount: Int = 0,
    strengthSessions: List<CardioSession> = emptyList(),
    workoutDetail: WorkoutDetailState? = null,
    onOpenWorkout: (CardioSession) -> Unit = {},
    onCloseWorkout: () -> Unit = {},
    resistanceSources: List<String> = emptyList(),
    excludedResistanceSources: Set<String> = emptySet(),
    onToggleResistanceSource: (String, Boolean) -> Unit = { _, _ -> },
    onOpenCardio: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedBreakdown by remember { mutableStateOf<DailyBalance?>(null) }
    var page: DashboardPage? by remember { mutableStateOf(null) }
    var wasRefreshing by remember { mutableStateOf(false) }
    var showSuccessToast by remember { mutableStateOf(false) }
    var refreshError by remember { mutableStateOf<String?>(null) }

    val inDetail = page != null
    // Workout detail sits under Resistance; everything else goes straight back to the Dashboard.
    val goBack = {
        if (page == DashboardPage.Workout) {
            onCloseWorkout()
            page = DashboardPage.Resistance
        } else page = null
    }
    BackHandlerEffect(enabled = inDetail) { goBack() }

    val today = todayString()
    val week = state.selectedWeek
    val weekDays = remember(week) { week.days() }
    // Current week: averages and the week-over-week window cover completed days only (today excluded).
    val averagingDays = remember(week, today) { week.averagingDays(today).toSet() }
    val excludesToday = week.contains(today)
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
            val weekRange = "${week.start.toShortDate()} – ${week.end.toShortDate()}"
            val bodyWindow = state.body.trends[BodyMetric.WEIGHT]?.windowDays
                ?.let { d -> if (d.isEmpty()) null else "${d.first().toShortDate()} – ${d.last().toShortDate()}" }
            when (val p = page) {
                null -> ScreenHeader(
                    title = "Dashboard",
                    actions = {
                        if (isLoading && wasRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = { wasRefreshing = true; onRefresh() }, enabled = !isLoading) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = TextSecondary)
                            }
                        }
                    },
                    bottom = {
                        WeekStepper(
                            week = week,
                            today = today,
                            weekStart = state.weekStart,
                            onPreviousWeek = onPreviousWeek,
                            onNextWeek = onNextWeek,
                            onCurrentWeek = onCurrentWeek
                        )
                    }
                )
                else -> DetailHeader(
                    title = when (p) {
                        is DashboardPage.Lens -> p.mode.label
                        is DashboardPage.Body -> p.metric.shortLabel
                        DashboardPage.Estimate -> "Expenditure"
                        DashboardPage.Resistance -> "Resistance"
                        DashboardPage.Workout -> workoutDetail?.session?.activity ?: "Workout"
                    },
                    subtitle = when (p) {
                        is DashboardPage.Lens, DashboardPage.Resistance -> weekRange
                        is DashboardPage.Body, DashboardPage.Estimate -> bodyWindow
                        DashboardPage.Workout -> workoutDetail?.session?.date?.let { "${it.toDayAbbr().lowercase().replaceFirstChar { c -> c.uppercase() }}, ${it.toShortDate()}" }
                    },
                    onBack = goBack
                )
            }

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
                            CircularProgressIndicator(color = BrandPrimary)
                        }
                    }
                    is DashboardUiState.Success -> {
                        when (val p = page) {
                            null -> DashboardHome(
                                balances = uiState.balances,
                                days = weekDays,
                                averagingDays = averagingDays,
                                weekToDate = state.weekToDate,
                                balanceCaption = balanceCaption(state, today),
                                summary = state.weekSummary,
                                goals = state.weekGoals,
                                resistanceCount = resistanceCount,
                                resistanceTarget = state.resistanceTarget,
                                cardioMinutes = cardioSessions.minutesOn(weekDays),
                                cardioSessions = cardioSessions.size,
                                cardioTarget = state.cardioMinutesTarget,
                                body = state.body,
                                onOpen = { page = it },
                                onOpenCardio = onOpenCardio,
                                onBarClick = { selectedBreakdown = it }
                            )
                            is DashboardPage.Lens -> DashboardDetail(
                                balances = uiState.balances,
                                days = weekDays,
                                averagingDays = averagingDays,
                                excludesToday = excludesToday,
                                viewMode = p.mode
                            )
                            is DashboardPage.Body -> BodyTrendDetail(state.body.trends[p.metric], p.metric, state.body.access)
                            DashboardPage.Resistance -> ResistanceDetail(
                                sessions = strengthSessions,
                                target = state.resistanceTarget,
                                sources = resistanceSources,
                                excludedSources = excludedResistanceSources,
                                onToggleSource = onToggleResistanceSource,
                                onOpen = {
                                    onOpenWorkout(it)
                                    page = DashboardPage.Workout
                                }
                            )
                            DashboardPage.Workout -> WorkoutDetailPage(workoutDetail)
                            DashboardPage.Estimate -> EstimateDetail(
                                estimate = state.body.estimate,
                                healthAvgBurn = state.weekSummary.avgBurn,
                                windowEnd = state.body.trends[BodyMetric.WEIGHT]?.windowDays?.lastOrNull()
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
private fun DashboardDetail(
    balances: List<DailyBalance>,
    days: List<String>,
    averagingDays: Set<String>,
    excludesToday: Boolean,
    viewMode: DashboardViewMode,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        MetricDetailHeader(
            balances = balances,
            days = days,
            averagingDays = averagingDays,
            excludesToday = excludesToday,
            viewMode = viewMode,
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
                color = TextSecondary
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
                        viewMode = viewMode
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
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
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
        Text("Sync successful", color = MaterialTheme.colorScheme.onSecondary, style = MaterialTheme.typography.labelSmall)
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
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK", color = BrandPrimary, fontWeight = FontWeight.Bold) }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
private fun CondensedLogItem(
    balance: DailyBalance,
    viewMode: DashboardViewMode
) {
    // History rows are read-only: they already show the day's split.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(48.dp)) {
            Text(
                text = balance.date.toDayAbbr(),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
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
                    LogDotRow(BrandPrimary, "${balance.intake.roundToInt()} in")
                    Spacer(Modifier.height(4.dp))
                    LogDotRow(
                        MaterialTheme.colorScheme.tertiary,
                        "${balance.burn.roundToInt()} out",
                        textColor = TextSecondary
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
            color = TextSecondary.copy(alpha = 0.5f)
        )
    }
}

/** US-002 caption: "Mon–Tue · avg −620/day", "Full week · avg …", or why there's no number yet. */
private fun balanceCaption(state: DashboardState, today: String): String {
    val week = state.selectedWeek
    val wtd = state.weekToDate
    val span = partialWeekSpan(week, today, state.weekStart) ?: "Full week"
    return when {
        week.averagingDays(today).isEmpty() -> "No completed days yet"
        wtd == null -> "$span · no days logged"
        else -> "$span · ${wtd.loggedDays} logged · avg ${(if (wtd.average > 0) "+" else "") + wtd.average.grouped()}/day"
    }
}
