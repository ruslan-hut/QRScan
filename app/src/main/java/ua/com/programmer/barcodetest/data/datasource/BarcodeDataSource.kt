package ua.com.programmer.barcodetest.data.datasource

import ua.com.programmer.barcodetest.data.BarcodeHistoryItem

/**
 * Interface for barcode data source operations.
 * This abstraction allows for different implementations (local database, remote API, etc.)
 */
interface BarcodeDataSource {
    /**
     * Saves a barcode to the data source
     * @param barcodeValue The barcode value
     * @param barcodeFormat The barcode format name
     * @param codeType The barcode format type code
     * @return true if saved successfully, false otherwise
     */
    suspend fun saveBarcode(
        barcodeValue: String,
        barcodeFormat: String,
        codeType: Int
    ): Boolean

    /**
     * Retrieves all history items from the data source
     * @return List of barcode history items, ordered by time descending
     */
    suspend fun getAllHistoryItems(): List<BarcodeHistoryItem>

    /**
     * Deletes a history item by ID
     * @param itemId The ID of the item to delete
     * @return true if deleted successfully, false otherwise
     */
    suspend fun deleteHistoryItem(itemId: Long): Boolean

    /**
     * Cleans old history items based on retention policy
     * @return Number of items deleted
     */
    suspend fun cleanOldHistory(): Int
}

