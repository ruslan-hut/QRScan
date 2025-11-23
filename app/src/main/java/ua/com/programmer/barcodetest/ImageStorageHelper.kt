package ua.com.programmer.qrscanner

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for saving images to internal storage.
 * Images are saved in the app's internal files directory.
 */
object ImageStorageHelper {

    private const val IMAGE_DIRECTORY = "barcode_images"
    private const val IMAGE_FORMAT = "jpg"
    private const val IMAGE_QUALITY = 95 // Increased quality to preserve brightness

    /**
     * Gets the directory for storing barcode images
     */
    private fun getImageDirectory(context: Context): File {
        val directory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use external files directory for Android 10+
            File(context.getExternalFilesDir(null), IMAGE_DIRECTORY)
        } else {
            // Use internal files directory
            File(context.filesDir, IMAGE_DIRECTORY)
        }
        
        if (!directory.exists()) {
            directory.mkdirs()
        }
        
        return directory
    }

    /**
     * Generates a unique filename based on timestamp
     */
    private fun generateFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
        return "barcode_$timestamp.$IMAGE_FORMAT"
    }

    /**
     * Saves a bitmap to internal storage
     * @param bitmap The bitmap to save
     * @param context The application context
     * @return The file path if successful, null otherwise
     */
    fun saveImage(bitmap: Bitmap, context: Context): String? {
        return try {
            val directory = getImageDirectory(context)
            val fileName = generateFileName()
            val file = File(directory, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, out)
                out.flush()
            }

            file.absolutePath
        } catch (e: IOException) {
            Utils().debug("Error saving image: ${e.message}")
            null
        }
    }

    /**
     * Deletes an image file
     * @param imagePath The path to the image file
     * @return true if deleted successfully, false otherwise
     */
    fun deleteImage(imagePath: String?): Boolean {
        if (imagePath.isNullOrEmpty()) return false
        
        return try {
            val file = File(imagePath)
            file.exists() && file.delete()
        } catch (e: Exception) {
            Utils().debug("Error deleting image: ${e.message}")
            false
        }
    }

    /**
     * Checks if an image file exists
     */
    fun imageExists(imagePath: String?): Boolean {
        if (imagePath.isNullOrEmpty()) return false
        return File(imagePath).exists()
    }
}

