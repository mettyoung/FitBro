package com.mettyoung.fitbro.data.food

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
actual fun rememberBarcodeScanner(): (suspend () -> BarcodeScanResult)? {
    val context = LocalContext.current
    return remember(context) {
        {
            suspendCancellableCoroutine { cont ->
                val options = GmsBarcodeScannerOptions.Builder()
                    .setBarcodeFormats(
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E,
                        Barcode.FORMAT_QR_CODE
                    )
                    .build()
                GmsBarcodeScanning.getClient(context, options)
                    .startScan()
                    .addOnSuccessListener { barcode ->
                        if (cont.isActive) cont.resume(BarcodeScanResult.Success(barcode.rawValue ?: ""))
                    }
                    .addOnCanceledListener {
                        if (cont.isActive) cont.resume(BarcodeScanResult.Cancelled)
                    }
                    .addOnFailureListener { e ->
                        if (cont.isActive) cont.resume(BarcodeScanResult.Error(e.message ?: "Scan failed"))
                    }
            }
        }
    }
}
