package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mettyoung.fitbro.ui.settings.ResistanceSourcesSheet
import com.mettyoung.fitbro.ui.settings.resistanceSourcesSummary
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.model.CardioSession
import com.mettyoung.fitbro.data.model.Measured
import com.mettyoung.fitbro.data.model.WorkoutDetail
import com.mettyoung.fitbro.data.model.endEpochMs
import com.mettyoung.fitbro.ui.ColorBurn
import com.mettyoung.fitbro.ui.BrandPrimary
import com.mettyoung.fitbro.ui.TextSecondary
import com.mettyoung.fitbro.util.formatLocalTime
import com.mettyoung.fitbro.util.toShortDate
import kotlin.math.roundToInt

/** Resistance drill-in: the week's strength sessions (any app), each opening its workout detail. */
@Composable
internal fun ResistanceDetail(
    sessions: List<CardioSession>,
    target: Int,
    sources: List<String>,
    excludedSources: Set<String>,
    onToggleSource: (String, Boolean) -> Unit,
    onOpen: (CardioSession) -> Unit
) {
    var showSources by remember { mutableStateOf(false) }
    if (showSources) {
        ResistanceSourcesSheet(sources, excludedSources, onToggleSource, onDismiss = { showSources = false })
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SurfaceCard {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${sessions.size}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(6.dp))
                    Text("/ $target sessions", style = MaterialTheme.typography.titleMedium, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                }
                Text(
                    "${sessions.sumOf { it.minutes }} min · strength sessions from Health Connect",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp).clickable { showSources = true }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        resistanceSourcesSummary(sources, excludedSources),
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (sessions.isEmpty()) {
            Text(
                "No strength sessions this week. Workouts from Hevy or Mi Fitness show up here once they sync to Health Connect.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
            return
        }

        SurfaceCard {
            Column {
                sessions.forEachIndexed { i, s ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onOpen(s) }.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(ColorBurn), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.activity, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${s.date.toDayAbbr().titleCase()} ${s.date.toShortDate()} · ${formatLocalTime(s.startEpochMs)} · ${s.minutes} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        SourceChip(s.source)
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
                    }
                    if (i < sessions.lastIndex) Divider()
                }
            }
        }
    }
}

/** Every field read from Health Connect for one workout, with the app that wrote it. */
@Composable
internal fun WorkoutDetailPage(state: WorkoutDetailState?) {
    when (state) {
        null -> Unit
        is WorkoutDetailState.Loading -> Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrandPrimary)
        }
        is WorkoutDetailState.Failed -> Text(
            "Couldn't read this workout: ${state.message}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        is WorkoutDetailState.Loaded -> LoadedWorkout(state.detail, state.overlapping)
    }
}

@Composable
private fun LoadedWorkout(d: WorkoutDetail, overlapping: List<CardioSession>) {
    val s = d.session
    val app = s.source
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Hero
        SurfaceCard {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${s.minutes}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(6.dp))
                    Text("min", style = MaterialTheme.typography.titleMedium, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
                }
                Text(
                    "${s.date.toDayAbbr().titleCase()} ${s.date.toShortDate()} · ${formatLocalTime(s.startEpochMs)}–${formatLocalTime(d.endEpochMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        Section("Session") {
            DetailRow("Workout type", s.type.label)
            DetailRow("Title", d.title ?: "—")
            DetailRow("Start", formatLocalTime(s.startEpochMs))
            DetailRow("End", formatLocalTime(d.endEpochMs))
            DetailRow("Duration", "${s.minutes} min")
            DetailRow("Recording", d.recordingMethod ?: "Not specified")
            DetailRow("Device", d.device ?: "Not specified")
            DetailRow("Laps", if (d.laps > 0) "${d.laps}" else "None")
            DetailRow("Route", if (d.hasRoute) "Recorded" else "None")
            DetailRow("Notes", d.notes ?: "—", last = true)
        }

        Section("During the workout") {
            MeasuredRow("Active calories", d.activeKcal, "None recorded") { "${it.roundToInt()} kcal" }
            when {
                !d.heartRateAllowed -> DetailRow("Heart rate", "Not allowed — enable Heart rate for FitBro in Health Connect", last = true)
                d.heartRate == null -> DetailRow("Heart rate", "No readings in this window", last = true)
                else -> {
                    val hr = d.heartRate
                    DetailRow("Avg heart rate", "${hr.value.avgBpm} bpm")
                    DetailRow("Min / max", "${hr.value.minBpm} / ${hr.value.maxBpm} bpm")
                    DetailRow("Readings", "${hr.value.samples}", last = true)
                }
            }
        }

        Section("Exercises") {
            if (d.segments.isEmpty()) {
                DetailRow("Exercises", "None — $app doesn't send sets or reps to Health Connect", last = true)
            } else {
                d.segments.forEachIndexed { i, seg ->
                    val mins = ((seg.endEpochMs - seg.startEpochMs) / 60_000.0).roundToInt()
                    DetailRow(
                        seg.name,
                        listOfNotNull(seg.repetitions.takeIf { it > 0 }?.let { "$it reps" }, "$mins min").joinToString(" · "),
                        last = i == d.segments.lastIndex
                    )
                }
            }
        }

        if (overlapping.isNotEmpty()) {
            Section("Also recorded at this time") {
                overlapping.forEachIndexed { i, o ->
                    DetailRow(
                        o.activity,
                        "${o.source} · ${formatLocalTime(o.startEpochMs)}–${formatLocalTime(o.endEpochMs)} · ${o.minutes} min",
                        last = i == overlapping.lastIndex
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> MeasuredRow(label: String, m: Measured<T>?, missing: String, format: (T) -> String) {
    DetailRow(label, m?.let { format(it.value) } ?: missing)
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp, top = 4.dp)
        )
        SurfaceCard { Column { content() } }
    }
}

@Composable
private fun DetailRow(label: String, value: String, last: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.width(120.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
    if (!last) Divider()
}

@Composable
private fun SourceChip(source: String) {
    Text(
        source,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        maxLines = 1,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun SurfaceCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) { content() }
}

@Composable
private fun Divider() = HorizontalDivider(
    modifier = Modifier.padding(horizontal = 16.dp),
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
)

private fun String.titleCase() = lowercase().replaceFirstChar { it.uppercase() }
