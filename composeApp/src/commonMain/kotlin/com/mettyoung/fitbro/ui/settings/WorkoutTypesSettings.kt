package com.mettyoung.fitbro.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.mettyoung.fitbro.data.model.WorkoutCategory
import com.mettyoung.fitbro.data.model.WorkoutType
import com.mettyoung.fitbro.ui.MiTextSecondary
import com.mettyoung.fitbro.ui.components.SectionLabel
import com.mettyoung.fitbro.ui.dashboard.CardioStateHolder

/** US-003: Settings section; opens [WorkoutTypesSheet]. */
@Composable
fun WorkoutTypesSettings(cardioStateHolder: CardioStateHolder) {
    val state by cardioStateHolder.state.collectAsState()
    var showSheet by remember { mutableStateOf(false) }

    Spacer(Modifier.height(20.dp))
    SectionLabel("Cardio")
    Card(
        modifier = Modifier.fillMaxWidth().clickable { showSheet = true },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Workout types",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${state.hiddenTypes.size} of ${WorkoutType.entries.size} types hidden from the Cardio tab",
                style = MaterialTheme.typography.bodySmall,
                color = MiTextSecondary
            )
        }
    }

    if (showSheet) WorkoutTypesSheet(cardioStateHolder, onDismiss = { showSheet = false })
}

/** Every [WorkoutType] grouped by category, switch on = shown on the Cardio tab. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutTypesSheet(cardioStateHolder: CardioStateHolder, onDismiss: () -> Unit) {
    val state by cardioStateHolder.state.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val grouped = remember { WorkoutType.entries.groupBy { it.category } }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Cardio workout types", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Hidden types don't show on the Cardio tab or count toward cardio minutes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MiTextSecondary
                )
            }
            TextButton(onClick = cardioStateHolder::resetHiddenTypes) { Text("Reset") }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            WorkoutCategory.entries.forEach { category ->
                val types = grouped[category].orEmpty()
                item(key = category.name) {
                    Spacer(Modifier.height(12.dp))
                    SectionLabel(category.label)
                }
                items(types, key = { it.name }) { type ->
                    val shown = type !in state.hiddenTypes
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { if (shown) cardioStateHolder.hideType(type) else cardioStateHolder.showType(type) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(type.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Switch(
                            checked = shown,
                            onCheckedChange = { on -> if (on) cardioStateHolder.showType(type) else cardioStateHolder.hideType(type) }
                        )
                    }
                }
            }
        }
    }
}
