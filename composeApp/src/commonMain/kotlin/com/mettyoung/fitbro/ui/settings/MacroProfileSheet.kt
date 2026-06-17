package com.mettyoung.fitbro.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.mettyoung.fitbro.data.model.MacroGoalProfile
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.util.MacroMath
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacroProfileSheet(
    profile: MacroGoalProfile?,
    stateHolder: MacroProfilesStateHolder,
    onDismiss: () -> Unit,
    onDeleteBlocked: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isEdit = profile != null

    var name by remember { mutableStateOf(profile?.name ?: "") }
    var proteinText by remember { mutableStateOf(profile?.proteinG?.roundToInt()?.toString() ?: "") }
    var carbsText by remember { mutableStateOf(profile?.carbsG?.roundToInt()?.toString() ?: "") }
    var fatText by remember { mutableStateOf(profile?.fatG?.roundToInt()?.toString() ?: "") }

    var nameError by remember { mutableStateOf(false) }
    var proteinError by remember { mutableStateOf(false) }
    var carbsError by remember { mutableStateOf(false) }
    var fatError by remember { mutableStateOf(false) }

    val computedCalories = MacroMath.caloriesFromMacros(
        proteinG = proteinText.toDoubleOrNull() ?: 0.0,
        carbG = carbsText.toDoubleOrNull() ?: 0.0,
        fatG = fatText.toDoubleOrNull() ?: 0.0
    )

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
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isEdit) "Edit Profile" else "Add Profile",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Profile Name") },
                modifier = Modifier.fillMaxWidth(),
                isError = nameError,
                supportingText = if (nameError) {
                    { Text("Name cannot be empty") }
                } else null,
                singleLine = true
            )

            OutlinedTextField(
                value = proteinText,
                onValueChange = { proteinText = it; proteinError = false },
                label = { Text("Protein (g)") },
                modifier = Modifier.fillMaxWidth(),
                isError = proteinError,
                supportingText = if (proteinError) {
                    { Text("Must be 0 or more") }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(
                value = carbsText,
                onValueChange = { carbsText = it; carbsError = false },
                label = { Text("Carbs (g)") },
                modifier = Modifier.fillMaxWidth(),
                isError = carbsError,
                supportingText = if (carbsError) {
                    { Text("Must be 0 or more") }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(
                value = fatText,
                onValueChange = { fatText = it; fatError = false },
                label = { Text("Fat (g)") },
                modifier = Modifier.fillMaxWidth(),
                isError = fatError,
                supportingText = if (fatError) {
                    { Text("Must be 0 or more") }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Text(
                text = "Calories: ${computedCalories.roundToInt()} kcal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val p = proteinText.toDoubleOrNull()
                    val c = carbsText.toDoubleOrNull()
                    val f = fatText.toDoubleOrNull()
                    nameError = name.isBlank()
                    proteinError = p == null || p < 0
                    carbsError = c == null || c < 0
                    fatError = f == null || f < 0
                    if (!nameError && !proteinError && !carbsError && !fatError) {
                        if (isEdit) {
                            stateHolder.updateProfile(profile!!.id, name.trim(), p!!, c!!, f!!, computedCalories)
                        } else {
                            stateHolder.addProfile(name.trim(), p!!, c!!, f!!, computedCalories)
                        }
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MiOrange)
            ) {
                Text(if (isEdit) "Save Changes" else "Add Profile")
            }

            if (isEdit) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(
                        onClick = {
                            if (stateHolder.isMappedToAnyWeekday(profile!!.id)) {
                                onDeleteBlocked()
                            } else {
                                stateHolder.deleteProfile(profile!!.id)
                                onDismiss()
                            }
                        }
                    ) {
                        Text(
                            text = "Delete",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
