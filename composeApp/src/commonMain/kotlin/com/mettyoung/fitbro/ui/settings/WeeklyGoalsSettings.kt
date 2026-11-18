package com.mettyoung.fitbro.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.ui.TextSecondary
import com.mettyoung.fitbro.ui.components.SectionLabel

/** FR-6: Dashboard weekly targets. */
@Composable
fun WeeklyGoalsSettings(
    resistanceTarget: Int,
    cardioMinutesTarget: Int,
    onResistanceChange: (Int) -> Unit,
    onCardioChange: (Int) -> Unit
) {
    Spacer(Modifier.height(20.dp))
    SectionLabel("Weekly goals")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            StepperRow("Resistance sessions", "Strength workouts per week", "$resistanceTarget",
                onMinus = { onResistanceChange(resistanceTarget - 1) }, onPlus = { onResistanceChange(resistanceTarget + 1) })
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            StepperRow("Cardio minutes", "Minutes per week", "$cardioMinutesTarget",
                onMinus = { onCardioChange(cardioMinutesTarget - 10) }, onPlus = { onCardioChange(cardioMinutesTarget + 10) })
        }
    }
}

@Composable
private fun StepperRow(title: String, subtitle: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        IconButton(onClick = onMinus) { Icon(Icons.Filled.Remove, contentDescription = "Decrease $title") }
        Text(value, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.widthIn(min = 40.dp))
        IconButton(onClick = onPlus) { Icon(Icons.Filled.Add, contentDescription = "Increase $title") }
    }
}
