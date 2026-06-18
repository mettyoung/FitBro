package com.mettyoung.fitbro.ui

import androidx.compose.runtime.Composable

@Composable
expect fun BackHandlerEffect(enabled: Boolean = true, onBack: () -> Unit)
