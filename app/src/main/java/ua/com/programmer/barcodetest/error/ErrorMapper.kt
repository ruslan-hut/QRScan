package ua.com.programmer.barcodetest.error

import android.util.Log
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Maps exceptions to AppError types.
 * Provides consistent error handling across the application.
 */
object ErrorMapper {

    private const val TAG = "ErrorMapper"

    /**
     * Maps a Throwable to an AppError
     */
    fun map(throwable: Throwable): AppError {
        return when (throwable) {
            is AppError -> throwable
            is IOException -> mapIOException(throwable)
            is SecurityException -> AppError.CameraError.PermissionDenied
            is IllegalStateException -> AppError.CameraError.CameraInitializationFailed
            else -> {
                Log.e(TAG, "Unmapped error: ${throwable.message}", throwable)
                AppError.UnknownError(
                    message = throwable.message ?: "Unknown error",
                    userMessage = "An unexpected error occurred. Please try again."
                )
            }
        }
    }

    /**
     * Maps IOException to appropriate AppError
     */
    private fun mapIOException(exception: IOException): AppError {
        return when (exception) {
            is SocketTimeoutException -> AppError.NetworkError.Timeout
            is UnknownHostException -> AppError.NetworkError.NoConnection
            else -> AppError.NetworkError.Unknown(exception.message ?: "Network error")
        }
    }

    /**
     * Maps database exceptions to DatabaseError
     */
    fun mapDatabaseError(exception: Exception, operation: String): AppError.DatabaseError {
        Log.e(TAG, "Database error during $operation: ${exception.message}", exception)
        return when (operation.lowercase()) {
            "save" -> AppError.DatabaseError.SaveFailed
            "load", "getall" -> AppError.DatabaseError.LoadFailed
            "delete" -> AppError.DatabaseError.DeleteFailed
            "cleanup", "clean" -> AppError.DatabaseError.CleanupFailed
            else -> AppError.DatabaseError.Unknown(exception.message ?: "Database error")
        }
    }

    /**
     * Gets user-friendly error message from AppError
     */
    fun getUserMessage(error: AppError): String {
        return error.userMessage
    }

    /**
     * Gets debug/log message from AppError
     */
    fun getDebugMessage(error: AppError): String {
        return when (error) {
            is AppError.UnknownError -> error.message
            else -> error.javaClass.simpleName + ": " + error.userMessage
        }
    }
}

