package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.model.CardioSession
import com.mettyoung.fitbro.data.model.DailyBalance
import com.mettyoung.fitbro.data.model.MetricComparison
import com.mettyoung.fitbro.data.model.WeekMetric
import com.mettyoung.fitbro.data.model.compareWeeks
import com.mettyoung.fitbro.data.model.comparedDayCount
import com.mettyoung.fitbro.data.model.comparisonDays
import com.mettyoung.fitbro.data.model.minutesOn
import com.mettyoung.fitbro.data.model.partialWeekSpan
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary
import com.mettyoung.fitbro.ui.components.ActivityRings
import com.mettyoung.fitbro.ui.components.MetricTileCard
import com.mettyoung.fitbro.ui.components.MiniTicks
import com.mettyoung.fitbro.ui.components.ScreenGutter
import com.mettyoung.fitbro.ui.components.ScreenHeader
import com.mettyoung.fitbro.ui.components.StatItem
import com.mettyoung.fitbro.ui.components.StatTripletCard
import com.mettyoung.fitbro.ui.components.TwoColumnTiles
import com.mettyoung.fitbro.util.todayString

// US-002: week-over-week comparison. Deltas are neutral-coloured (FitBro doesn't know the goal direction).

/** Like-for-like caption. */
private fun comparisonCaption(loggedDays: Int, comparedDays: Int, partialSpan: String?): String = when {
    comparedDays == 0 -> "Starts after today — compares completed days only"
    partialSpan != null -> "$partialSpan vs $partialSpan last week · $loggedDays of $comparedDays days logged"
    else -> "Full week · $loggedDays of 7 days logged"
}

/**
 * Week-over-week rows with cardio minutes filled in from CardioState: current week over its
 * averaging days (today excluded), previous week over the like-for-like comparison days.
 */
@Composable
fun rememberWeekComparison(
    state: DashboardState,
    cardioSessions: List<CardioSession>,
    previousCardioSessions: List<CardioSession>,
    today: String
): List<MetricComparison> {
    val week = state.selectedWeek
    return remember(state.weekSummary, state.previousWeekSummary, cardioSessions, previousCardioSessions, week, today) {
        compareWeeks(
            state.weekSummary.copy(cardioMinutes = cardioSessions.minutesOn(week.averagingDays(today))),
            state.previousWeekSummary.copy(cardioMinutes = previousCardioSessions.minutesOn(comparisonDays(week, today)))
        )
    }
}

