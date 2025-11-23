package ua.com.programmer.qrscanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.nio.ByteBuffer

/**
 * Utility class for processing barcode images.
 * Handles drawing bounding boxes on detected barcodes and converting ImageProxy to Bitmap.
 */
object BarcodeImageProcessor {

    private const val BOUNDING_BOX_COLOR = Color.GREEN
    private const val BOUNDING_BOX_STROKE_WIDTH = 8f

    /**
     * Converts ImageProxy to Bitmap
     * Uses YUV_420_888 format conversion to NV21
     */
    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val mediaImage = imageProxy.image
            if (mediaImage == null) return null

            val yPlane = mediaImage.planes[0]
            val uPlane = mediaImage.planes[1]
            val vPlane = mediaImage.planes[2]

            val yBuffer = yPlane.buffer
            val uBuffer = uPlane.buffer
            val vBuffer = vPlane.buffer

            val ySize = yBuffer.remaining()
            val width = imageProxy.width
            val height = imageProxy.height

            // U and V planes are subsampled (quarter resolution)
            val uvWidth = width / 2
            val uvHeight = height / 2
            val uvSize = uvWidth * uvHeight

            val nv21 = ByteArray(ySize + uvSize * 2)

            // Copy Y plane
            yBuffer.get(nv21, 0, ySize)

            // Get U and V data
            val uArray = ByteArray(uBuffer.remaining())
            uBuffer.get(uArray)
            val vArray = ByteArray(vBuffer.remaining())
            vBuffer.get(vArray)

            // Interleave V and U for NV21 format (VU order)
            // Since U and V are subsampled, we need to upscale them
            var uvIndex = 0
            for (i in 0 until uvSize) {
                val uIndex = minOf(i, uArray.size - 1)
                val vIndex = minOf(i, vArray.size - 1)
                nv21[ySize + uvIndex++] = vArray[vIndex]
                nv21[ySize + uvIndex++] = uArray[uIndex]
            }

            val yuvImage = android.graphics.YuvImage(
                nv21,
                android.graphics.ImageFormat.NV21,
                width,
                height,
                null
            )

            val out = java.io.ByteArrayOutputStream()
            // Use higher quality (100) to preserve brightness and avoid compression artifacts
            yuvImage.compressToJpeg(
                android.graphics.Rect(0, 0, width, height),
                100,
                out
            )
            val imageBytes = out.toByteArray()
            android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            Utils().debug("Error converting ImageProxy to Bitmap: ${e.message}")
            null
        }
    }

    /**
     * Draws bounding box around the detected barcode on the bitmap
     * @param bitmap The original bitmap
     * @param barcode The detected barcode with bounding box information
     * @param rotationDegrees The rotation degrees of the image
     * @return Bitmap with bounding box drawn, or original bitmap if barcode has no bounding box
     */
    fun drawBoundingBox(
        bitmap: Bitmap,
        barcode: Barcode,
        rotationDegrees: Int = 0
    ): Bitmap {
        val boundingBox = barcode.boundingBox ?: return bitmap

        // Create a mutable copy of the bitmap
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        // Create paint for bounding box
        val paint = Paint().apply {
            color = BOUNDING_BOX_COLOR
            style = Paint.Style.STROKE
            strokeWidth = BOUNDING_BOX_STROKE_WIDTH
            isAntiAlias = true
        }

        // Get bounding box coordinates
        val left = boundingBox.left.toFloat()
        val top = boundingBox.top.toFloat()
        val right = boundingBox.right.toFloat()
        val bottom = boundingBox.bottom.toFloat()

        // Draw rectangle around barcode
        canvas.drawRect(left, top, right, bottom, paint)

        return mutableBitmap
    }

    /**
     * Rotates bitmap to match the InputImage orientation
     * ML Kit bounding boxes are relative to the rotated InputImage, so we need to rotate the bitmap
     */
    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap
        
        val matrix = Matrix().apply {
            // Rotate around the center of the image
            postRotate(rotationDegrees.toFloat(), bitmap.width / 2f, bitmap.height / 2f)
        }
        
        // Calculate bounding box of rotated image
        val rect = android.graphics.RectF(
            0f, 0f, 
            bitmap.width.toFloat(), 
            bitmap.height.toFloat()
        )
        matrix.mapRect(rect)
        
        // Adjust matrix to translate to positive coordinates
        matrix.postTranslate(-rect.left, -rect.top)
        
        // Create new bitmap with correct dimensions
        val rotatedBitmap = Bitmap.createBitmap(
            rect.width().toInt(),
            rect.height().toInt(),
            Bitmap.Config.ARGB_8888
        )
        
        val canvas = Canvas(rotatedBitmap)
        canvas.drawBitmap(bitmap, matrix, null)
        
        return rotatedBitmap
    }

    /**
     * Enhances brightness and contrast of the bitmap
     * @param bitmap The bitmap to enhance
     * @param brightness Brightness value (0-150, default 80)
     * @param contrast Contrast value (50-200 representing 0.5-2.0, default 140 = 1.4)
     * @return Enhanced bitmap
     */
    fun enhanceBrightness(bitmap: Bitmap, brightness: Int = 80, contrast: Int = 140): Bitmap {
        val enhancedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(enhancedBitmap)
        
        // Convert contrast from 50-200 range to 0.5-2.0 scale
        val contrastScale = contrast / 100f
        
        // Create color matrix to adjust brightness and contrast
        val colorMatrix = ColorMatrix().apply {
            // Set contrast (scale RGB channels)
            setSaturation(1f) // Keep saturation
            setScale(contrastScale, contrastScale, contrastScale, 1f) // Adjust RGB contrast
            
            // Then, adjust brightness (add brightness value to each RGB channel)
            postConcat(ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, brightness.toFloat(),  // Red
                0f, 1f, 0f, 0f, brightness.toFloat(),  // Green
                0f, 0f, 1f, 0f, brightness.toFloat(),  // Blue
                0f, 0f, 0f, 1f, 0f    // Alpha
            )))
        }
        
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
            isAntiAlias = true
        }
        
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        // Recycle original if it was copied
        if (bitmap != enhancedBitmap) {
            bitmap.recycle()
        }
        
        return enhancedBitmap
    }

    /**
     * Processes ImageProxy and draws bounding box on it
     * @param imageProxy The ImageProxy from camera
     * @param barcode The detected barcode
     * @param brightness Brightness value (0-150, default 80)
     * @param contrast Contrast value (50-200 representing 0.5-2.0, default 140 = 1.4)
     * @return Bitmap with bounding box, or null if processing fails
     */
    fun processImageWithBarcode(
        imageProxy: ImageProxy,
        barcode: Barcode,
        brightness: Int = 80,
        contrast: Int = 140
    ): Bitmap? {
        val bitmap = imageProxyToBitmap(imageProxy) ?: return null
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        
        // Enhance brightness before rotation
        val enhancedBitmap = enhanceBrightness(bitmap, brightness, contrast)
        
        // Recycle original bitmap
        if (bitmap != enhancedBitmap) {
            bitmap.recycle()
        }
        
        // Rotate bitmap to match InputImage orientation (ML Kit coordinates are relative to rotated image)
        val rotatedBitmap = rotateBitmap(enhancedBitmap, rotationDegrees)
        
        // Recycle enhanced bitmap if it was rotated
        if (rotationDegrees != 0 && rotatedBitmap != enhancedBitmap) {
            enhancedBitmap.recycle()
        }
        
        return drawBoundingBox(rotatedBitmap, barcode, rotationDegrees)
    }
}

