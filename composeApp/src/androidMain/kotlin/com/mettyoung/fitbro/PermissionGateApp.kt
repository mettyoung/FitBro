package com.mettyoung.fitbro

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.NutritionRecord

private val REQUIRED_PERMISSIONS = setOf(
    HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
    HealthPermission.getReadPermission(NutritionRecord::class),
    HealthPermission.getReadPermission(BasalMetabolicRateRecord::class)
)

@Composable
fun PermissionGateApp() {
    val context = AndroidAppContext.context
    val sdkAvailable = remember {
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    MaterialTheme {
        if (sdkAvailable) {
            PermissionGateContent()
        } else {
            // Health Connect unavailable — show app; data source returns NotAvailable error state
            App()
        }
    }
}

@Composable
private fun PermissionGateContent() {
    val context = AndroidAppContext.context
    val client = remember { HealthConnectClient.getOrCreate(context) }
    val scope = rememberCoroutineScope()

    var allGranted by remember { mutableStateOf(false) }
    var checked by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { _ ->
        scope.launch {
            val currentlyGranted = try {
                client.permissionController.getGrantedPermissions()
            } catch (e: Exception) {
                emptySet()
            }
            allGranted = currentlyGranted.containsAll(REQUIRED_PERMISSIONS)
        }
    }

    LaunchedEffect(Unit) {
        val granted = try {
            client.permissionController.getGrantedPermissions()
        } catch (e: Exception) {
            emptySet()
        }
        allGranted = granted.containsAll(REQUIRED_PERMISSIONS)
        checked = true
    }

    when {
        !checked -> Box(
            modifier = Modifier.fillMaxSize().safeContentPadding(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        allGranted -> App()
        else -> PermissionScreen(onGrant = { launcher.launch(REQUIRED_PERMISSIONS) })
    }
}

@Composable
private fun PermissionScreen(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Health Connect Access Required",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "FitBro needs Health Connect permissions to read your calorie data (intake, BMR, and activity).",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onGrant) {
            Text("Grant Permissions")
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Permissions required to show data",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
