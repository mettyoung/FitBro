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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mettyoung.fitbro.data.model.DailyBalance
import com.mettyoung.fitbro.ui.ColorCarbs
import com.mettyoung.fitbro.ui.ColorFat
import com.mettyoung.fitbro.ui.ColorProtein
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary
import com.mettyoung.fitbro.util.MONTH_ABBR
import com.mettyoung.fitbro.util.toYMD
import kotlin.math.roundToInt

@Composable
fun BreakdownDialog(
    balance: DailyBalance,
    onDismiss: () -> Unit
) {
    val (y, m, d) = balance.date.toYMD()
    val dateDisplay = "${MONTH_ABBR[m]} $d, $y"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = dateDisplay,
                    style = MaterialTheme.typography.labelSmall,
                    color = MiOrange
                )
                Text(
                    text = "Daily Breakdown",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(Modifier.height(24.dp))
                
                val balanceColor = if (balance.balance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background, RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "NET BALANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MiTextSecondary
                    )
                    Text(
                        text = "${if (balance.balance >= 0) "+" else ""}${balance.balance.roundToInt()} kcal",
                        style = MaterialTheme.typography.displayMedium.copy(fontSize = 32.sp),
                        color = balanceColor
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Intake Section
                SectionTitle("INTAKE")
                ComponentRow(
                    label = "Total Caloric Intake",
                    value = balance.intake,
                    total = balance.intake,
                    color = MiOrange
                )

                Spacer(Modifier.height(24.dp))

                // Burn Section
                SectionTitle("BURN BREAKDOWN")
                val components = listOf(
                    Triple("Basal Metabolic Rate", balance.bmr, ColorProtein),
                    Triple("Active Lifestyle", balance.neat, ColorFat),
                    Triple("Thermic Effect of Food", balance.tef, ColorCarbs)
                )
                
                components.forEachIndexed { index, (label, value, color) ->
                    ComponentRow(label = label, value = value, total = balance.burn, color = color)
                    if (index < components.size - 1) Spacer(Modifier.height(16.dp))
                }

                Spacer(Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp)).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Total Burned",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${balance.burn.roundToInt()} kcal",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MiOrange, 
                        contentColor = Color.White
                    )
                ) {
                    Text("Dismiss", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MiOrange,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun ComponentRow(label: String, value: Double, total: Double, color: Color) {
    val fraction = if (total > 0) (value / total).toFloat().coerceIn(0f, 1f) else 0f
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                Spacer(Modifier.width(10.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                "${value.roundToInt()} kcal",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .background(color = color, shape = CircleShape)
            )
        }
    }
}
