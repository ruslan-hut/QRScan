package ua.com.programmer.barcodetest.data.repository

import ua.com.programmer.barcodetest.data.BarcodeHistoryItem

/**
 * Repository interface that defines the contract for barcode data operations.
 * The repository acts as a single source of truth and abstracts data sources.
 */
interface BarcodeRepository {
    /**
     * Saves a scanned barcode
     * @param barcodeValue The barcode value
     * @param barcodeFormat The barcode format name
     * @param codeType The barcode format type code
     * @return Result indicating success or failure
     */
    suspend fun saveBarcode(
        barcodeValue: String,
        barcodeFormat: String,
        codeType: Int
    ): Result<Unit>

    /**
     * Retrieves all barcode history items
     * @return Result containing list of history items or error
     */
    suspend fun getAllHistoryItems(): Result<List<BarcodeHistoryItem>>

    /**
     * Deletes a history item by ID
     * @param itemId The ID of the item to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteHistoryItem(itemId: Long): Result<Unit>

    /**
     * Cleans old history items based on retention policy
     * @param retentionDays Number of days to retain history (default: 30)
     * @return Result containing number of deleted items or error
     */
    suspend fun cleanOldHistory(retentionDays: Int = 30): Result<Int>
}

