package ua.com.programmer.qrscanner.error

import android.content.Context
import android.widget.Toast

/**
 * Utility functions for displaying errors to users
 */
object ErrorDisplay {

    /**
     * Shows error message to user via Toast
     */
    fun showError(context: Context, error: AppError) {
        Toast.makeText(context, error.userMessage, Toast.LENGTH_LONG).show()
    }

    /**
     * Shows error message from nullable AppError
     */
    fun showErrorIfPresent(context: Context, error: AppError?) {
        error?.let { showError(context, it) }
    }

    /**
     * Shows error message from Throwable
     */
    fun showError(context: Context, throwable: Throwable) {
        val appError = ErrorMapper.map(throwable)
        showError(context, appError)
    }
}

