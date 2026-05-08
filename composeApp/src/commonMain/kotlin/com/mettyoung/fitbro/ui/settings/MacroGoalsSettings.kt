package com.mettyoung.fitbro.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mettyoung.fitbro.data.cache.UserSettingsDataSource

@Composable
fun MacroGoalsSettings(
    userSettingsDataSource: UserSettingsDataSource,
    modifier: Modifier = Modifier
) {
    var proteinGoal by remember { mutableStateOf("") }
    var carbsGoal by remember { mutableStateOf("") }
    var fatGoal by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        proteinGoal = userSettingsDataSource.getProteinGoalG().toInt().toString()
        carbsGoal = userSettingsDataSource.getCarbsGoalG().toInt().toString()
        fatGoal = userSettingsDataSource.getFatGoalG().toInt().toString()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Macro Goals",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Set your daily macronutrient targets",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TextField(
            value = proteinGoal,
            onValueChange = { proteinGoal = it },
            label = { Text("Protein (g)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        TextField(
            value = carbsGoal,
            onValueChange = { carbsGoal = it },
            label = { Text("Carbs (g)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        TextField(
            value = fatGoal,
            onValueChange = { fatGoal = it },
            label = { Text("Fat (g)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Button(
            onClick = {
                isSaving = true
                try {
                    val p = proteinGoal.toDoubleOrNull() ?: 0.0
                    val c = carbsGoal.toDoubleOrNull() ?: 0.0
                    val f = fatGoal.toDoubleOrNull() ?: 0.0

                    if (p > 0) userSettingsDataSource.setProteinGoalG(p)
                    if (c > 0) userSettingsDataSource.setCarbsGoalG(c)
                    if (f > 0) userSettingsDataSource.setFatGoalG(f)
                } finally {
                    isSaving = false
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            enabled = !isSaving && proteinGoal.isNotEmpty() && carbsGoal.isNotEmpty() && fatGoal.isNotEmpty()
        ) {
            Text(
                text = if (isSaving) "Saving..." else "Save Goals",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
