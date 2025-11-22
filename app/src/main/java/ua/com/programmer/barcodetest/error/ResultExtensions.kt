package ua.com.programmer.barcodetest.error

/**
 * Extension functions for Result type to work with AppError
 */

/**
 * Maps a Result's exception to AppError
 */
fun <T> Result<T>.mapError(): Result<T> {
    return onFailure { exception ->
        val appError = ErrorMapper.map(exception)
        return@onFailure Result.failure<T>(appError)
    }
}

/**
 * Gets the AppError from a failed Result, or null if successful
 */
fun <T> Result<T>.getAppError(): AppError? {
    return exceptionOrNull()?.let { ErrorMapper.map(it) }
}

/**
 * Gets user-friendly error message from Result
 */
fun <T> Result<T>.getErrorMessage(): String? {
    return getAppError()?.userMessage
}

