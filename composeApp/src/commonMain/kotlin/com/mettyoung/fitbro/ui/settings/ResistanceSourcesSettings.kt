package com.mettyoung.fitbro.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.ui.MiTextSecondary
import com.mettyoung.fitbro.ui.components.SectionLabel
import com.mettyoung.fitbro.ui.dashboard.CardioStateHolder

/** Settings section: which apps' strength sessions count toward Resistance. */
@Composable
fun ResistanceSourcesSettings(cardioStateHolder: CardioStateHolder) {
    val state by cardioStateHolder.state.collectAsState()
    var showSheet by remember { mutableStateOf(false) }

    Spacer(Modifier.height(20.dp))
    SectionLabel("Resistance")
    Card(
        modifier = Modifier.fillMaxWidth().clickable { showSheet = true },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Sources",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = resistanceSourcesSummary(state.resistanceSources, state.excludedResistanceSources),
                style = MaterialTheme.typography.bodySmall,
                color = MiTextSecondary
            )
        }
    }

    if (showSheet) {
        ResistanceSourcesSheet(
            sources = state.resistanceSources,
            excluded = state.excludedResistanceSources,
            onToggle = cardioStateHolder::setResistanceSourceIncluded,
            onDismiss = { showSheet = false }
        )
    }
}

fun resistanceSourcesSummary(sources: List<String>, excluded: Set<String>): String {
    val counted = sources.filter { it !in excluded }
    return when {
        sources.isEmpty() -> "All apps · no strength workouts found yet"
        counted.isEmpty() -> "No apps counted"
        counted.size == sources.size -> "Counting all: ${counted.joinToString()}"
        else -> "Counting ${counted.joinToString()}"
    }
}

/** One switch per app that wrote strength sessions; on = counts toward Resistance. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResistanceSourcesSheet(
    sources: List<String>,
    excluded: Set<String>,
    onToggle: (source: String, included: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding()) {
            Text("Resistance sources", style = MaterialTheme.typography.titleLarge)
            Text(
                "Turn off an app to stop its strength workouts counting toward Resistance — e.g. when Hevy and your watch both record the same session.",
                style = MaterialTheme.typography.bodySmall,
                color = MiTextSecondary
            )
            Spacer(Modifier.height(12.dp))
            if (sources.isEmpty()) {
                Text(
                    "No strength workouts in Health Connect for these two weeks yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MiTextSecondary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
            sources.forEach { source ->
                val included = source !in excluded
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onToggle(source, !included) }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(source, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = included, onCheckedChange = { onToggle(source, it) })
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
