package ua.com.programmer.qrscanner

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class BarcodeImageAnalyzer(private val listener: BarcodeFoundListener) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()

    override fun analyze(imageProxy: ImageProxy) {
        @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError") val mediaImage =
            imageProxy.image
        if (mediaImage != null) {
            val inputImage =
                InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes: List<Barcode> ->
                    for (barcode in barcodes) {
                        listener.onBarcodeFound(barcode.rawValue, barcode.format)
                    }
                }
                .addOnFailureListener { e: Exception -> listener.onCodeNotFound(e.message) }
                .addOnCompleteListener {
                    imageProxy.close()
                    mediaImage.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
