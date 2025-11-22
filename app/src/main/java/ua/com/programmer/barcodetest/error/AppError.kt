package ua.com.programmer.qrscanner.error

/**
 * Sealed class hierarchy for application errors.
 * Provides type-safe error handling across the app.
 */
sealed class AppError : Exception() {
    abstract val userMessage: String

    /**
     * Database-related errors
     */
    sealed class DatabaseError(override val userMessage: String) : AppError() {
        object SaveFailed : DatabaseError("Failed to save barcode. Please try again.")
        object LoadFailed : DatabaseError("Failed to load history. Please try again.")
        object DeleteFailed : DatabaseError("Failed to delete item. Please try again.")
        object CleanupFailed : DatabaseError("Failed to clean old history.")
        data class Unknown(override val message: String) : DatabaseError("Database error occurred.")
    }

    /**
     * Network-related errors (for future use)
     */
    sealed class NetworkError(override val userMessage: String) : AppError() {
        object NoConnection : NetworkError("No internet connection available.")
        object Timeout : NetworkError("Request timed out. Please try again.")
        object ServerError : NetworkError("Server error occurred. Please try again later.")
        data class Unknown(override val message: String) : NetworkError("Network error occurred.")
    }

    /**
     * Camera-related errors
     */
    sealed class CameraError(override val userMessage: String) : AppError() {
        object PermissionDenied : CameraError("Camera permission is required to scan barcodes.")
        object CameraUnavailable : CameraError("Camera is not available.")
        object CameraInitializationFailed : CameraError("Failed to initialize camera.")
        data class Unknown(override val message: String) : CameraError("Camera error occurred.")
    }

    /**
     * Barcode scanning errors
     */
    sealed class BarcodeError(override val userMessage: String) : AppError() {
        object InvalidBarcode : BarcodeError("Invalid barcode format.")
        object ScanFailed : BarcodeError("Failed to scan barcode. Please try again.")
        data class Unknown(override val message: String) : BarcodeError("Barcode scanning error occurred.")
    }

    /**
     * Generic/Unknown errors
     */
    data class UnknownError(
        override val message: String,
        override val userMessage: String = "An unexpected error occurred. Please try again."
    ) : AppError()
}

