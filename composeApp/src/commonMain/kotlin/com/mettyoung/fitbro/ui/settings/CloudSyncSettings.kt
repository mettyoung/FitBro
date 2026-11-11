package com.mettyoung.fitbro.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.sync.SyncRepository
import com.mettyoung.fitbro.data.sync.SyncResult
import com.mettyoung.fitbro.data.sync.SyncSettings
import com.mettyoung.fitbro.data.sync.SyncStatus
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary
import com.mettyoung.fitbro.util.formatTimeAgo

/** FR-17: Worker URL + token, manual sync, and last-sync status. */
@Composable
fun CloudSyncSettings(syncRepository: SyncRepository, settings: SyncSettings) {
    val status by syncRepository.status.collectAsState()
    var url by remember { mutableStateOf(settings.getSyncUrl()) }
    var token by remember { mutableStateOf(settings.getSyncToken()) }

    Spacer(Modifier.height(32.dp))
    Text(
        text = "Cloud Sync",
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold
    )
    Text(
        text = "Sync your food diary with your FitBro MCP server so Grok can read and log meals.",
        style = MaterialTheme.typography.bodySmall,
        color = MiTextSecondary
    )
    Spacer(Modifier.height(16.dp))

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Server URL") },
                placeholder = { Text("https://fitbro-mcp.you.workers.dev") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text("Sync token") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status.label(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status is SyncStatus.Failed) MaterialTheme.colorScheme.error else MiTextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val newUrl = url.trim().trimEnd('/')
                        if (newUrl != settings.getSyncUrl()) {
                            // A different server starts from scratch: full push, pull from the beginning.
                            settings.setSyncCursor(null)
                        }
                        settings.setSyncUrl(newUrl)
                        settings.setSyncToken(token)
                        syncRepository.requestSync()
                    },
                    enabled = url.isNotBlank() && token.isNotBlank() && status !is SyncStatus.Syncing,
                    colors = ButtonDefaults.buttonColors(containerColor = MiOrange)
                ) {
                    Text("Save & sync")
                }
            }
        }
    }
}

private fun SyncStatus.label(): String = when (this) {
    SyncStatus.Idle -> "Not synced yet"
    SyncStatus.Syncing -> "Syncing…"
    is SyncStatus.Synced -> "Last synced ${formatTimeAgo(atEpochMs)}"
    is SyncStatus.Failed -> when (val r = reason) {
        SyncResult.Failure.NotConfigured -> "Enter a server URL and token"
        SyncResult.Failure.Unauthorized -> "Token rejected by server"
        is SyncResult.Failure.Http -> "Server error (${r.status})"
        is SyncResult.Failure.Network -> "Couldn't reach server"
        is SyncResult.Failure.Unexpected -> "Sync failed: ${r.cause.message ?: "unknown error"}"
    }
}
