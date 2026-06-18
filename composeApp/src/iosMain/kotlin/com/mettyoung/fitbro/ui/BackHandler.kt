package com.mettyoung.fitbro.ui

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandlerEffect(enabled: Boolean, onBack: () -> Unit) {
    // iOS handles back gestures natively via UINavigationController
}
