package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mettyoung.fitbro.data.model.DailyBalance
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun BreakdownDialog(
    balance: DailyBalance,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = balance.date,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                val balanceSign = if (balance.balance >= 0) "+" else ""
                val balanceColor = if (balance.balance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                Text(
                    text = "$balanceSign${balance.balance.roundToInt()} kcal",
                    style = MaterialTheme.typography.headlineSmall,
                    color = balanceColor
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                // Intake
                ComponentRow(
                    label = "Intake",
                    value = balance.intake,
                    total = balance.intake,
                    color = Color(0xFF2196F3)
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Burn breakdown",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))

                // Burn components
                val components = listOf(
                    Triple("BMR", balance.bmr, Color(0xFF9C27B0)),
                    Triple("TEF", balance.tef, Color(0xFFFF9800)),
                    Triple("NEAT", balance.neat, Color(0xFF4CAF50)),
                    Triple("EAT", balance.eat, Color(0xFF00BCD4))
                )
                components.forEach { (label, value, color) ->
                    ComponentRow(label = label, value = value, total = balance.burn, color = color)
                    Spacer(Modifier.height(6.dp))
                }

                Spacer(Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total burn", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${balance.burn.roundToInt()} kcal",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}

@Composable
private fun ComponentRow(label: String, value: Double, total: Double, color: Color) {
    val pct = if (total > 0) (value / total * 100).roundToInt() else 0
    val fraction = if (total > 0) (value / total).toFloat().coerceIn(0f, 1f) else 0f
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(
                "${value.roundToInt()} kcal  ($pct%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(3.dp)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .background(color = color, shape = RoundedCornerShape(3.dp))
            )
        }
    }
}
