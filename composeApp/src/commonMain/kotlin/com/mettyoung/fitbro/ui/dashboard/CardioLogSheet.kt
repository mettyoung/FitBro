package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.model.CardioSession
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.util.todayString

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

            OutlinedTextField(
                value = date,
                onValueChange = {},
                readOnly = true,
                label = { Text("Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                enabled = false,
                shape = RoundedCornerShape(12.dp)
            )

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
        DatePickerDialog(
            initialStartDate = date,
            onDismiss = { showDatePicker = false },
            onDateRangeSelected = { range ->
                date = range.startDate
                showDatePicker = false
            },
            allowFuture = false
        )
    }
}
