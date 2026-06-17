package com.mettyoung.fitbro.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.model.MacroGoalProfile
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MacroProfilesSettings(
    stateHolder: MacroProfilesStateHolder,
    onAddProfile: () -> Unit = {},
    onEditProfile: (MacroGoalProfile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by stateHolder.state.collectAsState()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showSheet by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf<MacroGoalProfile?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(top = statusBarPadding + 16.dp, bottom = 32.dp, start = 24.dp, end = 24.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Customize your metabolic targets",
                    style = MaterialTheme.typography.labelSmall,
                    color = MiOrange
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Macro Profiles",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.profiles.forEach { profile ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedProfile = profile
                                    showSheet = true
                                },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Text(
                                    text = profile.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "P: ${profile.proteinG.roundToInt()}g · " +
                                        "C: ${profile.carbsG.roundToInt()}g · " +
                                        "F: ${profile.fatG.roundToInt()}g · " +
                                        "${profile.caloriesKcal.roundToInt()} kcal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MiTextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    TextButton(onClick = {
                        selectedProfile = null
                        showSheet = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MiOrange
                        )
                        Text(
                            text = "Add Profile",
                            style = MaterialTheme.typography.labelLarge,
                            color = MiOrange
                        )
                    }
                }

                if (state.profiles.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))

                    Text(
                        text = "Weekly Schedule",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(12.dp))

                    val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    val defaultProfile = state.profiles.firstOrNull { it.name == "Default" }
                        ?: state.profiles.first()

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        dayLabels.forEachIndexed { weekday, label ->
                            val mappedProfileId = state.weekdayMappings[weekday]
                            val currentProfile = state.profiles.firstOrNull { it.id == mappedProfileId }
                                ?: defaultProfile
                            WeekdayRow(
                                dayLabel = label,
                                selectedProfile = currentProfile,
                                profiles = state.profiles,
                                onProfileSelected = { profile ->
                                    stateHolder.setMapping(weekday, profile.id)
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(48.dp))
            }
        }
    }

    if (showSheet) {
        MacroProfileSheet(
            profile = selectedProfile,
            stateHolder = stateHolder,
            onDismiss = { showSheet = false },
            onDeleteBlocked = {
                scope.launch {
                    snackbarHostState.showSnackbar("Unassign from all days first")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekdayRow(
    dayLabel: String,
    selectedProfile: MacroGoalProfile,
    profiles: List<MacroGoalProfile>,
    onProfileSelected: (MacroGoalProfile) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = dayLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(2f)
        ) {
            OutlinedTextField(
                value = selectedProfile.name,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth(),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                profiles.forEach { profile ->
                    DropdownMenuItem(
                        text = { Text(profile.name, style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            onProfileSelected(profile)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
