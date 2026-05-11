package com.mettyoung.fitbro.data.food

import androidx.compose.runtime.Composable

@Composable
actual fun rememberBarcodeScanner(): (suspend () -> BarcodeScanResult)? = null
