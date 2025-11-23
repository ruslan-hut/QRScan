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
            var barcodeFound = false
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes: List<Barcode> ->
                    if (barcodes.isNotEmpty()) {
                        // Process only the first barcode to avoid multiple image processing
                        val barcode = barcodes[0]
                        barcodeFound = true
                        listener.onBarcodeFound(barcode, imageProxy)
                    } else {
                        // No barcode found, close the ImageProxy
                        imageProxy.close()
                        mediaImage.close()
                    }
                }
                .addOnFailureListener { e: Exception -> 
                    listener.onCodeNotFound(e.message)
                    imageProxy.close()
                    mediaImage.close()
                }
                .addOnCompleteListener {
                    // ImageProxy is closed in onSuccess or onFailure
                }
        } else {
            imageProxy.close()
        }
    }
}
