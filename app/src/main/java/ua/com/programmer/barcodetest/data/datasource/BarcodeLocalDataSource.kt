package ua.com.programmer.qrscanner.data.datasource

import ua.com.programmer.qrscanner.Utils
import ua.com.programmer.qrscanner.data.BarcodeHistoryItem
import ua.com.programmer.qrscanner.data.database.BarcodeHistoryDao
import ua.com.programmer.qrscanner.data.database.BarcodeHistoryEntity
import java.util.Date
import java.util.Locale

/**
 * Local data source implementation using Room database.
 * This handles all direct database operations.
 */
class BarcodeLocalDataSource(
    private val dao: BarcodeHistoryDao
) : BarcodeDataSource {

    private val utils = Utils()

    override suspend fun saveBarcode(
        barcodeValue: String,
        barcodeFormat: String,
        codeType: Int
    ): Boolean {
        return try {
            if (barcodeValue.isNotEmpty() && barcodeFormat.isNotEmpty()) {
                val currentDate = Date()
                val eventTime = String.format("%ts", currentDate).toInt().toLong()
                val eventDate = String.format(
                    Locale.getDefault(),
                    "%td-%tm-%tY",
                    currentDate,
                    currentDate,
                    currentDate
                )

                val entity = BarcodeHistoryEntity(
                    id = 0, // Auto-generated
                    date = eventDate,
                    time = eventTime,
                    codeType = codeType,
                    codeValue = barcodeValue,
                    note = null
                )
                dao.insertHistoryItem(entity)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getAllHistoryItems(): List<BarcodeHistoryItem> {
        return try {
            val entities = dao.getAllHistoryItemsList()
            entities.map { entity ->
                BarcodeHistoryItem(
                    id = entity.id,
                    date = entity.date,
                    time = entity.time,
                    codeType = entity.codeType,
                    codeValue = entity.codeValue,
                    note = entity.note
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun deleteHistoryItem(itemId: Long): Boolean {
        return try {
            val deletedRows = dao.deleteHistoryItemById(itemId)
            deletedRows > 0
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun cleanOldHistory(retentionDays: Int): Int {
        return try {
            val cutoffTime = utils.dateBeginShiftDate(retentionDays)
            dao.deleteOldHistory(cutoffTime)
        } catch (e: Exception) {
            0
        }
    }
}

