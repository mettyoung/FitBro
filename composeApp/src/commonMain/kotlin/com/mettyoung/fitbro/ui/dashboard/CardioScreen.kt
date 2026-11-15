package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.foundation.layout.Row
import com.mettyoung.fitbro.ui.components.ScreenGutter
import com.mettyoung.fitbro.ui.components.ScreenHeader
import com.mettyoung.fitbro.ui.components.SectionLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    var sheetSession by remember { mutableStateOf<CardioSession?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ScreenHeader(
            title = "Cardio",
            overline = "${startDate.toShortDate()} – ${endDate.toShortDate()}"
        )

        Scaffold(
            modifier = Modifier.weight(1f),
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { sheetSession = null; showSheet = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Log cardio") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = ScreenGutter,
                    end = ScreenGutter,
                    top = innerPadding.calculateTopPadding() + 4.dp,
                    bottom = innerPadding.calculateBottomPadding() + 80.dp
                )
            ) {
                item {
                    WeeklySummaryCard(totalMinutes = state.totalMinutes, sessionCount = state.sessions.size)
                    Spacer(Modifier.height(20.dp))
                    if (sortedDates.isNotEmpty()) SectionLabel("Sessions")
                }

                items(sortedDates) { date ->
                    val sessions = grouped[date] ?: return@items
                    SessionDayGroup(
                        date = date,
                        sessions = sessions,
                        onSessionClick = { s -> sheetSession = s; showSheet = true }
                    )
                    Spacer(Modifier.height(12.dp))
                }
                if (sortedDates.isEmpty()) {
                    item {
                        Text(
                            text = "No sessions in this range yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MiTextSecondary,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }
            }
        }
    }

    if (showSheet) {
        CardioLogSheet(
            session = sheetSession,
            onDismiss = { showSheet = false },
            onSave = { date, minutes, note ->
                val s = sheetSession
                if (s == null) {
                    stateHolder.logSession(date, minutes, note)
                } else {
                    stateHolder.updateSession(s.id, date, minutes, note)
                }
            },
            onDelete = { id -> stateHolder.deleteSession(id) }
        )
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
    onSessionClick: (CardioSession) -> Unit,
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
                SessionRow(
                    session = session,
                    onClick = { onSessionClick(session) }
                )
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "${session.minutes} min",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!session.note.isNullOrBlank()) {
            Text(
                text = session.note,
                style = MaterialTheme.typography.bodyMedium,
                color = MiTextSecondary
            )
        }
    }
}

private fun formatDayLabel(dateStr: String): String {
    val (y, m, d) = dateStr.toYMD()
    val dow = dayOfWeekMonBased(y, m, d)
    return "${DAY_ABBR[dow]} ${MONTH_ABBR[m]} $d"
}
