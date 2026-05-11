package com.mettyoung.fitbro.data.food

import androidx.compose.runtime.Composable

sealed class BarcodeScanResult {
    data class Success(val barcode: String) : BarcodeScanResult()
    object NotAvailable : BarcodeScanResult()
    object Cancelled : BarcodeScanResult()
    data class Error(val message: String) : BarcodeScanResult()
}

@Composable
expect fun rememberBarcodeScanner(): (suspend () -> BarcodeScanResult)?
