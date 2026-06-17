package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.mettyoung.fitbro.data.model.CardioSession
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary
import com.mettyoung.fitbro.util.MONTH_ABBR
import com.mettyoung.fitbro.util.dayOfWeekMonBased
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

    var sheetSession by remember { mutableStateOf<CardioSession?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { sheetSession = null; showSheet = true },
                containerColor = MiOrange,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Log cardio", modifier = Modifier.size(24.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 16.dp
            )
        ) {
            item {
                WeeklySummaryCard(weeklyTotalMinutes = state.weeklyTotalMinutes)
                Spacer(Modifier.height(16.dp))
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
    weeklyTotalMinutes: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "This week",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$weeklyTotalMinutes min",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = formatDayLabel(date),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
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
                        modifier = Modifier.padding(vertical = 8.dp),
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
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!session.note.isNullOrBlank()) {
            Text(
                text = session.note,
                style = MaterialTheme.typography.bodySmall,
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
