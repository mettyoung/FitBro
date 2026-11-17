package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.material3.Card
import androidx.compose.foundation.layout.Row
import com.mettyoung.fitbro.ui.components.ScreenGutter
import com.mettyoung.fitbro.ui.components.ScreenHeader
import com.mettyoung.fitbro.ui.components.SectionLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mettyoung.fitbro.data.model.CardioSession
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary
import com.mettyoung.fitbro.util.MONTH_ABBR
import com.mettyoung.fitbro.util.dayOfWeekMonBased
import com.mettyoung.fitbro.util.toShortDate
import com.mettyoung.fitbro.util.toYMD

private val DAY_ABBR = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun CardioScreen(
    stateHolder: CardioStateHolder,
    modifier: Modifier = Modifier
) {
    val state by stateHolder.state.collectAsState()
    val grouped = state.sessions.groupBy { it.date }
    val sortedDates = grouped.keys.sortedDescending()
    val startDate = state.dateRange.startDate
    val endDate = state.dateRange.endDate

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenHeader(
            title = "Cardio",
            overline = "${startDate.toShortDate()} – ${endDate.toShortDate()}"
        )

        Scaffold(
            modifier = Modifier.weight(1f),
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = ScreenGutter,
                    end = ScreenGutter,
                    top = innerPadding.calculateTopPadding() + 4.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp
                )
            ) {
                item {
                    WeeklySummaryCard(totalMinutes = state.totalMinutes, sessionCount = state.sessions.size)
                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = "From Health Connect — workouts recorded by Mi Fitness or any other app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MiTextSecondary
                    )
                    Spacer(Modifier.height(16.dp))
                    if (sortedDates.isNotEmpty()) SectionLabel("Sessions")
                }

                items(sortedDates) { date ->
                    val sessions = grouped[date] ?: return@items
                    SessionDayGroup(
                        date = date,
                        sessions = sessions
                    )
                    Spacer(Modifier.height(12.dp))
                }
                if (sortedDates.isEmpty()) {
                    item {
                        Text(
                            text = when {
                                state.error != null -> state.error ?: ""
                                state.isLoading -> "Loading sessions…"
                                else -> "No exercise sessions in this range. Make sure Mi Fitness shares workouts with Health Connect."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.error != null) MaterialTheme.colorScheme.error else MiTextSecondary,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklySummaryCard(
    totalMinutes: Int,
    sessionCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Row(modifier = Modifier.padding(24.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("ACTIVE MINUTES", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = "$totalMinutes",
                    style = MaterialTheme.typography.displayLarge
                )
            }
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text("SESSIONS", style = MaterialTheme.typography.labelSmall)
                Text(
                    text = "$sessionCount",
                    style = MaterialTheme.typography.displayLarge
                )
            }
        }
    }
}

@Composable
private fun SessionDayGroup(
    date: String,
    sessions: List<CardioSession>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = formatDayLabel(date),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(12.dp))
            sessions.forEachIndexed { index, session ->
                SessionRow(session = session)
                if (index < sessions.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionRow(
    session: CardioSession,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = "${session.activity} · ${session.minutes} min",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = session.source,
            style = MaterialTheme.typography.bodyMedium,
            color = MiTextSecondary
        )
    }
}

private fun formatDayLabel(dateStr: String): String {
    val (y, m, d) = dateStr.toYMD()
    val dow = dayOfWeekMonBased(y, m, d)
    return "${DAY_ABBR[dow]} ${MONTH_ABBR[m]} $d"
}
