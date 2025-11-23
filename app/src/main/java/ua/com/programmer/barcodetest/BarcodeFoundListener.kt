package ua.com.programmer.qrscanner

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.common.Barcode

interface BarcodeFoundListener {
    fun onBarcodeFound(barcode: Barcode, imageProxy: ImageProxy)
    fun onCodeNotFound(error: String?)
}
