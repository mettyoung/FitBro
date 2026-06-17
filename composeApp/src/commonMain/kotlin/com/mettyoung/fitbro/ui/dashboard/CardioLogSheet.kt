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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mettyoung.fitbro.data.model.CardioSession
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.util.MONTH_ABBR
import com.mettyoung.fitbro.util.dayOfWeekMonBased
import com.mettyoung.fitbro.util.daysInMonth
import com.mettyoung.fitbro.util.todayString
import com.mettyoung.fitbro.util.toYMD

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardioLogSheet(
    session: CardioSession?,
    onDismiss: () -> Unit,
    onSave: (date: String, minutes: Int, note: String?) -> Unit,
    onDelete: (id: Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEdit = session != null

    var date by remember { mutableStateOf(session?.date ?: todayString()) }
    var minutesText by remember { mutableStateOf(session?.minutes?.toString() ?: "") }
    var note by remember { mutableStateOf(session?.note ?: "") }
    var minutesError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (isEdit) "Edit Session" else "Log Cardio",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(20.dp))

            Box {
                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = minutesText,
                onValueChange = { v ->
                    minutesText = v.filter { it.isDigit() }
                    minutesError = false
                },
                label = { Text("Minutes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = minutesError,
                supportingText = if (minutesError) {
                    { Text("Enter a number greater than 0") }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 2
            )

            Spacer(Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                if (isEdit) {
                    TextButton(onClick = { onDelete(session!!.id); onDismiss() }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        val mins = minutesText.toIntOrNull()
                        if (mins == null || mins <= 0) {
                            minutesError = true
                        } else {
                            onSave(date, mins, note.ifBlank { null })
                            onDismiss()
                        }
                    }
                ) {
                    Text("Save", color = MiOrange)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        SingleDayPickerDialog(
            initialDate = date,
            onDismiss = { showDatePicker = false },
            onDaySelected = { selected ->
                date = selected
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun SingleDayPickerDialog(
    initialDate: String,
    onDismiss: () -> Unit,
    onDaySelected: (String) -> Unit
) {
    val today = todayString()
    val (iy, im, _) = initialDate.toYMD()
    var displayYear by remember { mutableStateOf(iy) }
    var displayMonth by remember { mutableStateOf(im) }
    var selected by remember { mutableStateOf(initialDate) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Select Day",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

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
                        val ny = if (displayMonth == 12) displayYear + 1 else displayYear
                        val nm = if (displayMonth == 12) 1 else displayMonth + 1
                        "$ny-${nm.toString().padStart(2, '0')}-01" <= today
                    }
                    IconButton(onClick = {
                        if (displayMonth == 12) { displayYear++; displayMonth = 1 }
                        else displayMonth++
                    }, enabled = canGoNext) {
                        Icon(
                            Icons.Default.KeyboardArrowRight, contentDescription = "Next",
                            tint = if (canGoNext) MiOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
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

                val firstOffset = dayOfWeekMonBased(displayYear, displayMonth, 1)
                val daysCount = daysInMonth(displayYear, displayMonth)
                val totalCells = firstOffset + daysCount
                val weeks = (totalCells + 6) / 7

                repeat(weeks) { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        repeat(7) { dow ->
                            val dayNum = week * 7 + dow - firstOffset + 1
                            Box(modifier = Modifier.weight(1f)) {
                                if (dayNum in 1..daysCount) {
                                    val dayStr = "$displayYear-${displayMonth.toString().padStart(2, '0')}-${dayNum.toString().padStart(2, '0')}"
                                    val isFuture = dayStr > today
                                    val isSelected = dayStr == selected
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) MiOrange else Color.Transparent)
                                            .then(if (!isFuture) Modifier.clickable { selected = dayStr } else Modifier),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNum.toString(),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            ),
                                            color = when {
                                                isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                                isSelected -> Color.White
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp)) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(
                        onClick = { onDaySelected(selected) },
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
