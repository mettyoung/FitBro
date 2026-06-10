package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary

/** Lens applied to the dashboard Balance screen. Presentation-only, in-memory. */
enum class DashboardViewMode(val label: String) {
    BALANCE("Balance"),
    INTAKE("Intake"),
    EXPENDITURE("Expenditure")
}

/**
 * 3-way segmented toggle styled like the date navigator (rounded pill).
 * Selected segment uses [MiOrange] accent; unselected uses [MiTextSecondary].
 */
@Composable
fun DashboardViewModeToggle(
    selected: DashboardViewMode,
    onSelected: (DashboardViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DashboardViewMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Text(
                text = mode.label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) Color.White else MiTextSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) MiOrange else Color.Transparent)
                    .clickable { onSelected(mode) }
                    .padding(vertical = 10.dp)
            )
        }
    }
}
