package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.util.MONTH_ABBR
import com.mettyoung.fitbro.util.dayOfWeekMonBased
import com.mettyoung.fitbro.util.daysInMonth
import com.mettyoung.fitbro.util.plusDays
import com.mettyoung.fitbro.util.todayString
import com.mettyoung.fitbro.util.toYMD

@Composable
fun DatePickerDialog(
    initialStartDate: String,
    onDismiss: () -> Unit,
    onDateRangeSelected: (DateRange) -> Unit
) {
    val today = todayString()
    val (iy, im, _) = initialStartDate.toYMD()
    var displayYear by remember { mutableStateOf(iy) }
    var displayMonth by remember { mutableStateOf(im) }
    var selectedStart by remember { mutableStateOf(initialStartDate) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Select Week",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Month navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (displayMonth == 1) { displayYear--; displayMonth = 12 }
                        else displayMonth--
                    }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Prev", tint = MiOrange)
                    }
                    
                    Text(
                        text = "${MONTH_ABBR[displayMonth]} $displayYear",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    val canGoNext = run {
                        val nextYear = if (displayMonth == 12) displayYear + 1 else displayYear
                        val nextMonth = if (displayMonth == 12) 1 else displayMonth + 1
                        "$nextYear-${nextMonth.toString().padStart(2, '0')}-01" <= today
                    }
                    
                    IconButton(
                        onClick = {
                            if (displayMonth == 12) { displayYear++; displayMonth = 1 }
                            else displayMonth++
                        },
                        enabled = canGoNext
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowRight, 
                            contentDescription = "Next", 
                            tint = if (canGoNext) MiOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Day of week headers
                val dowHeaders = listOf("M", "T", "W", "T", "F", "S", "S")
                Row(modifier = Modifier.fillMaxWidth()) {
                    dowHeaders.forEach { label ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Day grid
                val firstOffset = dayOfWeekMonBased(displayYear, displayMonth, 1)
                val daysCount = daysInMonth(displayYear, displayMonth)
                val selectedEnd = selectedStart.plusDays(6)
                val totalCells = firstOffset + daysCount
                val weeks = (totalCells + 6) / 7

                repeat(weeks) { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        repeat(7) { dow ->
                            val cell = week * 7 + dow
                            val dayNum = cell - firstOffset + 1
                            Box(modifier = Modifier.weight(1f)) {
                                if (dayNum in 1..daysCount) {
                                    val dayStr = "$displayYear-${displayMonth.toString().padStart(2,'0')}-${dayNum.toString().padStart(2,'0')}"
                                    val isFuture = dayStr > today
                                    val isStart = dayStr == selectedStart
                                    val isEnd = dayStr == selectedEnd
                                    val inRange = dayStr in selectedStart..selectedEnd

                                    val bgColor = when {
                                        isStart || isEnd -> MiOrange
                                        inRange -> MiOrange.copy(alpha = 0.1f)
                                        else -> Color.Transparent
                                    }
                                    val textColor = when {
                                        isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                        isStart || isEnd -> Color.White
                                        inRange -> MiOrange
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }

                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(bgColor)
                                            .then(
                                                if (!isFuture) Modifier.clickable { selectedStart = dayStr }
                                                else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (isStart || isEnd || inRange) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            ),
                                            color = textColor,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Confirm / Cancel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(
                        onClick = {
                            onDateRangeSelected(DateRange(selectedStart, selectedStart.plusDays(6)))
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).background(MiOrange.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("OK", color = MiOrange, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
