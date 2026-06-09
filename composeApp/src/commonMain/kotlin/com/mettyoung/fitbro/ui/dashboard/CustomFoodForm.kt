package com.mettyoung.fitbro.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mettyoung.fitbro.data.model.CustomFood
import com.mettyoung.fitbro.ui.MiOrange
import kotlin.math.roundToInt

/**
 * Shared create/edit form for a [CustomFood]. [initial] non-null = edit mode (id preserved).
 * Used by the Log Food search sheet (create-and-add) and the custom-food manager (create/edit).
 */
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
            .padding(horizontal = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Back", tint = MiOrange)
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(16.dp))

        CustomFoodField(label = "Name", value = name, onValueChange = { name = it }, isText = true)
        Spacer(Modifier.height(12.dp))
        CustomFoodField(label = "Brand (optional)", value = brand, onValueChange = { brand = it }, isText = true)
        Spacer(Modifier.height(12.dp))
        CustomFoodField(label = "Serving size (g)", value = servingG, onValueChange = { servingG = it })
        Spacer(Modifier.height(12.dp))
        CustomFoodField(label = "Calories (kcal)", value = calories, onValueChange = { calories = it })
        Spacer(Modifier.height(12.dp))
        CustomFoodField(label = "Carbs (g)", value = carbs, onValueChange = { carbs = it })
        Spacer(Modifier.height(12.dp))
        CustomFoodField(label = "Protein (g)", value = protein, onValueChange = { protein = it })
        Spacer(Modifier.height(12.dp))
        CustomFoodField(label = "Fat (g)", value = fat, onValueChange = { fat = it })

        Spacer(Modifier.height(24.dp))

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
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MiOrange),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(actionLabel, color = Color.White)
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CustomFoodField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isText: Boolean = false
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isText) KeyboardType.Text else KeyboardType.Decimal
        ),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.background,
            unfocusedContainerColor = MaterialTheme.colorScheme.background
        )
    )
}
