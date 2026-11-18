package com.mettyoung.fitbro.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.model.WeekStart
import com.mettyoung.fitbro.ui.BrandPrimary
import com.mettyoung.fitbro.ui.TextSecondary
import com.mettyoung.fitbro.ui.components.SectionLabel

/** US-003: first day of the week for Trends and Cardio. */
@Composable
fun WeekStartSettings(weekStart: WeekStart, onWeekStartChange: (WeekStart) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }

    Spacer(Modifier.height(20.dp))
    SectionLabel("Weeks")
    Card(
        modifier = Modifier.fillMaxWidth().clickable { menuOpen = true },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Week starts on",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Used by Dashboard and Cardio",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Box {
                Text(weekStart.label, style = MaterialTheme.typography.bodyLarge, color = BrandPrimary)
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    WeekStart.entries.forEach { day ->
                        DropdownMenuItem(
                            text = { Text(day.label) },
                            onClick = {
                                menuOpen = false
                                onWeekStartChange(day)
                            }
                        )
                    }
                }
            }
        }
    }
}
