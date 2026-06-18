package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.model.CustomFood
import com.mettyoung.fitbro.ui.MiOrange
import com.mettyoung.fitbro.ui.MiTextSecondary
import com.mettyoung.fitbro.util.MacroMath
import kotlin.math.roundToInt

@Composable
internal fun CustomFoodFormContent(
    title: String,
    actionLabel: String,
    onBack: () -> Unit,
    onSubmit: (CustomFood) -> Unit,
    initial: CustomFood? = null
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var brand by remember { mutableStateOf(initial?.brandName ?: "") }
    var servingG by remember { mutableStateOf(initial?.servingSizeG?.let { it.roundToInt().toString() } ?: "") }
    var calories by remember { mutableStateOf(initial?.calories?.let { it.roundToInt().toString() } ?: "") }
    var carbs by remember { mutableStateOf(initial?.carbG?.let { it.toString() } ?: "") }
    var protein by remember { mutableStateOf(initial?.proteinG?.let { it.toString() } ?: "") }
    var fat by remember { mutableStateOf(initial?.fatG?.let { it.toString() } ?: "") }

    fun recalcCalories(c: String, p: String, f: String) {
        val sum = MacroMath.caloriesFromMacros(
            carbG = c.toDoubleOrNull() ?: 0.0,
            proteinG = p.toDoubleOrNull() ?: 0.0,
            fatG = f.toDoubleOrNull() ?: 0.0
        )
        calories = if (sum > 0) sum.roundToInt().toString() else ""
    }

    val servingVal = servingG.toDoubleOrNull()
    val caloriesVal = calories.toDoubleOrNull()
    val carbsVal = carbs.toDoubleOrNull()
    val proteinVal = protein.toDoubleOrNull()
    val fatVal = fat.toDoubleOrNull()
    val valid = name.isNotBlank() &&
        servingVal != null && servingVal > 0 &&
        caloriesVal != null && caloriesVal >= 0 &&
        carbsVal != null && carbsVal >= 0 &&
        proteinVal != null && proteinVal >= 0 &&
        fatVal != null && fatVal >= 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.background(MaterialTheme.colorScheme.background, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Nutrition per serving",
                    style = MaterialTheme.typography.labelSmall,
                    color = MiOrange
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        CustomFoodField(label = "Food Name", value = name, onValueChange = { name = it }, isText = true)
        Spacer(Modifier.height(16.dp))
        CustomFoodField(label = "Brand (optional)", value = brand, onValueChange = { brand = it }, isText = true)
        Spacer(Modifier.height(16.dp))
        CustomFoodField(label = "Serving Size (g)", value = servingG, onValueChange = { servingG = it })
        
        Spacer(Modifier.height(32.dp))
        Text("Macronutrients", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        CustomFoodField(label = "Carbs (g)", value = carbs, onValueChange = { carbs = it; recalcCalories(it, protein, fat) })
        Spacer(Modifier.height(12.dp))
        CustomFoodField(label = "Protein (g)", value = protein, onValueChange = { protein = it; recalcCalories(carbs, it, fat) })
        Spacer(Modifier.height(12.dp))
        CustomFoodField(label = "Fat (g)", value = fat, onValueChange = { fat = it; recalcCalories(carbs, protein, it) })
        
        Spacer(Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp)).padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Estimated Calories", style = MaterialTheme.typography.bodyMedium, color = MiTextSecondary)
            Text("${caloriesVal?.roundToInt() ?: 0} kcal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MiOrange)
        }

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = {
                onSubmit(
                    CustomFood(
                        id = initial?.id ?: 0,
                        name = name.trim(),
                        brandName = brand.trim().takeIf { it.isNotBlank() },
                        calories = caloriesVal ?: 0.0,
                        proteinG = proteinVal ?: 0.0,
                        carbG = carbsVal ?: 0.0,
                        fatG = fatVal ?: 0.0,
                        servingSizeG = servingVal ?: 0.0,
                        createdAt = initial?.createdAt ?: ""
                    )
                )
            },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MiOrange),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(actionLabel, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun CustomFoodField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isText: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isText) KeyboardType.Text else KeyboardType.Decimal
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MiOrange,
            focusedLabelColor = MiOrange,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        )
    )
}
