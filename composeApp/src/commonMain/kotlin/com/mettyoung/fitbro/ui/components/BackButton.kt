package com.mettyoung.fitbro.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable

/** ScreenHeader navigation slot for sub-pages; null when there's nowhere to go back to. */
fun backNavigation(onBack: (() -> Unit)?): (@Composable () -> Unit)? = onBack?.let { back ->
    { IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
}
