package com.mettyoung.fitbro.ui.settings

import androidx.compose.material3.Card
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import com.mettyoung.fitbro.ui.components.ScreenGutter
import com.mettyoung.fitbro.ui.components.ScreenHeader
import com.mettyoung.fitbro.ui.components.SectionLabel
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.model.MacroGoalProfile
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MacroProfilesSettings(
    stateHolder: MacroProfilesStateHolder,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    footer: @Composable () -> Unit = {}
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
                Snackbar(snackbarData = data, containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
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
            ScreenHeader(title = "Settings", overline = "Targets & sync", navigation = com.mettyoung.fitbro.ui.components.backNavigation(onBack))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = ScreenGutter)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel("Macro profiles", modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        selectedProfile = null
                        showSheet = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New")
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.profiles.forEach { profile ->
                        Card(
                            onClick = {
                                selectedProfile = profile
                                showSheet = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = profile.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "P ${profile.proteinG.roundToInt()}g · " +
                                            "C ${profile.carbsG.roundToInt()}g · " +
                                            "F ${profile.fatG.roundToInt()}g",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MiTextSecondary
                                    )
                                }
                                Text(
                                    text = "${profile.caloriesKcal.roundToInt()}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = " kcal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MiTextSecondary
                                )
                            }
                        }
                    }
                }

                if (state.profiles.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    SectionLabel("Weekly schedule")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        val dayLabels = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                        val defaultProfile = state.profiles.firstOrNull { it.name == "Default" }
                            ?: state.profiles.first()

                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                }

                footer()

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

@Composable
private fun IconButton(onClick: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onClick, modifier = modifier, content = content)
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
            modifier = Modifier.weight(1.5f)
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
                textStyle = MaterialTheme.typography.bodySmall,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MiOrange
                )
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
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