/** "Compare" tab: shares the Trends week (same state holder) with its own stepper. */
@Composable
fun WeekComparisonScreen(
    stateHolder: DashboardStateHolder,
    cardioSessions: List<CardioSession>,
    previousCardioSessions: List<CardioSession>,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val state by stateHolder.state.collectAsState()
    val today = todayString()
    val week = state.selectedWeek
    val comparison = rememberWeekComparison(state, cardioSessions, previousCardioSessions, today)
    val averagingDays = remember(week, today) { week.averagingDays(today).toSet() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(
            title = "Vs last week",
            overline = "Compare",
            navigation = com.mettyoung.fitbro.ui.components.backNavigation(onBack),
            bottom = {
                WeekStepper(
                    week = week,
                    today = today,
                    weekStart = state.weekStart,
                    onPreviousWeek = stateHolder::previousWeek,
                    onNextWeek = stateHolder::nextWeek,
                    onCurrentWeek = stateHolder::currentWeek
                )
            }
        )
        Column(modifier = Modifier.padding(horizontal = ScreenGutter)) {
            Spacer(Modifier.height(8.dp))
            when (val uiState = state.uiState) {
                is DashboardUiState.Loading -> Box(
                    modifier = Modifier.height(300.dp).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = MiOrange) }
                is DashboardUiState.Error -> Text(
                    uiState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                is DashboardUiState.Success -> WeekComparisonPage(
                    rows = comparison,
                    loggedDays = state.weekSummary.loggedDays,
                    comparedDays = comparedDayCount(week, today),
                    partialSpan = partialWeekSpan(week, today, state.weekStart),
                    showCarbsFat = state.showCarbsFat,
                    onToggleCarbsFat = { stateHolder.setShowCarbsFat(!state.showCarbsFat) },
                    days = remember(week) { week.days() },
                    balances = uiState.balances,
                    averagingDays = averagingDays,
                    previousBalances = state.previousWeekBalances,
                    previousDays = remember(week) { week.previous().days() },
                    cardioSessions = cardioSessions,
                    previousCardioSessions = previousCardioSessions
                )
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

/**
 * Rings (this week as a share of last week), 3-up stat card, then a 2-column tile grid with a
 * paired mini day-by-day chart per metric.
 */
@Composable
fun WeekComparisonPage(
    rows: List<MetricComparison>,
    loggedDays: Int,
    comparedDays: Int,
    partialSpan: String?,
    showCarbsFat: Boolean,
    onToggleCarbsFat: () -> Unit,
    days: List<String>,
    balances: List<DailyBalance>,
    /** Current-week days that count (today excluded); other days show no current tick. */
    averagingDays: Set<String>,
    previousBalances: List<DailyBalance>,
    previousDays: List<String>,
    cardioSessions: List<CardioSession>,
    previousCardioSessions: List<CardioSession>,
    modifier: Modifier = Modifier
) {
    val byMetric = rows.associateBy { it.metric }
    val styles = metricStyles()
    val ringMetrics = listOf(WeekMetric.AVG_INTAKE, WeekMetric.AVG_PROTEIN, WeekMetric.CARDIO_MINUTES)
    val tiles = WeekMetric.entries.filter { !it.optional || showCarbsFat }
    val dayLabels = days.map { it.toDayAbbr() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ActivityRings(
            progress = ringMetrics.map { ringProgress(byMetric[it]) },
            colors = ringMetrics.map { styles.getValue(it).color },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
        Text(
            "Rings fill to last week's level",
            style = MaterialTheme.typography.labelSmall,
            color = MiTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        StatTripletCard(
            items = ringMetrics.map { m ->
                val style = styles.getValue(m)
                val row = byMetric[m]
                StatItem(style.icon, style.color, style.shortLabel, row?.current.grouped(), "/${row?.previous.grouped()} ${m.unit}")
            },
            footerIcon = Icons.Filled.DateRange,
            footerIconTint = MiOrange,
            footerText = comparisonCaption(loggedDays, comparedDays, partialSpan)
        )
        TwoColumnTiles(tiles) { m, tileModifier ->
            val row = byMetric[m] ?: MetricComparison(m, null, null)
            val style = styles.getValue(m)
            MetricTileCard(
                icon = style.icon,
                color = style.color,
                title = m.label,
                value = formatValue(row.current, m),
                subtitle = "was ${formatValue(row.previous, m)} · ${formatDelta(row.delta, m.unit)}",
                modifier = tileModifier
            ) {
                MiniTicks(
                    current = dailySeries(m, days, balances, cardioSessions) { it in averagingDays },
                    previous = dailySeries(m, previousDays, previousBalances, previousCardioSessions) { true },
                    color = style.color,
                    diverging = m.isSigned,
                    firstLabel = dayLabels.first(),
                    lastLabel = dayLabels.last()
                )
            }
        }
        TextButton(onClick = onToggleCarbsFat, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (showCarbsFat) "Hide carbs & fat" else "Show carbs & fat")
        }
    }
}

/** This week as a share of last week (1.0 = matched). No last-week data → full ring if any this week. */
private fun ringProgress(row: MetricComparison?): Float {
    val cur = row?.current ?: return 0f
    val prev = row.previous
    if (prev == null || prev <= 0.0) return if (cur > 0) 1f else 0f
    return (cur / prev).toFloat()
}
